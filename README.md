# MiniJWS

![Java 25+](https://img.shields.io/badge/Java-25+-orange?logo=openjdk&logoColor=white)
![Maven 3.8+](https://img.shields.io/badge/Maven-3.8+-C71A36?logo=apachemaven&logoColor=white)
![CC BY-NC-SA 4.0](https://img.shields.io/badge/License-CC%20BY--NC--SA%204.0-lightgrey)

Un framework modular ligero de servidor HTTP en Java construido con Java 25.

Este proyecto surgió en mi segundo año de Ingeniería Informática como una iniciativa personal para aprender a fondo el protocolo HTTP, explorando cada aspecto del estándar a través de su implementación desde cero.

> **Nota:** Aunque el proyecto está listo para usarse directamente, su documentación también recoge qué clases de Java se emplean y por qué, las decisiones de diseño que se tomaron y los patrones utilizados con su justificación, con el objetivo de que sirva como recurso para que estudiantes vean conceptos universitarios aplicados en un proyecto real.

## Características

- **Servidor HTTP/1.1** con concurrencia mediante pool de hilos y conexiones persistentes
- **Pipeline de middleware** — logging, CORS, gzip, limitación de tasa, autenticación
- **Parámetros de ruta** — rutas estilo `/usuarios/:id`
- **Servicio de archivos estáticos** — basado en directorios con detección MIME
- **Parseo de cuerpo de solicitud** — JSON, form-urlencoded, texto plano
- **Soporte de cookies** — parsear cookies de solicitud, establecer cookies de respuesta
- **Ayudante de redirección** — 301/302 con `HttpResponse.redirect()`
- **Apagado gradual** — manejador SIGINT para detención limpia
- **Arquitectura modular** — módulos Maven independientes
- **Generación de códigos QR** (SVG/Imagen) mediante el módulo `miniQR`
- **Extracción de metadatos de APK Android** mediante el módulo `miniApkReader`
- **Servidor de sitios estáticos** con inyección de plantillas mediante el módulo `miniStaticServer`
- **Documentación completa** en `docs/`

## Estructura del Proyecto

```
MiniJWS/
├── miniJWS-core/                # Biblioteca central del servidor HTTP
│   └── src/main/java/io/github/blacknoize404/miniJWS/
│       ├── HttpServer.java        # Servidor principal (pool de hilos, rutas, middleware)
│       ├── primitives/            # HttpMethod, ContentType, Middleware, RequestRunner
│       ├── requests/              # HttpRequest, HttpDecoder
│       ├── responses/             # HttpResponse, HttpEncoder
│       ├── middleware/            # CorsMiddleware, AccessLog, Gzip, RateLimit
│       ├── handlers/              # StaticFileHandler
│       ├── headers/               # Header, Field, Parameter parsing
│       └── content/               # Mapeos de tipos MIME
├── miniJWS-demo/                 # Servidor de demostración completo
├── miniQR/                       # Generación de códigos QR (ZXing + JFreeSVG)
├── miniStaticServer/             # Servidor de archivos estáticos (+ inyección de QR)
├── miniApkReader/                # Parseador de metadatos de APK Android
├── public/                       # Recursos estáticos de demostración
├── docs/                         # Documentación completa
│   ├── index.md                 # Página principal de documentación
│   ├── architecture.md          # Arquitectura de módulos y flujo de solicitudes
│   ├── modules/                 # Documentación por módulo
│   ├── api/                     # Referencia de API
│   └── guides/                  # Primeros pasos, configuración, despliegue
└── README.md
```

## Requisitos

- Java 25+
- Maven 3.8+

## Compilación

```bash
# Compilar módulos en orden de dependencias
mvn clean install -f miniJWS-core/pom.xml
mvn clean install -f miniJWS-demo/pom.xml
mvn clean install -f miniQR/pom.xml
mvn clean install -f miniStaticServer/pom.xml
mvn clean install -f miniApkReader/pom.xml
```

## Inicio Rápido

```java
HttpServer server = new HttpServer(8080);

// Middleware
server.use(new AccessLogMiddleware());
server.use(new CorsMiddleware().allowOrigin("*"));

// Rutas
server.addRoute(HttpMethod.GET, "/", req ->
    new HttpResponse.Builder()
        .setStatusCode(200)
        .setContentType(ContentType.HTML)
        .setBody("<h1>¡Hola MiniJWS!</h1>")
        .build()
);

server.addRoute(HttpMethod.GET, "/hello/:name", req -> {
    String name = req.getParameters().get("name");
    return new HttpResponse.Builder()
        .setContentType(ContentType.TEXT)
        .setBody("Hola, " + name + "!")
        .build();
});

server.run();
```

## Ejecutar la Demo

```bash
# Iniciar el servidor de demostración en el puerto 8080
mvn compile exec:java -f miniJWS-demo/pom.xml
```

Luego abre http://localhost:8080 en tu navegador.

## Módulos

| Módulo | Descripción |
|--------|-------------|
| **[miniJWS-core](miniJWS-core/README.md)** | Servidor HTTP/1.1 central con middleware, rutas, parámetros, archivos estáticos, cookies, CORS |
| **[miniJWS-demo](miniJWS-demo/README.md)** | Servidor de demostración completo mostrando todas las funcionalidades |
| **[miniQR](miniQR/README.md)** | Generación de códigos QR usando ZXing con salida SVG mediante JFreeSVG |
| **[miniStaticServer](miniStaticServer/README.md)** | Servidor de archivos estáticos con placeholders de plantilla e inyección de QR |
| **[miniApkReader](miniApkReader/README.md)** | Extracción de metadatos de APK Android (paquete, versión, permisos, características) |

## Documentación

La documentación completa está disponible en el directorio [`docs/`](docs/index.md), incluyendo:
- [Visión general de la arquitectura](docs/architecture.md)
- [Detalles de módulos](docs/modules/)
- [Referencia de API](docs/api/)
- [Guía de primeros pasos](docs/guides/getting-started.md)
- [Configuración](docs/guides/configuration.md)
- [Despliegue](docs/guides/deployment.md)
- [Base de Conocimiento](docs/knowledge/index.md) — patrones usados, decisiones de diseño, clases de Java empleadas y por qué

## Licencia

CC-BY-NC-SA 4.0
