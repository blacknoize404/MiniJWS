# Routing API

## RequestRunner

`io.github.blacknoize404.miniJWS.primitives.RequestRunner`

A functional interface with a single method:

```java
HttpResponse run(HttpRequest request);
```

## Adding Routes

```java
HttpServer server = new HttpServer(8080);

// Lambda style
server.addRoute(HttpMethod.GET, "/api/status", req ->
    new HttpResponse.Builder()
            .setStatusCode(200)
            .setContentType(ContentType.JSON)
            .setBody("{\"status\":\"ok\"}")
            .build()
);
```

## Route Matching

### Exact Match

`GET:/api/users` matches only `/api/users`.

### Path Parameters

Routes like `/users/:id` match dynamic paths. The parameter value is extracted and added to `request.getParameters()`. Wildcard routes (`*` single segment, `**` all remaining) are supported and matched last:

```java
server.addRoute(HttpMethod.GET, "/*", new StaticFileHandler("./public"));   // single segment
server.addRoute(HttpMethod.GET, "/assets/**", req -> ...);                 // all segments
```

```java
server.addRoute(HttpMethod.GET, "/users/:id", req -> {
    String id = req.getParameters().get("id");
    return new HttpResponse.Builder()
            .setContentType(ContentType.TEXT)
            .setBody("User ID: " + id)
            .build();
});
```

Multiple parameters are supported:
```java
// Route: /posts/:year/:month
// Request: /posts/2026/06
req.getParameters().get("year");   // "2026"
req.getParameters().get("month");  // "06"
```

### Wildcard Matching Order

Routes are matched in this priority:

1. **Exact match** — `GET:/api/users` matches `/api/users`
2. **Path parameters** — `GET:/users/:id` matches `/users/42`
3. **Single wildcard** — `GET:/*` matches `/any-single-segment`
4. **Glob wildcard** — `GET:/assets/**` matches `/assets/a/b/c`

### Merge Order

Path parameters and query parameters are merged into the same `Map`:
- Query: `/users/42?debug=true` → params = `{id: "42", debug: "true"}`
- Path params take precedence if keys collide

## Static File Handler

```java
// Serve files from ./public directory
server.addRoute(HttpMethod.GET, "/*", new StaticFileHandler("./public"));

// With custom index files
server.addRoute(HttpMethod.GET, "/*", new StaticFileHandler("./public", "index.html", "index.htm"));
```

## Middleware

```java
// Log all requests
server.use(new AccessLogMiddleware());

// Enable CORS for all origins
server.use(new CorsMiddleware().allowOrigin("*"));

// Rate limit: 100 requests per 60 seconds per IP
server.use(new RateLimitMiddleware(100, 60));

// Compress responses with gzip
server.use(new GzipMiddleware(6));
```

## Query Parameters

```java
server.addRoute(HttpMethod.GET, "/search", req -> {
    String query = req.getParameters().getOrDefault("q", "");
    int page = Integer.parseInt(req.getParameters().getOrDefault("page", "1"));
});
```

## Custom Middleware

```java
server.use((request, chain) -> {
    System.out.println("Before: " + request.getUri());
    long start = System.nanoTime();
    HttpResponse response = chain.next(request);
    long ms = (System.nanoTime() - start) / 1_000_000;
    System.out.println("After: " + response.getStatusCode() + " (" + ms + "ms)");
    return response;
});
```

## Redirect

```java
HttpResponse.redirect("/login");          // 302 Found
HttpResponse.redirect("/new-url", 301);   // 301 Moved Permanently

---

[← Previous](http-response.en.md) · [Home](../index.en.md)  
[🇪🇸 Español](routing.md) · [🇬🇧 English](routing.en.md)
```
