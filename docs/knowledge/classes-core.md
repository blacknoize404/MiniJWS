# Core Classes

## HttpServer

**Package:** `io.github.blacknoize404.miniJWS.HttpServer`
**File:** `miniJWS-core/src/main/java/.../HttpServer.java`

The central orchestrator. Manages the `ServerSocket`, thread pool, routing table, middleware chain, and connection lifecycle.

### Key Fields

| Field | Type | Purpose |
|-------|------|---------|
| `routes` | `Map<String, RequestRunner>` | `ConcurrentHashMap` — key = `METHOD:/path` |
| `middlewares` | `List<Middleware>` | `CopyOnWriteArrayList` — thread-safe for read-heavy workloads |
| `socket` | `ServerSocket` | TCP acceptor |
| `threadPool` | `ExecutorService` | Fixed thread pool (default: `2 × CPU cores`) |
| `running` | `AtomicBoolean` | Controls the accept loop |
| `shutdownLatch` | `CountDownLatch` | Blocks `idle()` until `stop()` fires |

### Lifecycle

1. **Constructor:** Opens `ServerSocket`, creates thread pool
2. **Configuration:** `addRoute()`, `addStaticRoute()`, `use()` — all return `this` (fluent)
3. **`run()`:** Sets `running=true`, registers SIGINT hook, enters accept loop. Each accepted socket is dispatched to `handleConnection()` via the thread pool.
4. **`stop()`:** Sets `running=false`, counts down the latch, closes the socket
5. **`idle()`:** Blocks awaiting the latch (for daemon/server mode)
6. **Shutdown:** `run()` calls `shutdown()` after the accept loop exits, draining the thread pool

### Keep-Alive Loop (`handleConnection()`)

```
accept → for(up to 100 requests):
           decode(request) → middleware chain → encode(response)
           if Connection:close or timeout → break
         close socket
```

### Middleware Chain Construction

The `buildChain()` method creates a nested lambda chain. The last registered middleware runs first (wraps the terminal). The terminal middleware does route matching:

1. Exact match (`routes.get(key)`)
2. Path param match (`findRouteWithParams()` — linear scan of all routes)
3. 404

### Route Matching (`matchPath()`)

```java
static Map<String, String> matchPath(String pattern, String path)
```

Segment-by-segment comparison:
- Static segment: must match exactly
- `:param`: captures the segment into the params map
- `*`: matches any single segment (no capture)
- `**`: matches all remaining segments, returns immediately

---

## HttpRequest

**Package:** `io.github.blacknoize404.miniJWS.requests.HttpRequest`
**File:** `miniJWS-core/src/main/java/.../requests/HttpRequest.java`

Immutable request model built by the `Builder` inner class.

### Fields

| Field | Type | Description |
|-------|------|-------------|
| `httpMethod` | `HttpMethod` | Enum: GET, POST, etc. |
| `uri` | `URI` | Parsed URI (path only, query stripped) |
| `protocolVersion` | `String` | e.g. `HTTP/1.1` |
| `headers` | `Map<String, List<String>>` | Multi-valued headers (case-sensitive keys as received) |
| `parameters` | `Map<String, String>` | Merged query + path parameters (path params take precedence) |
| `cookies` | `Map<String, String>` | Parsed from `Cookie` header at construction time |
| `body` | `Optional<byte[]>` | Raw body bytes |

### Cookie Parsing

`parseCookies()` runs during construction. It splits the `Cookie` header on `;`, then each pair on `=`:

```
Cookie: session=abc123; token=xyz
→ {session: "abc123", token: "xyz"}
```

### Body Parsing Methods

| Method | Implementation |
|--------|---------------|
| `bodyAsString()` | `new String(body, UTF_8)` |
| `bodyAsForm()` | Splits on `&`, then `=`, URL-decodes each part |
| `bodyAsJson()` | Hand-written flat JSON parser (no external lib) — key:value pairs split on `,`, handles quoted strings and escaping |

The JSON parser is intentionally **flat** — it only handles one level of `{"key": "value", ...}`. Nested objects or arrays are skipped (depth tracking skips content inside `{}` and `[]`).

---

## HttpResponse

**Package:** `io.github.blacknoize404.miniJWS.responses.HttpResponse`
**File:** `miniJWS-core/src/main/java/.../responses/HttpResponse.java`

Immutable response model built by the `Builder` inner class.

### Default Headers

