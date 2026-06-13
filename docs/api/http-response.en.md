# HttpResponse API

`io.github.blacknoize404.miniJWS.responses.HttpResponse`

Immutable HTTP response model built via the Builder pattern.

## Properties

| Property | Type | Description |
|----------|------|-------------|
| `statusCode` | `int` | HTTP status code (200, 404, etc.) |
| `protocolVersion` | `String` | e.g. `HTTP/1.1` |
| `method` | `HttpMethod` | Originating request method |
| `headers` | `Map<String, List<String>>` | Response headers |
| `body` | `Optional<byte[]>` | Response body |

## Static Factory Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `redirect(String)` | `HttpResponse` | 302 redirect to location |
| `redirect(String, int)` | `HttpResponse` | Redirect with custom status (301, 302, etc.) |

## Builder Methods

| Method | Description |
|--------|-------------|
| `setStatusCode(int)` | Set HTTP status code |
| `setProtocolVersion(String)` | Set protocol version |
| `setMethod(HttpMethod)` | Set HTTP method |
| `setContentType(ContentType)` | Set Content-Type from enum |
| `setContentType(String)` | Set Content-Type as raw MIME |
| `addHeader(String, String)` | Add a single-valued header |
| `addHeader(String, List<String>)` | Add a multi-valued header |
| `setCookie(String, String)` | Set a simple cookie |
| `setCookie(String, String, int, String, boolean)` | Set cookie with Max-Age, Path, HttpOnly |
| `setBody(String)` | Set body from UTF-8 string |
| `setBody(byte[])` | Set body from raw bytes |
| `build()` | Build the HttpResponse |

## Example

```java
new HttpResponse.Builder()
    .setStatusCode(200)
    .setContentType(ContentType.JSON)
    .setCookie("session", "abc123", 3600, "/", true)
    .setBody("{\"status\":\"ok\"}")
    .build();

// Redirect
HttpResponse.redirect("/login");
HttpResponse.redirect("/new-url", 301);
```

## Default Headers

Every response automatically includes:
- `Server: MiniJWS`
- `Date: <current RFC 1123 timestamp>`

## HttpEncoder

`HttpEncoder.sendResponse(HttpResponse, OutputStream)`

Serializes a response to an output stream. Handles:
- Status line
- Headers (including Connection: keep-alive)
- Content-Length calculation
- Text vs binary encoding based on Content-Type

---

[← Previous](http-request.en.md) · [Next →](routing.en.md)  
[🇪🇸 Español](http-response.md) · [🇬🇧 English](http-response.en.md)
