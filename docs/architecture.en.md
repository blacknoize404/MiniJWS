# Architecture

## Overview

MiniJWS follows a modular architecture where each module is an independent Maven project
with a well-defined responsibility. Each module is self-contained with its own POM.

## Module Dependency Graph

```
miniJWS-core (no dependencies)
    |
    ├── miniJWS-demo ──► miniJWS-core
    ├── miniStaticServer ──► miniJWS-core, miniQR (optional)
    |
    miniApkReader (uses net.dongliu:apk-parser)
    |
    miniQR (uses com.google.zxing, org.jfree:jfreesvg)
```

## Core Architecture (miniJWS-core)

```
┌───────────────────────────────────────────────────────────────┐
│                         HttpServer                            │
│  ┌──────────┐  ┌──────────┐  ┌────────────┐  ┌──────────────┐ │
│  │  Routes  │  │ Thread   │  │ServerSocket│  │ Middleware   │ │
│  │  (Map)   │  │ Pool     │  │            │  │ List         │ │
│  └────┬─────┘  └──────────┘  └────────────┘  └──────┬───────┘ │
│       │                                              │        │
│  ┌────▼──────────────────────────────────────────────▼──────┐ │
│  │                  Request Lifecycle                       │ │
│  │                                                          │ │
│  │  ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────┐  │ │
│  │  │  Decoder │──►│Middleware│──►│  Runner  │──►│Encoder│  │ │
│  │  │  (parse) │   │ (chain)  │   │  (route) │   │(write)│  │ │
│  │  └──────────┘   └──────────┘   └──────────┘   └──────┘  │ │
│  └──────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────┘
```

### Request Flow

1. **Accept**: `HttpServer` accepts a TCP connection via `ServerSocket`
2. **Keep-Alive loop**: If HTTP/1.1 without `Connection: close`, the socket is reused
3. **Decode**: `HttpDecoder` parses the raw HTTP request into an `HttpRequest` object
4. **Middleware chain**: Each registered middleware runs in order (logging, CORS, rate limit, etc.)
5. **Route**: Match order: exact → path params (`:id`) → wildcard single (`*`) → wildcard glob (`**`)
6. **Execute**: The matching `RequestRunner` is called with the request
7. **Encode**: `HttpEncoder` serializes the `HttpResponse` back to the client
8. **Repeat**: If keep-alive, go back to step 3; otherwise close connection

### Package Structure (miniJWS-core)

```
io.github.blacknoize404.miniJWS/
├── HttpServer.java              # Main server class
├── DemoServer.java              # Basic example (legacy)
├── primitives/
│   ├── HttpMethod.java          # HTTP method enum
│   ├── HttpStatusCode.java      # Status code definitions
│   ├── ContentType.java         # MIME type enum
│   ├── RequestRunner.java       # Route handler interface
│   ├── Middleware.java          # Middleware interface
│   └── MiddlewareChain.java     # Chain interface
├── requests/
│   ├── HttpRequest.java         # Request model (Builder pattern, body parsing, cookies)
│   └── HttpDecoder.java         # Request parser
├── responses/
│   ├── HttpResponse.java        # Response model (Builder pattern, redirect factory, cookies)
│   └── HttpEncoder.java         # Response serializer
├── middleware/
│   ├── CorsMiddleware.java      # CORS headers & preflight
│   ├── AccessLogMiddleware.java # Apache-style request logging
│   ├── GzipMiddleware.java      # Response compression
│   └── RateLimitMiddleware.java # Per-IP rate limiting
├── handlers/
│   └── StaticFileHandler.java   # Directory-based file serving
├── headers/
│   ├── Header.java              # HTTP header model
│   ├── Field.java               # Header field parser
│   └── Parameter.java           # Header parameter parser
└── content/
    └── ContentTypes.java        # Extension-to-MIME mapping
```

### Middleware Pipeline

Middleware is executed in registration order. Each middleware can:
- Inspect and modify the request
- Short-circuit the chain (return a response immediately)
- Inspect and modify the response
- Execute code before and after the handler

```
Request ──► Middleware 1 ──► Middleware 2 ──► ... ──► Route Handler ──► Response
                  │                │                          │
                  ▼                ▼                          ▼
            (log request)   (check CORS)              (handle route)
                  │                │                          │
                  ◄────────────────┼──────────────────────────┘
                                   │
                                   ▼
                            (add CORS headers)
```

### Keep-Alive Connections

HTTP/1.1 connections are persistent by default. The server:
- Reuses the same `BufferedInputStream` across multiple requests on the same socket
- Closes after `Connection: close` header, idle timeout (10s), or 100 requests
- Sets `Connection: keep-alive` on each response

### Wildcard Routing

Routes can use wildcards for flexible matching:

- `*` — matches a single path segment (e.g. `/*` matches `/any-single-segment`)
- `**` — matches all remaining segments (e.g. `/files/**` matches `/files/a/b/c`)

Use `addStaticRoute()` for convenience when serving static files with `/*`:

```java
server.addStaticRoute("/*", new StaticFileHandler("./public"));
```

### Thread Safety

- Routes use `ConcurrentHashMap` — safe to modify at runtime
- Middleware list uses `CopyOnWriteArrayList` — thread-safe for read-heavy workloads
- Each connection runs on a separate thread
- Thread pool uses `Executors.newFixedThreadPool()`
- Graceful shutdown uses `CountDownLatch` — `idle()` awaits the latch, `stop()` counts it down