Every response automatically includes:
- `Server: MiniJWS` (from `HttpServer.SERVER_NAME`)
- `Date: <RFC 1123 timestamp>` (generated at build time)

### Redirect Factory

```java
HttpResponse.redirect("/login");         // 302 Found
HttpResponse.redirect("/new-url", 301);  // 301 Moved Permanently
```

Both build a response with `Location` header and no body.

### Cookie Support

```java
builder.setCookie("name", "value");                         // simple
builder.setCookie("name", "value", 3600, "/", true);        // with Max-Age, Path, HttpOnly
```

Internally adds a `Set-Cookie` header. The full-featured variant constructs `name=value; Max-Age=3600; Path=/; HttpOnly`.

### Builder

```java
new HttpResponse.Builder()
    .setStatusCode(200)
    .setContentType(ContentType.JSON)
    .addHeader("Cache-Control", "no-cache")
    .setBody("{\"ok\":true}")
    .build();
```

---

## HttpDecoder

**Package:** `io.github.blacknoize404.miniJWS.requests.HttpDecoder`
**File:** `miniJWS-core/src/main/java/.../requests/HttpDecoder.java`

Static utility class that parses `InputStream → Optional<HttpRequest>`.

### Parsing Algorithm

1. **Request Line:** `readLine()` → split on space → `HttpMethod`, `URI`, protocol
2. **Headers:** Loop `readLine()` until empty line. Each line split on `:`. `Content-Length` and `Transfer-Encoding` are tracked during header parsing.
3. **Obs-fold:** Lines starting with space/tab are continuations of the previous header value.
4. **Body:**
   - If `Transfer-Encoding: chunked` → `readChunkedBody()` (parse hex size, strip extensions, read chunk, skip CRLF)
   - If `Content-Length > 0` → `readExactBody()` (blocking read exact bytes)
5. **URI:** query portion parsed separately → `parseQueryParams()` with `URLDecoder.decode(UTF_8)`

### readLine() Implementation

Byte-by-byte loop accumulating bytes until `\r\n` is found:

```java
while ((b = in.read()) != -1) {
    if (b == CR) { crFound = true; }
    else if (b == LF) { return buf.toString(US_ASCII); }
    else { if (crFound) buf.write(CR); buf.write(b); }
}
```

Max line length: `8_192` bytes (returns `null` if exceeded).

### Size Limits

- `MAX_CHUNK_SIZE`: 10 MiB per chunk
- `MAX_CONTENT_LENGTH`: 50 MiB total body
- Duplicate `Content-Length` → reject (return empty)

---

## HttpEncoder

**Package:** `io.github.blacknoize404.miniJWS.responses.HttpEncoder`
**File:** `miniJWS-core/src/main/java/.../responses/HttpEncoder.java`

Static utility class that serializes `HttpResponse → OutputStream`.

### Serialization Order

1. Status line: `HTTP/1.1 200 OK\r\n`
2. All headers: `Key: value\r\n`
3. Content-Length header (if body present)
4. Empty line `\r\n`
5. Body bytes (written directly to `OutputStream`, not through the text writer)

**Critical detail:** Headers are written through a `BufferedWriter(OutputStreamWriter(outputStream, US_ASCII))` and flushed before the body is written directly via `outputStream.write(data)`. This prevents the US-ASCII writer from corrupting binary content (gzip, images, UTF-8 multi-byte sequences).

---

## StaticFileHandler

**Package:** `io.github.blacknoize404.miniJWS.handlers.StaticFileHandler`
**File:** `miniJWS-core/src/main/java/.../handlers/StaticFileHandler.java`

Implements `RequestRunner` to serve files from a directory.

### Security

Three-layer path traversal protection:
1. **String check:** `rawPath.contains("..")` → 400
2. **Normalize check:** `baseDir.resolve(relative).normalize().startsWith(baseDir)` → 403
3. **Race condition:** `NoSuchFileException` caught → 404 instead of 500

### Features

- Directory index files (default: `index.html`, configurable)
- MIME detection via `ContentType.fromExtension()`
- Binary file support (raw bytes)

### File → Content-Type Resolution

Uses `ContentType.fromExtension(ext)` which maps file extension to MIME type via the `EXT_MAP` in `ContentType`. Unknown extensions default to `application/octet-stream`.

---

[← Previous](decisions.md) · [Next →](classes-middleware.md)  
[🇪🇸 Español](classes-core.md) · [🇬🇧 English](classes-core.md)
