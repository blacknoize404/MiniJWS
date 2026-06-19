# MiniJWS

![Java 25+](https://img.shields.io/badge/Java-25+-orange?logo=openjdk&logoColor=white)
![Maven 3.8+](https://img.shields.io/badge/Maven-3.8+-C71A36?logo=apachemaven&logoColor=white)
![CC BY-NC-SA 4.0](https://img.shields.io/badge/License-CC%20BY--NC--SA%204.0-lightgrey)

Versión en español [readme.es.md](README.es.md)

A lightweight modular Java HTTP server framework built with Java 25.

This project started during my second year of Computer Engineering as a personal initiative to deeply learn the HTTP protocol, exploring every aspect of the standard through its implementation from scratch.

> **Note:** Although the project is ready to use directly, its documentation also covers which Java classes are used and why, the design decisions made, and the patterns employed with their rationale, aiming to serve as a resource where students can see university concepts applied in a real project.

## Why MiniJWS?

In an ecosystem dominated by Spring Boot, Javalin, Spark, and Helidon, MiniJWS offers a lightweight, understandable, and extensible alternative that prioritises transparency over magic.

### vs the competition

| Aspect                   | Spring Boot | Javalin / Spark  | Helidon    | **MiniJWS**                                       |
|--------------------------|-------------|------------------|------------|---------------------------------------------------|
| Size                     | ~50 MB base | ~10 MB           | ~30 MB     | **~200 KB**                                       |
| Startup                  | 3-6 s       | 0.5-1 s          | 1-2 s      | **< 100 ms**                                      |
| Annotations / reflection | Extensive   | Minimal          | Moderate   | **None**                                          |
| Learning curve           | High        | Medium           | High       | **Low**                                           |
| Source code              | ~15 M lines | ~50 K lines      | ~1 M lines | **~3 K lines**                                    |
| Cross-cutting modules    | Not native  | No               | No         | **Yes (miniQR, miniApkReader, miniStaticServer)** |
| Educational purpose      | No          | No               | No         | **Yes (full pedagogical documentation)**          |

While commercial alternatives solve enterprise problems with layers of abstraction, MiniJWS solves the problem of **understanding how an HTTP server works under the hood** — without sacrificing practical usefulness.

### What you can build

- **Embedded servers for IoT devices** — instant startup and minimal footprint
- **REST APIs for prototyping** — a functional API in 5 lines with zero configuration
- **Local dev tools** — test servers, mocks, admin panels
- **Smart static sites** — combine miniStaticServer with miniQR to generate pages with QR codes injected into HTML templates
- **Lightweight microservices** — each Maven module deploys independently, ideal for modular architectures
- **Metadata analysers** — miniApkReader extracts packages, permissions, and features from Android APK files over a REST API
- **Education & research** — readable, documented code deliberately crafted so students can see HTTP implemented without magic
- **Custom middleware** — the single-method `Middleware` interface lets you write your own pipeline in seconds

### Philosophy

MiniJWS does not compete with Spring Boot in the enterprise space. Its potential lies in being the right choice when you need:

1. **To understand** what is really happening in an HTTP request
2. **Full control** with no auto-configuration or classpath scanning
3. **Real modularity** — add QR, APK, or static serving as independent modules
4. **Immediate results** — a working server with no annotations, no Gradle, no framework giants

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
