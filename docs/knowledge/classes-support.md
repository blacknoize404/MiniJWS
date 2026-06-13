# Support Classes & Other Modules

## ContentType Enum

**File:** `miniJWS-core/src/main/java/.../primitives/ContentType.java`

Maps MIME types to an enum with extension-based lookup.

### Enum Values

| Constant | MIME Type |
|----------|-----------|
| `FILE` | `application/octet-stream` |
| `JSON` | `application/json;charset=utf-8` |
| `CSS` | `text/css;charset=utf-8` |
| `JS` | `text/javascript;charset=utf-8` |
| `HTML` | `text/html;charset=utf-8` |
| `XML` | `text/xml;charset=utf-8` |
| `TEXT` | `text/plain;charset=utf-8` |
| `SVG` | `image/svg+xml` |
| `ICO` | `image/x-icon` |
| `PNG` | `image/png` |
| `JPEG` | `image/jpeg` |
| `GIF` | `image/gif` |
| `WEBP` | `image/webp` |
| `MP4` | `video/mp4` |
| `WEBM` | `video/webm` |
| `WOFF2` | `font/woff2` |
| `TTF` | `font/ttf` |
| `PDF` | `application/pdf` |
| `ZIP` | `application/zip` |

### EXT_MAP (Extension Aliases)

```java
EXT_MAP.put("jpg", JPEG);    // enum is "JPEG", extension "jpg"
EXT_MAP.put("jpeg", JPEG);
EXT_MAP.put("tiff", FILE);   // no TIFF enum, map to generic binary
```

Built from `name().toLowerCase()` for all enum values, then overrides for aliases.

### Key Methods

```java
// Lookup by file extension
ContentType.fromExtension("html") → Optional(HTML)
ContentType.fromExtension("jpg")  → Optional(JPEG)

// Reverse lookup by MIME string
ContentType.fromMime("image/jpeg") → JPEG
```

---

## HttpMethod Enum

**File:** `miniJWS-core/src/main/java/.../primitives/HttpMethod.java`

Standard HTTP methods:

```java
public enum HttpMethod {
    GET, HEAD, POST, PUT, DELETE,
    CONNECT, OPTIONS, TRACE, PATCH
}
```

Used as route key component (`method + ":" + path`) and in `HttpRequest`.

---

## HttpStatusCode

**File:** `miniJWS-core/src/main/java/.../primitives/HttpStatusCode.java`

Static mapping of status codes to reason phrases:

| Status | Phrase |
|--------|--------|
| 100 | CONTINUE |
| 200 | OK |
| 204 | NO_CONTENT |
| 301 | MOVED_PERMANENTLY |
| 302 | FOUND |
| 400 | BAD_REQUEST |
| 401 | UNAUTHORIZED |
| 403 | FORBIDDEN |
| 404 | NOT_FOUND |
| 429 | TOO_MANY_REQUESTS |
| 500 | INTERNAL_SERVER_ERROR |
| ... | ... |

Used by `HttpEncoder` to write the status line: `HTTP/1.1 200 OK\r\n`.

---

## RequestRunner Interface

**File:** `miniJWS-core/src/main/java/.../primitives/RequestRunner.java`

```java
@FunctionalInterface
public interface RequestRunner {
    HttpResponse run(HttpRequest request);
}
```

Route handlers are `RequestRunner` instances. Typically expressed as lambdas:

```java
server.addRoute(HttpMethod.GET, "/", req ->
    new HttpResponse.Builder()
        .setStatusCode(200)
        .setBody("OK")
        .build()
);
```

---

## Middleware & MiddlewareChain Interfaces

**Files:**
- `miniJWS-core/src/main/java/.../primitives/Middleware.java`
- `miniJWS-core/src/main/java/.../primitives/MiddlewareChain.java`

```java
@FunctionalInterface
public interface Middleware {
    HttpResponse run(HttpRequest request, MiddlewareChain chain);
}

@FunctionalInterface
public interface MiddlewareChain {
    HttpResponse next(HttpRequest request);
}
```

`MiddlewareChain` is the "next link" in the chain. A middleware calls `chain.next(request)` to pass control downstream, optionally modifying the request or processing the response on return.

---

## ContentTypes (Extension-to-MIME)

**File:** `miniJWS-core/src/main/java/.../content/ContentTypes.java`

