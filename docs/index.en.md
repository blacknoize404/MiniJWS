# MiniJWS Documentation

MiniJWS is a lightweight, modular Java HTTP server framework built with Java 25.
It provides a flexible foundation for building web servers and services with a clean,
modular architecture.

## Project Structure

```
MiniJWS/
├── miniJWS-core/              # Core HTTP server library
├── miniJWS-demo/              # Full demo server
├── miniQR/                    # QR code generation module
├── miniStaticServer/          # Static file server module
├── miniApkReader/             # Android APK reader module
├── public/                    # Demo static assets
├── docs/                      # Documentation
└── README.md
```

## Modules Overview

| Module | Description |
|--------|-------------|
| **miniJWS-core** | Core HTTP/1.1 server with routing, middleware, path params, static files, cookies, CORS, gzip, rate limiting, keep-alive |
| **miniJWS-demo** | Complete demo server showcasing all features |
| **miniQR** | QR code generation using ZXing with SVG output support |
| **miniStaticServer** | Static file server with template support and QR code injection |
| **miniApkReader** | Android APK metadata extraction and parsing |

## Feature Highlights

- **HTTP/1.1** with keep-alive, chunked transfer, Content-Length
- **Middleware pipeline** — logging, CORS, gzip, rate limiting, custom middleware
- **Routing** — exact, path params (`:id`), wildcards (`*`, `**`)
- **Static file serving** — directory-based with MIME detection
- **Request body parsing** — JSON, form-urlencoded, plain text
- **Cookie support** — parse/set with `HttpOnly`, `Max-Age`, `Path`
- **Redirect helper** — 301/302 via `HttpResponse.redirect()`
- **Graceful shutdown** — SIGINT hook
- **QR generation** (miniQR), **APK metadata** (miniApkReader), **static sites** (miniStaticServer)

## Quick Start

```java
HttpServer server = new HttpServer(8080);

server.use(new AccessLogMiddleware());
server.use(new CorsMiddleware().allowOrigin("*"));

server.addRoute(HttpMethod.GET, "/", req ->
    new HttpResponse.Builder()
        .setContentType(ContentType.HTML)
        .setStatusCode(200)
        .setBody("<h1>Hello!</h1>")
        .build()
);

server.addRoute(HttpMethod.GET, "/hello/:name", req -> {
    String name = req.getParameters().get("name");
    return new HttpResponse.Builder()
        .setContentType(ContentType.TEXT)
        .setBody("Hello, " + name + "!")
        .build();
});

server.run();
```

## Reading Guide for Students

If you are a student wanting to learn how university concepts apply to a real project, this guide will help you navigate:

1. **Start with the HTTP protocol** — read [The HTTP Protocol — Fundamentals](knowledge/http-protocol.en.md) to understand the foundation everything is built on.
2. **Explore the source code** — the most important files are in `miniJWS-core/src/main/java/`. Begin with `HttpServer.java` (the main loop), then `HttpDecoder.java` (how requests are parsed) and `HttpEncoder.java` (how responses are written).
3. **Study the patterns** — the [Knowledge Base](knowledge/index.md) documents Builder, Chain of Responsibility, Strategy, and Immutability patterns with real code examples from the project.
4. **Review design decisions** — every important decision (why not NIO, why immutable messages, why `CopyOnWriteArrayList`) is explained in [Design Decisions](knowledge/decisions.md).
5. **Check the Java classes used** — the [Java API Reference](knowledge/java-api/index.md) explains each JDK class used in the project and why it was chosen.
6. **Put it into practice** — follow the [Getting Started Guide](guides/getting-started.md) to create your own server and experiment by modifying the code.

## Documentation Sections

- [Architecture Overview](architecture.md) — request flow, module dependencies, wildcard routing, thread safety
- [Modules](modules/) — details per module
- [API Reference](api/) — HttpServer, HttpRequest, HttpResponse, Routing
- [Getting Started](guides/getting-started.md) — build, run, create your own server
- [Configuration](guides/configuration.md) — middleware options, keep-alive, thread pool
- [Deployment](guides/deployment.md) — fat JAR, systemd, security notes
- [Knowledge Base](knowledge/index.md) — deep-dive into patterns, decisions, classes, and architecture
- [Java API Reference](knowledge/java-api/index.md) — comprehensive Java language and API docs used in the project
