package io.github.blacknoize404.miniJWS.middleware;

import io.github.blacknoize404.miniJWS.primitives.Middleware;
import io.github.blacknoize404.miniJWS.primitives.MiddlewareChain;
import io.github.blacknoize404.miniJWS.requests.HttpRequest;
import io.github.blacknoize404.miniJWS.responses.HttpResponse;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public final class AccessLogMiddleware implements Middleware {

    private static final int QUEUE_CAPACITY = 16_384;
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.US);

    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private final Thread worker;

    public AccessLogMiddleware() {
        this(new PrintWriter(System.out, true));
    }

    public AccessLogMiddleware(String filePath) throws IOException {
        var pw = new PrintWriter(new FileWriter(filePath, StandardCharsets.UTF_8, true), true);
        this.worker = new Thread(() -> drainTo(pw), "access-log");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    public AccessLogMiddleware(PrintWriter writer) {
        this.worker = new Thread(() -> drainTo(writer), "access-log");
        this.worker.setDaemon(true);
        this.worker.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            this.worker.interrupt();
            try { this.worker.join(2_000); } catch (InterruptedException ignored) {}
            flushRemaining(writer);
        }));
    }

    @Override
    public HttpResponse run(HttpRequest request, MiddlewareChain chain) {
        long start = System.nanoTime();
        HttpResponse response = chain.next(request);
        long elapsed = System.nanoTime() - start;

        String logLine = formatLogLine(request, response, elapsed);
        queue.offer(logLine);

        return response;
    }

    private String formatLogLine(HttpRequest request, HttpResponse response, long elapsedNanos) {
        String remoteAddr = extractRemoteAddr(request);
        String method = request.getHttpMethod().name();
        String path = request.getUri().getRawPath();
        int status = response.getStatusCode();
        long bodySize = response.getBody().map(b -> (long) b.length).orElse(0L);
        long ms = elapsedNanos / 1_000_000;

        return String.format(Locale.US, "%s - - [%s] \"%s %s %s\" %d %d (%dms)",
                remoteAddr,
                ZonedDateTime.now(ZoneOffset.UTC).format(DATE_FMT),
                method,
                path,
                request.getProtocolVersion(),
                status,
                bodySize,
                ms);
    }

    private void drainTo(PrintWriter writer) {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                String line = queue.take();
                writer.println(line);
                writer.flush();
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } finally {
            flushRemaining(writer);
        }
    }

    private void flushRemaining(PrintWriter writer) {
        var lines = new java.util.ArrayList<String>();
        queue.drainTo(lines);
        for (String line : lines) {
            writer.println(line);
        }
        writer.flush();
    }

    private static String extractRemoteAddr(HttpRequest request) {
        return request.getHeader("X-Forwarded-For")
                .orElse(request.getHeader("X-Real-IP")
                .orElse("127.0.0.1"));
    }
}
