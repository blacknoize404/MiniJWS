# Configuration

## Server Configuration

The `HttpServer` constructor accepts:

```java
// Port only — thread pool = 2 * CPU cores
new HttpServer(8080);

// Custom thread pool size
new HttpServer(8080, 50);
```

## Thread Pool

The thread pool is a fixed-size pool:
- Default: `Runtime.getRuntime().availableProcessors() * 2`
- Each connection gets its own thread
- Threads are reused via `Executors.newFixedThreadPool()`

## Keep-Alive

HTTP/1.1 persistent connections are enabled by default:
- **Max requests per connection:** 100 (`HttpServer.MAX_KEEPALIVE_REQUESTS`)
- **Idle timeout:** 10 seconds (`HttpServer.KEEPALIVE_TIMEOUT_MS`)
- Respects `Connection: close` header

## Graceful Shutdown

When `stop()` is called:
1. `running` flag is set to false
2. `ServerSocket.close()` throws in `accept()`, ending the accept loop
3. `shutdown()` drains the thread pool with 5-second grace period
4. A shutdown hook via `Runtime.getRuntime().addShutdownHook()` auto-registers on `run()`

```java
server.run();                    // with SIGINT hook
server.run(false);               // without hook (manual stop())
```

## Middleware Pipeline

Middleware runs in registration order:

```java
server.use(new AccessLogMiddleware());   // runs first
server.use(new CorsMiddleware());        // runs second
server.use(new RateLimitMiddleware(100, 60)); // runs third
```

Each middleware can short-circuit the chain by returning a response directly.

## Built-in Middleware Options

### CorsMiddleware
| Method | Default | Description |
|--------|---------|-------------|
| `allowOrigin(String)` | `*` | Allowed origin |
| `allowMethods(String...)` | GET, POST, PUT, DELETE, OPTIONS, PATCH | Allowed methods |
| `allowHeaders(String...)` | Content-Type, Authorization | Allowed headers |
| `allowCredentials(boolean)` | false | Credentials flag |
| `maxAge(int)` | -1 | Preflight cache seconds |

### RateLimitMiddleware
| Parameter | Description |
|-----------|-------------|
| `maxRequests` | Max requests in the window |
| `windowSeconds` | Window duration in seconds |

### GzipMiddleware
| Parameter | Description |
|-----------|-------------|
| `level` | Compression level 1-9 (default 6) |

## Content Types

File extensions are mapped to MIME types in `ContentTypes.EXTENSION_TO_MIME`:

| Extension | MIME Type |
|-----------|-----------|
| `html` | `text/html;charset=utf-8` |
| `css` | `text/css;charset=utf-8` |
| `js` | `text/javascript;charset=utf-8` |
| `json` | `application/json;charset=utf-8` |
| `png` | `image/png` |
| `svg` | `image/svg+xml` |
| `mp4` | `video/mp4` |
| `apk` | `application/vnd.android.package-archive` |

Additional aliases in `ContentType.EXT_MAP`:
- `jpg` → `JPEG`
- `jpeg` → `JPEG`
- `tiff` → `FILE` (generic binary)

## Static File Handler

```java
new StaticFileHandler("./public");
new StaticFileHandler("./public", "index.html", "index.htm"); // custom index files
```

Features:
- MIME detection via file extension
- Index file serving for directories
- Path traversal prevention (`../` blocked)
- 404 for missing files

## Environment Variables

There are no environment variable dependencies. All configuration is done via constructors and method calls.

---

[← Previous](getting-started.en.md) · [Next →](deployment.en.md)  
[🇪🇸 Español](configuration.md) · [🇬🇧 English](configuration.en.md)
