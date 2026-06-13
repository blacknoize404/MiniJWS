package io.github.blacknoize404.miniJWS.demo;

import io.github.blacknoize404.miniJWS.HttpServer;
import io.github.blacknoize404.miniJWS.handlers.StaticFileHandler;
import io.github.blacknoize404.miniJWS.middleware.*;
import io.github.blacknoize404.miniJWS.primitives.ContentType;
import io.github.blacknoize404.miniJWS.primitives.HttpMethod;
import io.github.blacknoize404.miniJWS.responses.HttpResponse;

import java.io.IOException;
import java.util.Map;

public final class DemoServer {

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        HttpServer server = new HttpServer(port);

        // ── Middleware pipeline ──────────────────────────────────
        server.use(new AccessLogMiddleware());
        server.use(new CorsMiddleware().allowOrigin("*"));
        server.use(new RateLimitMiddleware(200, 60));

        // ── Routes ───────────────────────────────────────────────

        // Static files from ./public (HTML, CSS, JS, images)
        server.addStaticRoute("/*", "./public");

        // Home — demonstrates method chaining
        server.addRoute(HttpMethod.GET, "/", req ->
                HttpResponse.redirect("/index.html")
        );

        // Hello — path parameter
        server.addRoute(HttpMethod.GET, "/hello", req -> {
            String name = req.getParameters().getOrDefault("name", "World");
            return new HttpResponse.Builder()
                    .setContentType(ContentType.TEXT)
                    .setBody("Hello, " + name + "!")
                    .build();
        });

        // Hello — path parameter with :name style
        server.addRoute(HttpMethod.GET, "/hello/:name", req -> {
            String name = req.getParameters().get("name");
            return new HttpResponse.Builder()
                    .setContentType(ContentType.TEXT)
                    .setBody("Hello, " + name + "!")
                    .build();
        });

        // API info
        server.addRoute(HttpMethod.GET, "/api/info", req -> {
            String json = """
                    {
                        "server": "MiniJWS",
                        "version": "1.0-SNAPSHOT",
                        "status": "running",
                        "features": [
                            "middleware", "CORS", "path-params",
                            "static-files", "keep-alive", "gzip",
                            "rate-limit", "cookies", "redirect"
                        ]
                    }
                    """;
            return new HttpResponse.Builder()
                    .setContentType(ContentType.JSON)
                    .setBody(json)
                    .build();
        });

        // POST with JSON body parsing
        server.addRoute(HttpMethod.POST, "/api/data", req -> {
            var json = req.bodyAsJson().orElse(Map.of());
            var form = req.bodyAsForm().orElse(Map.of());

            String response = "{"
                    + "\"parsed_json\": " + mapToJson(json) + ", "
                    + "\"parsed_form\": " + mapToJson(form)
                    + "}";

            return new HttpResponse.Builder()
                    .setContentType(ContentType.JSON)
                    .setBody(response)
                    .build();
        });

        // Set a cookie
        server.addRoute(HttpMethod.GET, "/set-cookie", req -> {
            String name = req.getParameters().getOrDefault("name", "user");
            String value = req.getParameters().getOrDefault("value", "miniJWS");
            return new HttpResponse.Builder()
                    .setContentType(ContentType.TEXT)
                    .setCookie(name, value, 3600, "/", true)
                    .setBody("Cookie set: " + name + "=" + value)
                    .build();
        });

        // Read cookies
        server.addRoute(HttpMethod.GET, "/get-cookies", req -> {
            var cookies = req.getCookies();
            return new HttpResponse.Builder()
                    .setContentType(ContentType.JSON)
                    .setBody(mapToJson(cookies))
                    .build();
        });

        // Redirect demo
        server.addRoute(HttpMethod.GET, "/old-path", req ->
                HttpResponse.redirect("/new-path", 301)
        );

        server.addRoute(HttpMethod.GET, "/new-path", req ->
                new HttpResponse.Builder()
                        .setContentType(ContentType.TEXT)
                        .setBody("You were redirected here!")
                        .build()
        );

        // Echo — shows request details
        server.addRoute(HttpMethod.GET, "/echo", req -> {
            String info = """
                    Method: %s
                    Path: %s
                    Protocol: %s
                    Headers: %s
                    Params: %s
                    Cookies: %s
                    """.formatted(
                    req.getHttpMethod(),
                    req.getUri(),
                    req.getProtocolVersion(),
                    req.getHeaders(),
                    req.getParameters(),
                    req.getCookies()
            );
            return new HttpResponse.Builder()
                    .setContentType(ContentType.TEXT)
                    .setBody(info)
                    .build();
        });

        System.out.println("=".repeat(50));
        System.out.println("  MiniJWS Demo Server");
        System.out.println("  http://localhost:" + port);
        System.out.println("=".repeat(50));
        System.out.println("  Routes:");
        System.out.println("    GET  /                 → redirect to /index.html");
        System.out.println("    GET  /hello            → Hello, World!");
        System.out.println("    GET  /hello?name=X     → Hello, X!");
        System.out.println("    GET  /hello/:name      → Hello, {name}!");
        System.out.println("    GET  /api/info         → Server info (JSON)");
        System.out.println("    POST /api/data         → Parse JSON/form body");
        System.out.println("    GET  /set-cookie       → Set a cookie");
        System.out.println("    GET  /get-cookies      → Read cookies");
        System.out.println("    GET  /old-path         → 301 redirect to /new-path");
        System.out.println("    GET  /echo             → Echo request details");
        System.out.println("    GET  /*                → Static files from ./public");
        System.out.println("=".repeat(50));

        server.run();
    }

    private static String mapToJson(Map<String, String> map) {
        var sb = new StringBuilder("{");
        var it = map.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            sb.append('"').append(e.getKey()).append('"').append(':');
            sb.append('"').append(e.getValue().replace("\"", "\\\"")).append('"');
            if (it.hasNext()) sb.append(", ");
        }
        return sb.append('}').toString();
    }
}
