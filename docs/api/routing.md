# API de Routing

## RequestRunner

`io.github.blacknoize404.miniJWS.primitives.RequestRunner`

Una interfaz funcional con un único método:

```java
HttpResponse run(HttpRequest request);
```

## Añadir Rutas

```java
HttpServer server = new HttpServer(8080);

// Estilo lambda
server.addRoute(HttpMethod.GET, "/api/status", req ->
    new HttpResponse.Builder()
            .setStatusCode(200)
            .setContentType(ContentType.JSON)
            .setBody("{\"status\":\"ok\"}")
            .build()
);
```

## Coincidencia de Rutas

### Coincidencia Exacta

`GET:/api/users` coincide solo con `/api/users`.

### Parámetros de Ruta

Rutas como `/users/:id` coinciden con rutas dinámicas. El valor del parámetro se extrae y se añade a `request.getParameters()`. Las rutas con comodín (`*` un segmento, `**` todos los segmentos) también están soportadas y se emparejan al final:

```java
server.addRoute(HttpMethod.GET, "/*", new StaticFileHandler("./public"));   // un segmento
server.addRoute(HttpMethod.GET, "/assets/**", req -> ...);                 // todos los segmentos
```

```java
server.addRoute(HttpMethod.GET, "/users/:id", req -> {
    String id = req.getParameters().get("id");
    return new HttpResponse.Builder()
            .setContentType(ContentType.TEXT)
            .setBody("ID de usuario: " + id)
            .build();
});
```

Se soportan múltiples parámetros:
```java
// Ruta: /posts/:year/:month
// Solicitud: /posts/2026/06
req.getParameters().get("year");   // "2026"
req.getParameters().get("month");  // "06"
```

### Orden de Coincidencia de Comodines

Las rutas se emparejan en esta prioridad:

1. **Coincidencia exacta** — `GET:/api/users` coincide con `/api/users`
2. **Parámetros de ruta** — `GET:/users/:id` coincide con `/users/42`
3. **Comodín simple** — `GET:/*` coincide con `/cualquier-segmento`
4. **Comodín global** — `GET:/assets/**` coincide con `/assets/a/b/c`

### Orden de Fusión

Los parámetros de ruta y de query se fusionan en el mismo `Map`:
- Query: `/users/42?debug=true` → params = `{id: "42", debug: "true"}`
- Los parámetros de ruta tienen prioridad si hay conflicto de claves

## StaticFileHandler

```java
// Servir archivos desde el directorio ./public
server.addRoute(HttpMethod.GET, "/*", new StaticFileHandler("./public"));

// Con archivos de índice personalizados
server.addRoute(HttpMethod.GET, "/*", new StaticFileHandler("./public", "index.html", "index.htm"));
```

## Middleware

```java
// Registrar todas las solicitudes
server.use(new AccessLogMiddleware());

// Habilitar CORS para todos los orígenes
server.use(new CorsMiddleware().allowOrigin("*"));

// Limitar tasa: 100 solicitudes por 60 segundos por IP
server.use(new RateLimitMiddleware(100, 60));

// Comprimir respuestas con gzip
server.use(new GzipMiddleware(6));
```

## Parámetros de Query

```java
server.addRoute(HttpMethod.GET, "/search", req -> {
    String query = req.getParameters().getOrDefault("q", "");
    int page = Integer.parseInt(req.getParameters().getOrDefault("page", "1"));
});
```

## Middleware Personalizado

```java
server.use((request, chain) -> {
    System.out.println("Antes: " + request.getUri());
    long start = System.nanoTime();
    HttpResponse response = chain.next(request);
    long ms = (System.nanoTime() - start) / 1_000_000;
    System.out.println("Después: " + response.getStatusCode() + " (" + ms + "ms)");
    return response;
});
```

## Redirección

```java
HttpResponse.redirect("/login");          // 302 Found
HttpResponse.redirect("/new-url", 301);   // 301 Moved Permanently

---

[← Anterior](http-response.md) · [Inicio](../index.md)  
[🇪🇸 Español](routing.md) · [🇬🇧 English](routing.en.md)
```
