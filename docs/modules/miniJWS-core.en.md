# Module: miniJWS-core

The core HTTP server library.

## Dependencies

- `org.jetbrains:annotations:24.1.0` (compile scope)

## HttpServer

The main server class. Manages routing, middleware, keep-alive, and graceful shutdown.

### Constructor

| Constructor | Description |
|-------------|-------------|
| `HttpServer(int port)` | Uses `2 * CPU cores` threads |
| `HttpServer(int port, int parallelism)` | Custom thread pool size |

### Methods

| Method | Description |
|--------|-------------|
| `addRoute(HttpMethod, String, RequestRunner)` | Register a route handler |
| `removeRoute(HttpMethod, String)` | Unregister a route |
| `use(Middleware)` | Register middleware in the pipeline |
| `run()` | Start server (blocks, with shutdown hook) |
| `run(boolean addShutdownHook)` | Start with optional shutdown hook |
| `stop()` | Gracefully stop (closes socket, drains pool) |
| `idle()` | Put main thread to wait (for daemon mode) |

### Routing

```java
// Exact match
server.addRoute(HttpMethod.GET, "/api/users", request -> {
    return new HttpResponse.Builder()
            .setStatusCode(200)
            .setContentType(ContentType.JSON)
            .setBody("{\"users\":[]}")
            .build();
});

// Path parameters
server.addRoute(HttpMethod.GET, "/users/:id", request -> {
    String id = request.getParameters().get("id");
    // ...
});
```

### Middleware

```java
server.use(new AccessLogMiddleware());          // request logging
server.use(new CorsMiddleware().allowOrigin("*")); // CORS headers
server.use(new RateLimitMiddleware(100, 60));   // 100 req/min per IP
```

## HttpRequest

Immutable request object with:

```java
request.getHttpMethod();          // GET, POST, etc.
request.getUri();                 // parsed URI
request.getHeaders();             // Map<String, List<String>>
request.getParameters();          // query + path params
request.getCookies();             // parsed Cookie header
request.getBody();                // Optional<byte[]>
request.getHeader("Content-Type");// Optional<String>

// Body parsing
request.bodyAsString();           // Optional<String> (UTF-8)
request.bodyAsForm();            // Optional<Map<String, String>>
request.bodyAsJson();            // Optional<Map<String, String>> (flat)
```

## HttpResponse

Immutable response object with:

```java
// Builder
new HttpResponse.Builder()
    .setStatusCode(200)
    .setContentType(ContentType.HTML)
    .addHeader("Cache-Control", "no-cache")
    .setBody("<h1>OK</h1>")
    .build();

// Redirect helper
HttpResponse.redirect("/login");          // 302
HttpResponse.redirect("/new-url", 301);   // 301

// Cookies
new HttpResponse.Builder()
    .setCookie("session", "abc123")
    .setCookie("token", "xyz", 3600, "/", true)
    .build();
```

## HttpDecoder

Parses raw bytes from an `InputStream` into an `Optional<HttpRequest>`.

```java
HttpDecoder.decode(inputStream);   // InputStream → Optional<HttpRequest>
HttpDecoder.decode(bufferedStream);// BufferedInputStream → Optional<HttpRequest>
```

Supports:
- Request line parsing (method, URI, protocol)
- Multi-value header parsing
- Query parameter extraction
- Content-Length body reading
- Chunked transfer encoding

## HttpEncoder

Serializes an `HttpResponse` to an `OutputStream`. Auto-detects content type to choose between text and binary encoding.

## ContentType Enum

Maps file extensions to MIME types:

| Constant | Extension(s) | MIME |
|----------|-------------|------|
| `HTML` | html | `text/html;charset=utf-8` |
| `JSON` | json | `application/json;charset=utf-8` |
| `JPEG` | jpg, jpeg | `image/jpeg` |
| `PNG` | png | `image/png` |
| `TEXT` | txt | `text/plain;charset=utf-8` |
| ... | ... | ... |

## Built-in Handlers

### StaticFileHandler

```java
server.addRoute(HttpMethod.GET, "/*", new StaticFileHandler("./public"));
```

Serves files from a directory with MIME detection, index file support, and path traversal prevention.

## Built-in Middleware

| Middleware | Description |
|------------|-------------|
| `AccessLogMiddleware` | Apache Common Log Format logging |
| `CorsMiddleware` | CORS headers with configurable origin, methods, headers |
| `GzipMiddleware` | Response compression with configurable level |
| `RateLimitMiddleware` | Per-IP rate limiting with configurable max/window |

## Keep-Alive

HTTP/1.1 persistent connections are enabled by default. The server reuses the same socket for up to 100 requests or 10 seconds of idle time.

## Thread Safety

- Routes use `ConcurrentHashMap` — safe to modify at runtime
- Each connection runs on a thread from the fixed pool
- Middleware runs on the connection handler thread

---

[← Home](../index.en.md) · [Next →](miniJWS-demo.en.md)  
[🇪🇸 Español](miniJWS-core.md) · [🇬🇧 English](miniJWS-core.en.md)
