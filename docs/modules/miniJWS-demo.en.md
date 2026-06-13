# Module: miniJWS-demo

Complete demo server showcasing all features of miniJWS-core.

## Dependencies

| Dependency | Scope |
|------------|-------|
| `io.github.blacknoize404:miniJWS-core:1.0-SNAPSHOT` | compile |

## How to Run

```bash
mvn compile exec:java -f miniJWS-demo/pom.xml
```

Opens on http://localhost:8080 by default. Custom port:

```bash
mvn exec:java -f miniJWS-demo/pom.xml -Dexec.args="9090"
```

## Demo Routes

| Route | Description |
|-------|-------------|
| `/` | 302 redirect → `/index.html` |
| `/index.html` | Static HTML page from `./public` |
| `/hello` | `Hello, World!` |
| `/hello?name=X` | `Hello, X!` |
| `/hello/:name` | Path parameter — `Hello, {name}!` |
| `/api/info` | JSON with server info and feature list |
| `POST /api/data` | Parses JSON or form body |
| `/set-cookie` | Sets a cookie with params |
| `/get-cookies` | Reads cookies as JSON |
| `/old-path` | 301 → `/new-path` |
| `/echo` | Echoes request details |
| `/*` | Static files from `./public` |

## Middleware Used

| Middleware | Configuration |
|-----------|---------------|
| `AccessLogMiddleware` | Logs to stdout |
| `CorsMiddleware` | `allowOrigin("*")` |
| `RateLimitMiddleware` | `200 req / 60 sec` per IP |

## Features Demonstrated

- Middleware pipeline (logging, CORS, rate limit)
- Exact route matching
- Path parameters (`:name`)
- Static file serving with directory index
- Query parameters
- Request body parsing (JSON + form)
- Cookie reading and setting
- 301/302 redirect
- Keep-alive connections
- Gzip compression (via `GzipMiddleware`)
- Graceful shutdown (Ctrl+C)

---

[← Previous](miniJWS-core.en.md) · [Next →](miniQR.en.md)  
[🇪🇸 Español](miniJWS-demo.md) · [🇬🇧 English](miniJWS-demo.en.md)
