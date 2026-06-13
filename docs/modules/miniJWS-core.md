# Módulo: miniJWS-core

La biblioteca central del servidor HTTP.

## Dependencias

- `org.jetbrains:annotations:24.1.0` (alcance compile)

## HttpServer

La clase principal del servidor. Gestiona enrutamiento, middleware, keep-alive y apagado gradual.

### Constructor

| Constructor | Descripción |
|-------------|-------------|
| `HttpServer(int port)` | Usa `2 * núcleos de CPU` hilos |
| `HttpServer(int port, int parallelism)` | Tamaño de pool personalizado |

### Métodos

| Método | Descripción |
|--------|-------------|
| `addRoute(HttpMethod, String, RequestRunner)` | Registrar un manejador de ruta |
| `removeRoute(HttpMethod, String)` | Desregistrar una ruta |
| `use(Middleware)` | Registrar middleware en el pipeline |
| `run()` | Iniciar servidor (bloqueante, con hook de apagado) |
| `run(boolean addShutdownHook)` | Iniciar con hook de apagado opcional |
| `stop()` | Detener gradualmente (cierra socket, drena pool) |
| `idle()` | Poner el hilo principal en espera (para modo daemon) |

### Enrutamiento

```java
// Coincidencia exacta
server.addRoute(HttpMethod.GET, "/api/users", request -> {
    return new HttpResponse.Builder()
            .setStatusCode(200)
            .setContentType(ContentType.JSON)
            .setBody("{\"users\":[]}")
            .build();
});

// Parámetros de ruta
server.addRoute(HttpMethod.GET, "/users/:id", request -> {
    String id = request.getParameters().get("id");
    // ...
});
```

### Middleware

```java
server.use(new AccessLogMiddleware());          // logging de solicitudes
server.use(new CorsMiddleware().allowOrigin("*")); // cabeceras CORS
server.use(new RateLimitMiddleware(100, 60));   // 100 req/min por IP
```

## HttpRequest

Objeto de solicitud inmutable con:

```java
request.getHttpMethod();          // GET, POST, etc.
request.getUri();                 // URI parseado
request.getHeaders();             // Map<String, List<String>>
request.getParameters();          // parámetros de query + ruta
request.getCookies();             // cabecera Cookie parseada
request.getBody();                // Optional<byte[]>
request.getHeader("Content-Type");// Optional<String>

// Parseo de cuerpo
request.bodyAsString();           // Optional<String> (UTF-8)
request.bodyAsForm();            // Optional<Map<String, String>>
request.bodyAsJson();            // Optional<Map<String, String>> (plano)
```

## HttpResponse

Objeto de respuesta inmutable con:

```java
// Builder
new HttpResponse.Builder()
    .setStatusCode(200)
    .setContentType(ContentType.HTML)
    .addHeader("Cache-Control", "no-cache")
    .setBody("<h1>OK</h1>")
    .build();

// Ayudante de redirección
HttpResponse.redirect("/login");          // 302
HttpResponse.redirect("/new-url", 301);   // 301

// Cookies
new HttpResponse.Builder()
    .setCookie("session", "abc123")
    .setCookie("token", "xyz", 3600, "/", true)
    .build();
```

## HttpDecoder

Parsea bytes crudos de un `InputStream` en un `Optional<HttpRequest>`.

```java
HttpDecoder.decode(inputStream);   // InputStream → Optional<HttpRequest>
HttpDecoder.decode(bufferedStream);// BufferedInputStream → Optional<HttpRequest>
```

Soporta:
- Parseo de línea de solicitud (método, URI, protocolo)
- Parseo de cabeceras multi-valor
- Extracción de parámetros de query
- Lectura de cuerpo con Content-Length
- Transferencia codificada chunked

## HttpEncoder

Serializa un `HttpResponse` a un `OutputStream`. Detecta automáticamente el tipo de contenido para elegir entre codificación de texto y binaria.

## Enum ContentType

Mapea extensiones de archivo a tipos MIME:

| Constante | Extensión(es) | MIME |
|-----------|---------------|------|
| `HTML` | html | `text/html;charset=utf-8` |
| `JSON` | json | `application/json;charset=utf-8` |
| `JPEG` | jpg, jpeg | `image/jpeg` |
| `PNG` | png | `image/png` |
| `TEXT` | txt | `text/plain;charset=utf-8` |
| ... | ... | ... |

## Manejadores Integrados

### StaticFileHandler

```java
server.addRoute(HttpMethod.GET, "/*", new StaticFileHandler("./public"));
```

Sirve archivos desde un directorio con detección MIME, soporte de archivo índice y prevención de path traversal.

## Middleware Integrado

| Middleware | Descripción |
|------------|-------------|
| `AccessLogMiddleware` | Logging en formato Apache Common Log |
| `CorsMiddleware` | Cabeceras CORS con origen, métodos y cabeceras configurables |
| `GzipMiddleware` | Compresión de respuestas con nivel configurable |
| `RateLimitMiddleware` | Limitación de tasa por IP con máximo/ventana configurables |

## Keep-Alive

Las conexiones persistentes HTTP/1.1 están habilitadas por defecto. El servidor reutiliza el mismo socket para hasta 100 solicitudes o 10 segundos de inactividad.

## Seguridad de Hilos

- Las rutas usan `ConcurrentHashMap` — seguro modificar en tiempo de ejecución
- Cada conexión se ejecuta en un hilo del pool fijo
- El middleware se ejecuta en el hilo del manejador de conexión

---

[← Inicio](../index.md) · [Siguiente →](miniJWS-demo.md)  
[🇪🇸 Español](miniJWS-core.md) · [🇬🇧 English](miniJWS-core.en.md)
