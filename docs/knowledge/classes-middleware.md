# Middleware Classes

All middleware implement the `Middleware` functional interface:

```java
@FunctionalInterface
public interface Middleware {
    HttpResponse run(HttpRequest request, MiddlewareChain chain);
}
```

They are registered via `server.use(middleware)` and executed in registration order.

---

## AccessLogMiddleware

**File:** `miniJWS-core/src/main/java/.../middleware/AccessLogMiddleware.java`

Apache Common Log Format logging with asynchronous I/O.

### Architecture

```
Request Thread                  Worker Thread (daemon)
    │                                │
    ├── formatLogLine()              │
    ├── queue.offer(line) ──────────►├── queue.take() (blocks)
    │                                ├── writer.println(line)
    │                                ├── writer.flush()
    │                                └── loop
    └── return response
```

### Constructor Options

| Constructor | Output Target |
|-------------|---------------|
| `AccessLogMiddleware()` | `System.out` (wrapped in `PrintWriter`) |
| `AccessLogMiddleware(String filePath)` | File (append, UTF-8) |
| `AccessLogMiddleware(PrintWriter writer)` | Custom writer |

### Log Format

```
127.0.0.1 - - [13/Jun/2026:14:30:00 +0000] "GET /hello HTTP/1.1" 200 13 (2ms)
```

Fields: remote IP (respects `X-Forwarded-For`), timestamp, request line, status code, body size, elapsed ms.

### Key Design Points

- `BlockingQueue<String>` (capacity 16_384) decouples request handling from log I/O
- `queue.offer()` never blocks (returns false if full, line is dropped)
- Worker is a daemon thread (doesn't prevent JVM exit)
- Shutdown hook drains remaining entries with `flushRemaining()`

### Shutdown Hook

```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    worker.interrupt();
    worker.join(2_000);  // wait up to 2s for flush
    flushRemaining(writer);
}));
```

---

## CorsMiddleware

**File:** `miniJWS-core/src/main/java/.../middleware/CorsMiddleware.java`

CORS (Cross-Origin Resource Sharing) implementation.

### Configuration Methods

| Method | Default | Description |
|--------|---------|-------------|
| `allowOrigin(String)` | `*` | Allowed origin |
| `allowMethods(String...)` | GET, POST, PUT, DELETE, OPTIONS, PATCH | Allowed methods |
| `allowHeaders(String...)` | Content-Type, Authorization | Allowed headers |
| `allowCredentials(boolean)` | `false` | Whether to send credentials |
| `maxAge(int)` | `-1` | Preflight cache duration |

### Request Flow

1. **No Origin header** → skip (pass through to next middleware)
2. **OPTIONS request** → return 204 with CORS headers (preflight)
3. **Normal request** → call `chain.next()`, add CORS headers to response

### Special Handling: `*` + Credentials

The CORS spec forbids `Access-Control-Allow-Origin: *` when `Access-Control-Allow-Credentials: true`. When `allowCredentials(true)` is called with `allowOrigin("*")`:
- `allowCredentials(true)` **throws** `IllegalStateException` immediately (fail-fast)
- Workaround: use `allowOrigin("https://specific.domain")` with credentials, or call `allowCredentials(true)` before `allowOrigin("*")` (order matters because `allowCredentials()` validates on call — you can set credentials first, then origin to `*` to skip validation)

Wait — actually looking at the code:

```java
public CorsMiddleware allowCredentials(boolean allow) {
    if (allow && "*".equals(allowOrigin)) {
        throw new IllegalStateException(...);
    }
    this.allowCredentials = allow;
    return this;
}
```

The order matters: `allowCredentials(true)` throws if `*`. But `allowCredentials(false)` → then `allowOrigin("*")` works. And then `allowCredentials(true)` would throw. So the safe sequence is `allowCredentials(true)` first, then `allowOrigin("specific-domain")`.

### Header Copying

When adding CORS headers to an existing response, the middleware rebuilds the response (copies all headers, status, method, body) to preserve immutability.

---

## GzipMiddleware

**File:** `miniJWS-core/src/main/java/.../middleware/GzipMiddleware.java`

Response compression for clients that accept gzip encoding.

### Algorithm

1. Check `Accept-Encoding` header — if no `gzip`, pass through
2. Call `chain.next(request)` to get the response
3. Skip if: body empty, already `Content-Encoding`, or body < 256 bytes
4. Compress with `GZIPOutputStream`
5. Skip if compressed ≥ original size
6. Build new response with `Content-Encoding: gzip`

### Compression Tuning

```java
new GzipMiddleware();    // default level 6
new GzipMiddleware(9);   // max compression (slower)
new GzipMiddleware(1);   // fastest, least compression
```

### Level Setting

```java
private final int level;
public GzipMiddleware(int level) {
    this.level = Math.max(1, Math.min(9, level));
}
```

The level is applied via an anonymous subclass:

```java
var gz = new GZIPOutputStream(bos) {{
    def.setLevel(level);
}};
```

### Double Compression Guard

```java
boolean alreadyEncoded = response.getHeaders().keySet().stream()
    .anyMatch(k -> k.equalsIgnoreCase("Content-Encoding"));
if (alreadyEncoded) return response;
```

Prevents compressing an already-compressed response.

### try-finally on GZIPOutputStream

```java
try {
    gz.write(data);
} finally {
    gz.close();  // ensures trailer is written even if write fails
}
```

The `close()` call writes the gzip trailer (CRC32 + size). Without it, the stream is incomplete.

---

## RateLimitMiddleware

**File:** `miniJWS-core/src/main/java/.../middleware/RateLimitMiddleware.java`

Per-IP rate limiting using a sliding window.

### Data Structure

```java
ConcurrentHashMap<String, Queue<Instant>> requests = new ConcurrentHashMap<>();
AtomicInteger totalEntries = new AtomicInteger(0);
```

Each IP has a `ConcurrentLinkedQueue<Instant>` of request timestamps.

### Algorithm

1. Extract client IP (respects `X-Forwarded-For`, `X-Real-IP`)
2. Get or create the timestamp queue for that IP
3. `synchronized(queue)`:
   - Remove timestamps older than the window
   - If queue size ≥ `maxRequests` → return 429
   - Otherwise, add current timestamp
4. If `totalEntries > CLEANUP_THRESHOLD` (10_000), trigger `cleanupStaleEntries()`

### Cleanup (Memory Leak Prevention)

```java
private void cleanupStaleEntries(Instant cutoff) {
    for (var it = requests.entrySet().iterator(); it.hasNext();) {
        var entry = it.next();
        Queue<Instant> q = entry.getValue();
        synchronized (q) {
            while (!q.isEmpty() && q.peek().isBefore(cutoff)) {
                q.poll();
            }
            if (q.isEmpty()) {
                it.remove();         // remove IP from map
                removed++;
            }
        }
    }
    totalEntries.addAndGet(-removed);
}
```

Without this cleanup, inactive IPs accumulate in the `ConcurrentHashMap` forever, causing a memory leak. The threshold-based cleanup (`> 10_000` entries) ensures bounded memory usage.

### 429 Response

```java
return new HttpResponse.Builder()
    .setStatusCode(429)
    .setContentType(ContentType.TEXT)
    .addHeader("Retry-After", String.valueOf(window.toSeconds()))
    .setBody("429 - Too Many Requests")
    .build();
```

---

[← Previous](classes-core.md) · [Next →](classes-support.md)  
[🇪🇸 Español](classes-middleware.md) · [🇬🇧 English](classes-middleware.md)
