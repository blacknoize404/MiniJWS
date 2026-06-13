# MiniJWS

A lightweight modular Java HTTP server framework built with Java 25.

This project started during my second year of Computer Engineering as a personal initiative to deeply learn the HTTP protocol, exploring every aspect of the standard through its implementation from scratch.

> **Note:** Although the project is ready to use directly, its documentation also covers which Java classes are used and why, the design decisions made, and the patterns employed with their rationale, aiming to serve as a resource where students can see university concepts applied in a real project.

## Features

- **HTTP/1.1 server** with thread pool concurrency and keep-alive connections
- **Middleware pipeline** — logging, CORS, gzip, rate limiting, auth
- **Path parameters** — `/users/:id` style routing
- **Static file serving** — directory-based with MIME detection
- **Request body parsing** — JSON, form-urlencoded, plain text
- **Cookie support** — parse request cookies, set response cookies
- **Redirect helper** — 301/302 with `HttpResponse.redirect()`
- **Graceful shutdown** — SIGINT handler for clean stop
- **Modular architecture** — standalone Maven modules
- **QR code generation** (SVG/Image) via `miniQR` module
- **Android APK metadata** extraction via `miniApkReader` module
- **Static site server** with template injection via `miniStaticServer` module
- **Comprehensive documentation** in `docs/`

## Project Structure

```
MiniJWS/
├── miniJWS-core/                # Core HTTP server library
│   └── src/main/java/io/github/blacknoize404/miniJWS/
│       ├── HttpServer.java        # Main server (thread pool, routing, middleware)
│       ├── primitives/            # HttpMethod, ContentType, Middleware, RequestRunner
│       ├── requests/              # HttpRequest, HttpDecoder
│       ├── responses/             # HttpResponse, HttpEncoder
│       ├── middleware/            # CorsMiddleware, AccessLog, Gzip, RateLimit
│       ├── handlers/              # StaticFileHandler
│       ├── headers/               # Header, Field, Parameter parsing
│       └── content/               # MIME type mappings
├── miniJWS-demo/                 # Full demo server using all features
├── miniQR/                       # QR code generation (ZXing + JFreeSVG)
├── miniStaticServer/             # Static file server (+ QR template injection)
├── miniApkReader/                # Android APK metadata parser
├── public/                       # Demo static assets
├── docs/                         # Full documentation
│   ├── index.md                 # Documentation home
│   ├── architecture.md          # Module architecture & request flow
│   ├── modules/                 # Per-module documentation
│   ├── api/                     # API reference
│   └── guides/                  # Getting started, configuration, deployment
└── README.md
```

## Requirements

- Java 25+
- Maven 3.8+

## Building

```bash
# Build modules in dependency order
mvn clean install -f miniJWS-core/pom.xml
mvn clean install -f miniJWS-demo/pom.xml
mvn clean install -f miniQR/pom.xml
mvn clean install -f miniStaticServer/pom.xml
mvn clean install -f miniApkReader/pom.xml
```

## Quick Start

```java
HttpServer server = new HttpServer(8080);

// Middleware
server.use(new AccessLogMiddleware());
server.use(new CorsMiddleware().allowOrigin("*"));

// Routes
server.addRoute(HttpMethod.GET, "/", req ->
    new HttpResponse.Builder()
        .setStatusCode(200)
        .setContentType(ContentType.HTML)
        .setBody("<h1>Hello MiniJWS!</h1>")
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

## Run the Demo

```bash
# Start the full demo server on port 8080
mvn compile exec:java -f miniJWS-demo/pom.xml
```

Then open http://localhost:8080 in your browser.

## Modules

| Module | Description |
|--------|-------------|
| **[miniJWS-core](miniJWS-core/README.md)** | Core HTTP/1.1 server with middleware, routing, path params, static files, cookies, CORS |
| **[miniJWS-demo](miniJWS-demo/README.md)** | Complete demo server showcasing all features |
| **[miniQR](miniQR/README.md)** | QR code generation using ZXing with SVG output via JFreeSVG |
| **[miniStaticServer](miniStaticServer/README.md)** | Static file server with template placeholders and QR code injection |
| **[miniApkReader](miniApkReader/README.md)** | Android APK metadata extraction (package, version, permissions, features) |

## Documentation

Full documentation is available in the [`docs/`](docs/index.md) directory, including:
- [Architecture overview](docs/architecture.md)
- [Module details](docs/modules/)
- [API reference](docs/api/)
- [Getting started guide](docs/guides/getting-started.md)
- [Configuration](docs/guides/configuration.md)
- [Deployment](docs/guides/deployment.md)
- [Knowledge Base](docs/knowledge/index.md) — patterns used, design decisions, Java classes employed and why

## License

CC-BY-NC-SA 4.0
