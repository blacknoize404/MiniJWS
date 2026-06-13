package io.github.blacknoize404.miniJWS;

import io.github.blacknoize404.miniJWS.primitives.*;

import io.github.blacknoize404.miniJWS.requests.HttpDecoder;
import io.github.blacknoize404.miniJWS.requests.HttpRequest;
import io.github.blacknoize404.miniJWS.responses.HttpEncoder;
import io.github.blacknoize404.miniJWS.handlers.StaticFileHandler;
import io.github.blacknoize404.miniJWS.responses.HttpResponse;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import java.util.concurrent.CopyOnWriteArrayList;

public final class HttpServer {

    public static final String SERVER_NAME = "MiniJWS";

    private final Map<String, RequestRunner> routes = new ConcurrentHashMap<>();
    private final List<Middleware> middlewares = new CopyOnWriteArrayList<>();
    private final ServerSocket socket;
    private final ExecutorService threadPool;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);
    private final int port;

    public HttpServer(int port, int parallelism) throws IOException {
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(parallelism);
        this.socket = new ServerSocket(port);
    }

    public HttpServer(int port) throws IOException {
        this(port, Runtime.getRuntime().availableProcessors() * 2);
    }

    public HttpServer addRoute(HttpMethod method, String path, RequestRunner runner) {
        String key = routeKey(method, path);
        routes.put(key, runner);
        return this;
    }

    public HttpServer addStaticRoute(String mountPath, String directory) {
        String normalized = normalizePath(mountPath);
        if (!normalized.endsWith("/**")) {
            normalized = normalized.endsWith("/*") ? normalized + "*" : normalized + "/**";
        }
        String key = routeKey(HttpMethod.GET, normalized);
        routes.put(key, new StaticFileHandler(directory));
        return this;
    }

    public void removeRoute(HttpMethod method, String path) {
        routes.remove(routeKey(method, path));
    }

    public HttpServer use(Middleware middleware) {
        middlewares.add(middleware);
        return this;
    }

    public void run() {
        run(true);
    }

    public void run(boolean addShutdownHook) {
        running.set(true);

        if (addShutdownHook) {
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        }

        System.out.println("[MiniJWS] Server started on port " + port);

        while (running.get()) {
            try {
                Socket client = socket.accept();
                threadPool.execute(() -> handleConnection(client));
            } catch (IOException e) {
                if (running.get()) {
                    System.err.println("[MiniJWS] Accept error: " + e.getMessage());
                }
            }
        }

        shutdown();
    }

    public void stop() {
        running.set(false);
        shutdownLatch.countDown();
        try {
            socket.close();
        } catch (IOException ignored) {}
    }

    public void idle() {
        try {
            shutdownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static final int MAX_KEEPALIVE_REQUESTS = 100;
    private static final int KEEPALIVE_TIMEOUT_MS = 10_000;

    private void handleConnection(Socket client) {
        try (Socket s = client;
             InputStream in = s.getInputStream();
             OutputStream out = s.getOutputStream()) {

            s.setSoTimeout(KEEPALIVE_TIMEOUT_MS);
            var reader = new BufferedInputStream(in);
            int requests = 0;

            while (requests < MAX_KEEPALIVE_REQUESTS) {
                Optional<HttpRequest> decoded = HttpDecoder.decode(reader);

                if (decoded.isEmpty()) break;

                HttpRequest request = decoded.get();
                requests++;

                MiddlewareChain chain = buildChain(request, out);
                HttpResponse response = chain.next(request);

                boolean keepAlive = shouldKeepAlive(request, response);
                if (!keepAlive) {
                    response.getHeaders().put("Connection", List.of("close"));
                    HttpEncoder.sendResponse(response, out);
                    break;
                }

                response.getHeaders().put("Connection", List.of("keep-alive"));
                HttpEncoder.sendResponse(response, out);
            }

        } catch (java.net.SocketTimeoutException e) {
            // idle timeout — close connection gracefully
        } catch (IOException e) {
            System.err.println("[MiniJWS] Connection error: " + e.getMessage());
        }
    }

    private static boolean shouldKeepAlive(HttpRequest request, HttpResponse response) {
        if (!"HTTP/1.1".equals(request.getProtocolVersion()) &&
            !"HTTP/1.0".equals(request.getProtocolVersion())) {
            return false;
        }

        String conn = request.getHeader("Connection").orElse("");
        boolean requestClose = conn.equalsIgnoreCase("close");
        boolean requestKeepAlive = conn.equalsIgnoreCase("keep-alive");

        if ("HTTP/1.1".equals(request.getProtocolVersion())) {
            return !requestClose;
        }

        return requestKeepAlive;
    }

    private MiddlewareChain buildChain(HttpRequest request, OutputStream out) {
        MiddlewareChain terminal = req -> {
            String path = req.getUri().getRawPath();
            String key = routeKey(req.getHttpMethod(), path);
            RequestRunner runner = routes.get(key);

            if (runner == null) {
                runner = findRouteWithParams(req.getHttpMethod(), path, req);
            }

            if (runner != null) {
                return runner.run(req);
            }
            return new HttpResponse.Builder()
                    .setStatusCode(404)
                    .setContentType(ContentType.TEXT)
                    .setBody("404 - Not Found")
                    .build();
        };

        MiddlewareChain chain = terminal;
        for (int i = middlewares.size() - 1; i >= 0; i--) {
            Middleware mw = middlewares.get(i);
            MiddlewareChain next = chain;
            chain = req -> mw.run(req, next);
        }

        return chain;
    }

    private void shutdown() {
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("[MiniJWS] Server stopped");
    }

    private RequestRunner findRouteWithParams(HttpMethod method, String path, HttpRequest original) {
        String normalized = normalizePath(path);

        for (var entry : routes.entrySet()) {
            String[] entryParts = entry.getKey().split(":", 2);
            if (entryParts.length != 2) continue;
            if (!entryParts[0].equals(method.name())) continue;

            String pattern = entryParts[1];
            Map<String, String> params = matchPath(pattern, normalized);
            if (params != null) {
                Map<String, String> merged = new HashMap<>(original.getParameters());
                merged.putAll(params);

                var headersCopy = new LinkedHashMap<String, List<String>>();
                for (var h : original.getHeaders().entrySet()) {
                    headersCopy.put(h.getKey(), List.copyOf(h.getValue()));
                }

                HttpRequest modified = new HttpRequest.Builder()
                        .setHttpMethod(original.getHttpMethod())
                        .setUri(original.getUri())
                        .setProtocolVersion(original.getProtocolVersion())
                        .setHeaders(headersCopy)
                        .setParameters(merged)
                        .setBody(original.getBody().orElse(null))
                        .build();

                RequestRunner runner = entry.getValue();
                return req -> runner.run(modified);
            }
        }
        return null;
    }

    static Map<String, String> matchPath(String pattern, String path) {
        String[] patSegments = pattern.split("/");
        String[] pathSegments = path.split("/");

        Map<String, String> params = new LinkedHashMap<>();
        int pi = 0;

        for (int si = 0; si < patSegments.length; si++) {
            if (patSegments[si].equals("**")) {
                return params;
            }
            if (pi >= pathSegments.length) return null;

            if (patSegments[si].equals("*")) {
                pi++;
            } else if (patSegments[si].startsWith(":")) {
                params.put(patSegments[si].substring(1), pathSegments[pi]);
                pi++;
            } else {
                if (!patSegments[si].equals(pathSegments[pi])) return null;
                pi++;
            }
        }

        return pi == pathSegments.length ? params : null;
    }

    private static String routeKey(HttpMethod method, String path) {
        return method.name() + ":" + normalizePath(path);
    }

    private static String normalizePath(String path) {
        String p = path;
        if (!p.startsWith("/")) p = "/" + p;
        if (!p.equals("/") && p.endsWith("/")) p = p.substring(0, p.length() - 1);
        return p;
    }
}