Legacy class (included for backward compatibility). Defines a static `Map<String, String>` mapping file extensions to MIME types. Overlaps with `ContentType` enum functionality but provides direct string-based lookups.

---

## Header Parsing Classes

### Header
**File:** `miniJWS-core/src/main/java/.../headers/Header.java`

Model class for HTTP header key-value parsing.

### Field
**File:** `miniJWS-core/src/main/java/.../headers/Field.java`

Parser for structured header fields (e.g., `Content-Type: text/html; charset=utf-8`).

### Parameter
**File:** `miniJWS-core/src/main/java/.../headers/Parameter.java`

Parser for header parameters (the `key=value` parts after `;`).

---

## Other Modules

### miniQR (QR Code Generator)

**Package:** `io.github.blacknoize404.miniQR`

Uses ZXing for QR generation and JFreeSVG for SVG output.

| Method | Description |
|--------|-------------|
| `generateQRCodeImage(text, size)` | `BufferedImage` via `QRCodeWriter` |
| `convertToSVG(image, w, h)` | `BufferedImage` → SVG string via `SVGGraphics2D` |
| `generateSVG(text, size)` | Direct text-to-SVG (combines the above two) |

Error correction: `ErrorCorrectionLevel.L` (low — 7% recovery, max data capacity).

**Dependencies:** `com.google.zxing:core:3.5.3`, `org.jfree:jfreesvg:3.4`

---

### miniStaticServer

**Package:** `io.github.blacknoize404.miniStaticServer`

Wraps `HttpServer` for directory-based static file serving with template injection.

#### StaticSite

Scans a directory and adds file routes automatically. Supports `{{variable}}` template substitution in HTML files.

```java
StaticSite site = new StaticSite(8080, Path.of("data"));
site.addTemplate("serverIp", "192.168.1.100");
site.start();
site.idle();
```

#### QrStaticSite

Extends the static file server concept with QR code injection. Replaces `{{placeholder}}` in HTML with inline QR code SVG elements.

```java
QrStaticSite site = new QrStaticSite(80, Path.of("data"));
site.addQrPlaceholder("downloadQR", "https://example.com/app.apk", 250);
site.start();
```

**Utility method:** `QrStaticSite.getLocalIp()` — discovers the local network IP via UDP socket to `8.8.8.8:12345`.

---

### miniApkReader

**Package:** `io.github.blacknoize404.miniApkReader`

Android APK metadata extraction.

#### ApkInfo Record

```java
public record ApkInfo(
    String packageName,       // com.example.app
    String versionName,       // 1.0.0
    long versionCode,         // 42
    String minSdkVersion,     // 26
    String targetSdkVersion,  // 33
    List<String> permissions, // [INTERNET, ...]
    List<String> features,    // [android.hardware.camera, ...]
    String label,             // "My App"
    String icon               // res/mipmap/ic_launcher.png
) {}
```

#### ApkReader

```java
ApkInfo info = ApkReader.read(Path.of("app.apk"));
String formatted = ApkReader.printInfo(info);
```

#### ApkInfoExtractor

CLI wrapper for `ApkReader.read()`:

```bash
mvn compile exec:java \
  -Dexec.mainClass="io.github.blacknoize404.miniApkReader.ApkInfoExtractor" \
  -Dexec.args="path/to/app.apk"
```

**Dependency:** `net.dongliu:apk-parser:2.6.10`

---

### miniJWS-demo

**Package:** `io.github.blacknoize404.miniJWS.demo`

Comprehensive demo server that exercises all middleware and route types. See [DemoServer.java](../../miniJWS-demo/src/main/java/io/github/blacknoize404/miniJWS/demo/DemoServer.java).

| Demo Feature | Description |
|-------------|-------------|
| Middleware | AccessLog, CORS (`*`), RateLimit (200/60s) |
| Static files | `./public` via `addStaticRoute("/*", "./public")` |
| Path params | `/hello/:name` |
| Query params | `/hello?name=X` |
| Body parsing | `POST /api/data` with JSON and form |
| Cookies | `/set-cookie`, `/get-cookies` |
| Redirect | `/old-path` → 301 → `/new-path` |
| Echo | `/echo` shows all request details |
| Keep-alive | Default (HTTP/1.1) |

---

[← Previous](classes-middleware.md) · [Next →](java-api/index.md)  
[🇪🇸 Español](classes-support.md) · [🇬🇧 English](classes-support.md)
