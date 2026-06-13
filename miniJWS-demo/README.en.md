# miniJWS-demo

![Java 25+](https://img.shields.io/badge/Java-25+-orange?logo=openjdk&logoColor=white)
![Maven 3.8+](https://img.shields.io/badge/Maven-3.8+-C71A36?logo=apachemaven&logoColor=white)

Full demo server showcasing all features of miniJWS-core.

## Run

```bash
mvn compile exec:java
```

Opens on http://localhost:8080.

## Routes

| Route | Description |
|-------|-------------|
| `/` | 302 → `/index.html` |
| `/index.html` | Static page from `./public` |
| `/hello` | `Hello, World!` |
| `/hello?name=X` | `Hello, X!` |
| `/hello/:name` | Path param — `Hello, {name}!` |
| `/api/info` | JSON with server info |
| `POST /api/data` | Parse JSON or form body |
| `/set-cookie` | Set a cookie |
| `/get-cookies` | Read cookies as JSON |
| `/old-path` | 301 → `/new-path` |
| `/echo` | Echo request details |
| `/*` | Static files from `./public` |

## Middleware

| Middleware | Config |
|-----------|--------|
| `AccessLogMiddleware` | Stdout |
| `CorsMiddleware` | `allowOrigin("*")` |
| `RateLimitMiddleware` | 200 req / 60 sec per IP |

## Build

```bash
mvn clean install
```
