# HttpServer API

`io.github.blacknoize404.miniJWS.HttpServer`

The main server class that accepts TCP connections, runs middleware, and dispatches HTTP requests.

## Constructor

| Constructor | Description |
|-------------|-------------|
| `HttpServer(int port)` | Creates server with `2 * CPU cores` threads |
| `HttpServer(int port, int parallelism)` | Creates server with custom thread pool size |

## Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `addRoute(HttpMethod, String, RequestRunner)` | `HttpServer` | Register a route handler (fluent) |
| `addStaticRoute(String, StaticFileHandler)` | `HttpServer` | Register a `/*` static file route (fluent) |
| `removeRoute(HttpMethod, String)` | `void` | Unregister a route |
| `use(Middleware)` | `HttpServer` | Register middleware in the pipeline |
| `run()` | `void` | Start the server (blocks, registers SIGINT hook) |
| `run(boolean addShutdownHook)` | `void` | Start with optional shutdown hook |
| `stop()` | `void` | Set running=false and close the socket |
| `idle()` | `void` | Put main thread to wait (for daemon mode) |

## Route Matching

Routes are matched in this order:
1. **Exact match** — `GET:/api/users` matches `/api/users`
2. **Path parameters** — `GET:/users/:id` matches `/users/42`
3. **Single wildcard** — `GET:/*` matches `/any-single-segment`
4. **Glob wildcard** — `GET:/assets/**` matches `/assets/a/b/c`

Path parameters are stored in `HttpRequest.getParameters()`:
- `request.getParameters().get("id")` → `"42"`
- Query parameters and path parameters are merged

### Route Key Format

Routes are stored internally as `METHOD:/path`. The path is normalized:
- Trailing `/` is removed (except for root `/`)

## Middleware

Middleware is executed in registration order. The `use()` method adds to the end of the chain.

```java
server.use(middleware1);  // runs first
server.use(middleware2);  // runs second, after middleware1 calls chain.next()
```

## Keep-Alive

HTTP/1.1 keep-alive is enabled by default:
- Max 100 requests per connection
- Idle timeout: 10 seconds
- Respects `Connection: close` header

## Thread Safety

- Routes use `ConcurrentHashMap` — safe to modify at runtime
- Middleware list uses `CopyOnWriteArrayList` — thread-safe at runtime
- Each connection runs on a separate thread
- Thread pool uses `Executors.newFixedThreadPool()`

---

[← Home](../index.en.md) · [Next →](http-request.en.md)  
[🇪🇸 Español](http-server.md) · [🇬🇧 English](http-server.en.md)
