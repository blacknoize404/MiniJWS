# miniJWS-core — Unit Tests

## Test Classes (16 files, ~130+ tests)

| Test Class | Source Under Test | Tests | What it evaluates |
|------------|-------------------|-------|-------------------|
| `HttpMethodTest` | `HttpMethod` (enum) | 2 | All 9 standard HTTP methods exist; `valueOf` is case-sensitive |
| `HttpStatusCodeTest` | `HttpStatusCode` | 3 | Known codes return correct messages; unknown codes return "UNKNOWN"; map is non-empty |
| `ContentTypeTest` | `ContentType` (enum) | 7 | `fromExtension` for 19 known types + unknown/blank/case-insensitive; `fromMime`; `mime()` accessor |
| `ContentTypesTest` | `ContentTypes` | 5 | `forExtension` for 12 known types + unknown/null/blank returns default; case-insensitivity |
| `HttpRequestTest` | `HttpRequest` | 20 | Builder creates valid requests; null checks; header joining; cookie parsing; UTF-8 body; form/JSON body parsers; edge cases (empty body, empty object, missing fields) |
| `HttpResponseTest` | `HttpResponse` | 18 | Default status 200; custom status; content-type via enum/string; Server/Date headers; body via string/bytes/null; custom headers; Set-Cookie (simple + with options); redirect (302 default + 301); builder independence |
| `HttpEncoderTest` | `HttpEncoder` | 7 | Status line format (200, 404); header serialization; body + Content-Length; no body omits Content-Length; CRLF separation; multiple header values |
| `HttpDecoderTest` | `HttpDecoder` | 14 | Simple GET; headers; query params; POST body; empty/null input; invalid method/request line; duplicate Content-Length; negative Content-Length; chunked TE; chunked with extensions; URI without query; folded headers; oversized header rejection |
| `HttpServerTest` | `HttpServer` | 9 | `matchPath` parameter extraction (6 scenarios: `:id`, `:id/:postId`, `*`, `**`, root, mismatch); `SERVER_NAME`; constructor variants; `addRoute`/`removeRoute`; `addStaticRoute` |
| `StaticFileHandlerTest` | `StaticFileHandler` | 9 | Existing file served (200, correct content); non-existent file (404); path traversal (`..`) blocked (400/403); directory index served; directory without index (404); content-type detection from extension; custom index files |
| `HeaderTest` | `Header` | 5 | Simple header; multiple values; parameters; missing colon throws; `toString` |
| `FieldTest` | `Field` | 6 | Type + subtype; type only; parameters; empty subtype; multiple parameters; `toString` |
| `ParameterTest` | `Parameter` | 6 | Key=value; whitespace trimming; missing `=` throws; empty value; null/empty throws; `toString` |
| `CorsMiddlewareTest` | `CorsMiddleware` | 11 | No origin passthrough; origin adds CORS header; preflight returns 204; preflight includes Methods/Headers; credentials+wildcard throws; credentials with specific origin sets header; maxAge; custom methods/headers; preserves original status/body |
| `GzipMiddlewareTest` | `GzipMiddleware` | 10 | No `Accept-Encoding` passthrough; gzip compresses; compressed body is smaller; valid gzip (decompressible); small body skips; no body skips; already encoded skips; non-gzip encoding skips; default/clamped compression levels |
| `RateLimitMiddlewareTest` | `RateLimitMiddleware` | 7 | First request allows; within limit allows; exceeds limit returns 429; Retry-After header; different IPs independent; X-Forwarded-For support; fallback to localhost |

## Running the Tests

```bash
cd miniJWS-core
mvn test
```
