# miniJWS-core

![Java 25+](https://img.shields.io/badge/Java-25+-orange?logo=openjdk&logoColor=white)
![Maven 3.8+](https://img.shields.io/badge/Maven-3.8+-C71A36?logo=apachemaven&logoColor=white)

Core HTTP/1.1 server library for MiniJWS — lightweight, modular, zero-dependency (except JetBrains annotations).

## Features

- **HTTP/1.1** with keep-alive, chunked transfer, Content-Length
- **Middleware pipeline** — logging, CORS, gzip, rate limiting, custom
- **Routing** — exact, path params (`:id`), wildcards (`*`, `**`)
- **Static file serving** — directory-based with MIME detection
- **Request body parsing** — JSON, form-urlencoded, plain text
- **Cookie support** — parse/set with `HttpOnly`, `Max-Age`, `Path`
- **Redirect helper** — 301/302 via `HttpResponse.redirect()`
- **Graceful shutdown** — SIGINT hook with `CountDownLatch`

## Package Structure

```
io.github.blacknoize404.miniJWS/
├── HttpServer.java              # Main server (thread pool, routing, middleware)
├── primitives/
│   ├── ContentType.java         # MIME type enum
│   ├── HttpMethod.java          # HTTP method enum
│   ├── HttpStatusCode.java      # Status code enum
│   ├── Middleware.java          # Middleware interface
│   ├── MiddlewareChain.java     # Chain interface
│   └── RequestRunner.java       # Route handler interface
├── requests/
│   ├── HttpDecoder.java         # Request parser (line-by-line)
│   └── HttpRequest.java         # Request model (Builder, body parsing, cookies)
├── responses/
│   ├── HttpEncoder.java         # Response writer
│   └── HttpResponse.java        # Response model (Builder, redirect, cookies)
├── middleware/
│   ├── AccessLogMiddleware.java # Apache-style async logging
│   ├── CorsMiddleware.java      # CORS with preflight
│   ├── GzipMiddleware.java      # Gzip compression
│   └── RateLimitMiddleware.java # Per-IP rate limiting
├── handlers/
│   └── StaticFileHandler.java   # Built-in static file server
├── headers/
│   ├── Header.java              # HTTP header model
│   ├── Field.java               # Header field parser
│   └── Parameter.java           # Header parameter parser
└── content/
    └── ContentTypes.java        # Extension-to-MIME mapping
```

## Quick Start

```java
HttpServer server = new HttpServer(8080);

server.use(new AccessLogMiddleware());
server.use(new CorsMiddleware().allowOrigin("*"));

server.addRoute(HttpMethod.GET, "/", req ->
    new HttpResponse.Builder()
        .setStatusCode(200)
        .setContentType(ContentType.HTML)
        .setBody("<h1>Hello!</h1>")
        .build()
);

server.run();
```

## Build

```bash
mvn clean install
```
