# Módulo: miniJWS-demo

Servidor de demostración completo mostrando todas las funcionalidades de miniJWS-core.

## Dependencias

| Dependencia | Alcance |
|-------------|---------|
| `io.github.blacknoize404:miniJWS-core:1.0-SNAPSHOT` | compile |

## Cómo Ejecutar

```bash
mvn compile exec:java -f miniJWS-demo/pom.xml
```

Abre en http://localhost:8080 por defecto. Puerto personalizado:

```bash
mvn exec:java -f miniJWS-demo/pom.xml -Dexec.args="9090"
```

## Rutas de Demostración

| Ruta | Descripción |
|------|-------------|
| `/` | 302 redirect → `/index.html` |
| `/index.html` | Página HTML estática desde `./public` |
| `/hello` | `¡Hola, Mundo!` |
| `/hello?name=X` | `¡Hola, X!` |
| `/hello/:name` | Parámetro de ruta — `¡Hola, {name}!` |
| `/api/info` | JSON con información del servidor y lista de funcionalidades |
| `POST /api/data` | Parsea cuerpo JSON o form |
| `/set-cookie` | Establece una cookie con parámetros |
| `/get-cookies` | Lee cookies como JSON |
| `/old-path` | 301 → `/new-path` |
| `/echo` | Devuelve detalles de la solicitud |
| `/*` | Archivos estáticos desde `./public` |

## Middleware Utilizado

| Middleware | Configuración |
|-----------|---------------|
| `AccessLogMiddleware` | Logea a stdout |
| `CorsMiddleware` | `allowOrigin("*")` |
| `RateLimitMiddleware` | `200 req / 60 seg` por IP |

## Funcionalidades Demostradas

- Pipeline de middleware (logging, CORS, limitación de tasa)
- Coincidencia exacta de rutas
- Parámetros de ruta (`:name`)
- Servicio de archivos estáticos con índice de directorio
- Parámetros de query
- Parseo de cuerpo de solicitud (JSON + form)
- Lectura y establecimiento de cookies
- Redirección 301/302
- Conexiones keep-alive
- Compresión Gzip (mediante `GzipMiddleware`)
- Apagado gradual (Ctrl+C)

---

[← Anterior](miniJWS-core.md) · [Siguiente →](miniQR.md)  
[🇪🇸 Español](miniJWS-demo.md) · [🇬🇧 English](miniJWS-demo.en.md)
