# miniJWS-core

Biblioteca central del servidor HTTP/1.1 para MiniJWS — ligera, modular, sin dependencias (excepto anotaciones JetBrains).

## Características

- **HTTP/1.1** con keep-alive, transferencia chunked, Content-Length
- **Pipeline de middleware** — logging, CORS, gzip, limitación de tasa, personalizado
- **Routing** — exacto, parámetros de ruta (`:id`), comodines (`*`, `**`)
- **Servicio de archivos estáticos** — basado en directorios con detección MIME
- **Parseo de cuerpo de solicitud** — JSON, form-urlencoded, texto plano
- **Soporte de cookies** — parsear/establecer con `HttpOnly`, `Max-Age`, `Path`
- **Ayudante de redirección** — 301/302 mediante `HttpResponse.redirect()`
- **Apagado gradual** — hook SIGINT con `CountDownLatch`

## Estructura del Paquete

```
io.github.blacknoize404.miniJWS/
├── HttpServer.java              # Servidor principal (pool de hilos, rutas, middleware)
├── primitives/
│   ├── ContentType.java         # Enum de tipos MIME
│   ├── HttpMethod.java          # Enum de métodos HTTP
│   ├── HttpStatusCode.java      # Enum de códigos de estado
│   ├── Middleware.java          # Interfaz de middleware
│   ├── MiddlewareChain.java     # Interfaz de cadena
│   └── RequestRunner.java       # Interfaz de manejador de ruta
├── requests/
│   ├── HttpDecoder.java         # Parseador de solicitudes (línea por línea)
│   └── HttpRequest.java         # Modelo de solicitud (Builder, parseo de cuerpo, cookies)
├── responses/
│   ├── HttpEncoder.java         # Escritor de respuestas
│   └── HttpResponse.java        # Modelo de respuesta (Builder, redirect, cookies)
├── middleware/
│   ├── AccessLogMiddleware.java # Logging asíncrono estilo Apache
│   ├── CorsMiddleware.java      # CORS con preflight
│   ├── GzipMiddleware.java      # Compresión Gzip
│   └── RateLimitMiddleware.java # Limitación de tasa por IP
├── handlers/
│   └── StaticFileHandler.java   # Servidor de archivos estáticos integrado
├── headers/
│   ├── Header.java              # Modelo de cabecera HTTP
│   ├── Field.java               # Parseador de campos de cabecera
│   └── Parameter.java           # Parseador de parámetros de cabecera
└── content/
    └── ContentTypes.java        # Mapeo extensión-a-MIME
```

## Inicio Rápido

```java
HttpServer server = new HttpServer(8080);

server.use(new AccessLogMiddleware());
server.use(new CorsMiddleware().allowOrigin("*"));

server.addRoute(HttpMethod.GET, "/", req ->
    new HttpResponse.Builder()
        .setStatusCode(200)
        .setContentType(ContentType.HTML)
        .setBody("<h1>¡Hola!</h1>")
        .build()
);

server.run();
```

## Compilación

```bash
mvn clean install
```
