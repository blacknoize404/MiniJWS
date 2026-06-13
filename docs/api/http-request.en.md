# HttpRequest API

`io.github.blacknoize404.miniJWS.requests.HttpRequest`

Immutable HTTP request model built via the Builder pattern.

## Properties

| Property | Type | Description |
|----------|------|-------------|
| `httpMethod` | `HttpMethod` | GET, POST, PUT, DELETE, etc. |
| `uri` | `URI` | Request URI (path only, no query) |
| `protocolVersion` | `String` | e.g. `HTTP/1.1` |
| `headers` | `Map<String, List<String>>` | Request headers (multi-value) |
| `parameters` | `Map<String, String>` | Query string + path parameters |
| `cookies` | `Map<String, String>` | Parsed from Cookie header |
| `body` | `Optional<byte[]>` | Request body |

## Accessor Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `getUri()` | `URI` | Request URI |
| `getHttpMethod()` | `HttpMethod` | HTTP method |
| `getHeaders()` | `Map<String, List<String>>` | All headers |
| `getParameters()` | `Map<String, String>` | Query + path params |
| `getCookies()` | `Map<String, String>` | Request cookies |
| `getProtocolVersion()` | `String` | Protocol version |
| `getBody()` | `Optional<byte[]>` | Raw body bytes |
| `getHeader(String)` | `Optional<String>` | Single header value |

## Body Parsing

| Method | Returns | Description |
|--------|---------|-------------|
| `bodyAsString()` | `Optional<String>` | Body as UTF-8 string |
| `bodyAsForm()` | `Optional<Map<String, String>>` | Parse `application/x-www-form-urlencoded` |
| `bodyAsJson()` | `Optional<Map<String, String>>` | Parse flat JSON object |

## Builder Methods

| Method | Description |
|--------|-------------|
| `setHttpMethod(HttpMethod)` | Set the HTTP method |
| `setUri(URI)` | Set the request URI |
| `setProtocolVersion(String)` | Set protocol version |
| `setHeaders(Map)` | Set all headers at once |
| `addHeader(String, List<String>)` | Add a single header |
| `setParameters(Map)` | Set query/path parameters |
| `addParameter(String, String)` | Add a single parameter |
| `setBody(byte[])` | Set the request body |
| `build()` | Build the HttpRequest |

## HttpDecoder

```java
HttpDecoder.decode(InputStream)              → Optional<HttpRequest>
HttpDecoder.decode(BufferedInputStream)       → Optional<HttpRequest>
```

Parses raw HTTP from an InputStream. Handles:
- Request line (method, URI, protocol)
- Multi-value headers
- Query parameter extraction
- Content-Length body
- Chunked transfer encoding
- Cookie header

---

[← Previous](http-server.en.md) · [Next →](http-response.en.md)  
[🇪🇸 Español](http-request.md) · [🇬🇧 English](http-request.en.md)
