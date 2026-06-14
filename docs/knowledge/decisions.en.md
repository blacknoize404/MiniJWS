# Design Decisions

This document captures the rationale behind key design decisions in MiniJWS.

---

## 1. Flat Multi-Module Layout (No Root POM)

**Decision:** Each module (`miniJWS-core`, `miniJWS-demo`, `miniQR`, etc.) is a standalone Maven project with its own `pom.xml`. There is no root POM or `modules/` parent directory.

**Rationale:**
- Modules can be built independently without a parent aggregator
- CI pipelines can build only the changed module
- Each module controls its own version, plugins, and dependencies
- Simpler IDE import (each module opens as its own project)

**Trade-off:** Build order must be managed manually (`miniJWS-core` → `miniJWS-demo` → `miniQR` → `miniStaticServer` → `miniApkReader`).

---

## 2. Immutable Request/Response Objects

**Decision:** `HttpRequest` and `HttpResponse` are immutable after construction.

**Rationale:**
- Thread safety without synchronization — requests flow through middleware chains on different threads
- Predictable behaviour — no middleware can modify the request after another middleware has read it
- Easy to cache or retry

**Trade-off:** Copy-on-modify is necessary when middleware needs to alter the request (e.g., `CorsMiddleware` and `findRouteWithParams` build new instances). This allocation cost is acceptable for HTTP workloads.

---

## 3. Builder Pattern for HTTP Messages

**Decision:** Use the Builder pattern instead of constructors or setters on mutable objects.

**Rationale:**
- Optional fields don't require overloaded constructors
- Fluent chaining improves readability
- `build()` enforces validation (`Objects.requireNonNull`) at a known point
- The builder is mutable; the built object is not — clean separation of construction vs. use

---

## 4. Middleware as Linked Chain (Wrapping)

**Decision:** Build the middleware chain by wrapping each middleware around the previous one (last-wrapping-first), rather than iterating an index through a list.

```java
// buildChain():
MiddlewareChain chain = terminal;
for (int i = middlewares.size() - 1; i >= 0; i--) {
    Middleware mw = middlewares.get(i);
    MiddlewareChain next = chain;
    chain = req -> mw.run(req, next);
}
```

**Rationale:**
- No index variable to manage during execution
- Works naturally with lambdas and closures
- Middleware can short-circuit by returning without calling `next()`
- Each middleware decides when/if to call `next()`, enabling pre/post processing

---

## 5. Line-by-Line BufferedInputStream Parsing (Not NIO)

**Decision:** `HttpDecoder` reads the HTTP request using `BufferedInputStream.readLine()` semantics implemented manually (byte-by-byte), rather than using Java NIO (`ByteBuffer`, `Channel`) or high-level parsers.

**Rationale:**
- Simple, predictable, blocking semantics
- No complex buffer management
- Works correctly with keep-alive sockets (unlike `available()` which returns 0 for rapid pipelined requests)
- Easy to enforce line length limits and detect malformed input

**Trade-off:** Slower than NIO for very high throughput, but adequate for moderate HTTP workloads.

---

## 6. Keep-Alive Loop in Connection Handler

**Decision:** The `handleConnection()` method loops over up to 100 requests on the same socket, rather than one request per connection.

**Rationale:**
- HTTP/1.1 defaults to persistent connections
- Reduces TCP handshake overhead
- The same thread handles all requests on one socket, improving locality
- Respects `Connection: close` and idle timeout

**Implementation details:**
- 100 max requests per connection (`MAX_KEEPALIVE_REQUESTS`)
- 10-second socket timeout (`KEEPALIVE_TIMEOUT_MS`)
- Sets `Connection: keep-alive` or `Connection: close` on each response
- Exits the loop on malformed requests, timeout, or close header

---

## 7. CopyOnWriteArrayList for Middleware

**Decision:** `middlewares` field uses `CopyOnWriteArrayList` instead of `ArrayList` or synchronized list.

**Rationale:**
- Middleware is typically registered at startup and rarely modified at runtime
- Read operations vastly outnumber writes once the server is running
- CopyOnWriteArrayList provides lock-free reads for all middleware evaluations
- The copy-on-write cost is paid only during `use()` calls, which is negligible

---

## 8. CountDownLatch for Graceful Shutdown

**Decision:** Use `CountDownLatch(1)` in `idle()`/`stop()` instead of `wait()`/`notify()`.

**Rationale:**
- `CountDownLatch` is a modern Java concurrency primitive with clear semantics
- No risk of missed notifications or spurious wake-ups
- `idle()` blocks the main thread until `stop()` is called (via SIGINT or programmatic)
- The latch is one-shot (counts down 1→0), matching the lifecycle

---

## 9. Async Logging with BlockingQueue

**Decision:** `AccessLogMiddleware` writes logs asynchronously via a `BlockingQueue` and a dedicated daemon thread.

**Rationale:**
- Log I/O (especially to files) can block the request thread
- The bounded queue (`16_384`) provides backpressure
- The daemon thread doesn't prevent JVM exit
- A shutdown hook flushes remaining entries on shutdown

---

## 10. Wildcard Routing (`*` and `**`)

**Decision:** Two wildcard levels — `*` for single segment, `**` for all remaining segments.

**Rationale:**
- `*` maps cleanly to file handlers (`/*` matches any one-level path)
- `**` is needed for recursive serving (`/assets/**` matches `/assets/css/main.css`)
- Match order: exact → path params → `*` → `**` — ensures predictable resolution

---

## 11. CORS `*` + Credentials Guard

**Decision:** `allowCredentials(true)` throws `IllegalStateException` if `allowOrigin("*")` is set.

**Rationale:**
- The CORS spec explicitly forbids `Access-Control-Allow-Origin: *` with credentials
- Enforcing this at configuration time (fail-fast) is better than silently producing invalid CORS headers at runtime

---

## 12. Raw Bytes for Body Encoding

**Decision:** `HttpEncoder` writes the body as raw bytes directly to the `OutputStream`, not through the ASCII writer.

**Rationale:**
- The `BufferedWriter` with `US-ASCII` charset corrupts non-ASCII bytes (multi-byte UTF-8, gzip compressed bytes)
- Headers are ASCII-safe, but bodies may be arbitrary binary data
- Solution: write headers through the writer, flush, then write body bytes directly via `outputStream.write(data)`

---

## 13. StaticFileHandler: Defence in Depth

**Decision:** Path traversal prevention is implemented at two levels.

**Rationale:**
- Explicit `..` check in the raw path string
- `Path.normalize()` + `startsWith(baseDir)` check after resolution
- `NoSuchFileException` catch for race conditions (file deleted between `isFile()` check and `readAllBytes()`)

---

## 14. No External Dependencies for Core

**Decision:** `miniJWS-core` depends only on `org.jetbrains:annotations` (for `@Nullable`/`@NotNull`). The core has zero runtime dependencies.

**Rationale:**
- Zero-dependency core is easier to audit, embed, and distribute
- Minimises classpath conflicts
- The middleware and handler implementations are optional (included in core for convenience but not as separate libraries)

---

## 15. Thread Pool: Fixed Size

**Decision:** Use `Executors.newFixedThreadPool()` with `2 * CPU cores` as default.

**Rationale:**
- Fixed pool prevents unbounded thread growth under load
- 2× CPU cores is a good default for I/O-bound HTTP servers
- The pool size is configurable via the second constructor parameter
- Shutdown uses `awaitTermination(5s)` before `shutdownNow()`

---

[← Previous](patterns.en.md) · [Next →](classes-core.en.md)  
[🇪🇸 Español](decisions.md) · [🇬🇧 English](decisions.en.md)
