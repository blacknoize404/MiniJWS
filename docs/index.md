# Documentación de MiniJWS

MiniJWS es un framework modular ligero de servidor HTTP en Java construido con Java 25.
Proporciona una base flexible para construir servidores y servicios web con una arquitectura
limpia y modular.

## Estructura del Proyecto

```
MiniJWS/
├── miniJWS-core/              # Biblioteca central del servidor HTTP
├── miniJWS-demo/              # Servidor de demostración completo
├── miniQR/                    # Módulo de generación de códigos QR
├── miniStaticServer/          # Módulo de servidor de archivos estáticos
├── miniApkReader/             # Módulo lector de APK Android
├── public/                    # Recursos estáticos de demostración
├── docs/                      # Documentación
└── README.md
```

## Resumen de Módulos

| Módulo | Descripción |
|--------|-------------|
| **miniJWS-core** | Servidor HTTP/1.1 central con routing, middleware, parámetros de ruta, archivos estáticos, cookies, CORS, gzip, limitación de tasa, keep-alive |
| **miniJWS-demo** | Servidor de demostración completo mostrando todas las funcionalidades |
| **miniQR** | Generación de códigos QR usando ZXing con soporte de salida SVG |
| **miniStaticServer** | Servidor de archivos estáticos con soporte de plantillas e inyección de QR |
| **miniApkReader** | Extracción y parseo de metadatos de APK Android |

## Funcionalidades Destacadas

- **HTTP/1.1** con keep-alive, transferencia chunked, Content-Length
- **Pipeline de middleware** — logging, CORS, gzip, limitación de tasa, middleware personalizado
- **Routing** — exacto, parámetros de ruta (`:id`), comodines (`*`, `**`)
- **Servicio de archivos estáticos** — basado en directorios con detección MIME
- **Parseo de cuerpo de solicitud** — JSON, form-urlencoded, texto plano
- **Soporte de cookies** — parsear/establecer con `HttpOnly`, `Max-Age`, `Path`
- **Ayudante de redirección** — 301/302 mediante `HttpResponse.redirect()`
- **Apagado gradual** — hook SIGINT
- **Generación de QR** (miniQR), **metadatos de APK** (miniApkReader), **sitios estáticos** (miniStaticServer)

## Inicio Rápido

```java
HttpServer server = new HttpServer(8080);

server.use(new AccessLogMiddleware());
server.use(new CorsMiddleware().allowOrigin("*"));

server.addRoute(HttpMethod.GET, "/", req ->
    new HttpResponse.Builder()
        .setContentType(ContentType.HTML)
        .setStatusCode(200)
        .setBody("<h1>¡Hola!</h1>")
        .build()
);

server.addRoute(HttpMethod.GET, "/hello/:name", req -> {
    String name = req.getParameters().get("name");
    return new HttpResponse.Builder()
        .setContentType(ContentType.TEXT)
        .setBody("¡Hola, " + name + "!")
        .build();
});

server.run();
```

## Guía de Lectura para Estudiantes

Si eres estudiante y quieres aprender cómo se aplican los conceptos universitarios en un proyecto real, esta guía te orienta:

1. **Empieza por el protocolo HTTP** — lee [El Protocolo HTTP — Fundamentos](knowledge/http-protocol.md) para entender la base sobre la que está construido todo.
2. **Explora el código fuente** — los archivos más importantes están en `miniJWS-core/src/main/java/`. Comienza por `HttpServer.java` (el bucle principal), sigue por `HttpDecoder.java` (cómo se parsean las solicitudes) y `HttpEncoder.java` (cómo se escriben las respuestas).
3. **Estudia los patrones** — la [Base de Conocimiento](knowledge/index.md) documenta con ejemplos reales del proyecto los patrones Builder, Chain of Responsibility, Strategy e Inmutabilidad.
4. **Revisa las decisiones de diseño** — cada decisión importante (por qué no se usó NIO, por qué mensajes inmutables, por qué `CopyOnWriteArrayList`) está explicada en [Decisiones de Diseño](knowledge/decisions.md).
5. **Consulta las clases de Java usadas** — la [Referencia de API Java](knowledge/java-api/index.md) explica cada clase del JDK utilizada en el proyecto y por qué se eligió.
6. **Ponlo en práctica** — sigue la [Guía de Primeros Pasos](guides/getting-started.md) para crear tu propio servidor y experimenta modificando el código.

## Secciones de Documentación

- [Visión General de Arquitectura](architecture.md) — flujo de solicitudes, dependencias entre módulos, routing con comodines, seguridad de hilos
- [Módulos](modules/) — detalles por módulo
- [Referencia de API](api/) — HttpServer, HttpRequest, HttpResponse, Routing
- [Primeros Pasos](guides/getting-started.md) — compilar, ejecutar, crear tu propio servidor
- [Configuración](guides/configuration.md) — opciones de middleware, keep-alive, pool de hilos
- [Despliegue](guides/deployment.md) — fat JAR, systemd, notas de seguridad
- [Base de Conocimiento](knowledge/index.md) — análisis profundo de patrones, decisiones, clases y arquitectura
- [Referencia de API Java](knowledge/java-api/index.md) — documentación completa del lenguaje Java y APIs usadas en el proyecto
