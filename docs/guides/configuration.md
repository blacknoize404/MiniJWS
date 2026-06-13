# Configuración

## Configuración del Servidor

El constructor de `HttpServer` acepta:

```java
// Solo puerto — pool de hilos = 2 * núcleos de CPU
new HttpServer(8080);

// Tamaño de pool personalizado
new HttpServer(8080, 50);
```

## Pool de Hilos

El pool de hilos es de tamaño fijo:
- Por defecto: `Runtime.getRuntime().availableProcessors() * 2`
- Cada conexión obtiene su propio hilo
- Los hilos se reutilizan mediante `Executors.newFixedThreadPool()`

## Keep-Alive

Las conexiones persistentes HTTP/1.1 están habilitadas por defecto:
- **Máx. solicitudes por conexión:** 100 (`HttpServer.MAX_KEEPALIVE_REQUESTS`)
- **Tiempo de inactividad:** 10 segundos (`HttpServer.KEEPALIVE_TIMEOUT_MS`)
- Respeta la cabecera `Connection: close`

## Apagado Gradual

Cuando se llama a `stop()`:
1. La bandera `running` se establece a false
2. `ServerSocket.close()` lanza una excepción en `accept()`, terminando el bucle
3. `shutdown()` drena el pool de hilos con 5 segundos de gracia
4. Un shutdown hook mediante `Runtime.getRuntime().addShutdownHook()` se registra automáticamente en `run()`

```java
server.run();                    // con hook SIGINT
server.run(false);               // sin hook (stop() manual)
```

## Pipeline de Middleware

El middleware se ejecuta en orden de registro:

```java
server.use(new AccessLogMiddleware());   // se ejecuta primero
server.use(new CorsMiddleware());        // se ejecuta segundo
server.use(new RateLimitMiddleware(100, 60)); // se ejecuta tercero
```

Cada middleware puede cortocircuitar la cadena devolviendo una respuesta directamente.

## Opciones de Middleware Integrado

### CorsMiddleware
| Método | Por defecto | Descripción |
|--------|-------------|-------------|
| `allowOrigin(String)` | `*` | Origen permitido |
| `allowMethods(String...)` | GET, POST, PUT, DELETE, OPTIONS, PATCH | Métodos permitidos |
| `allowHeaders(String...)` | Content-Type, Authorization | Cabeceras permitidas |
| `allowCredentials(boolean)` | false | Bandera de credenciales |
| `maxAge(int)` | -1 | Segundos de caché preflight |

### RateLimitMiddleware
| Parámetro | Descripción |
|-----------|-------------|
| `maxRequests` | Máx. solicitudes en la ventana |
| `windowSeconds` | Duración de la ventana en segundos |

### GzipMiddleware
| Parámetro | Descripción |
|-----------|-------------|
| `level` | Nivel de compresión 1-9 (por defecto 6) |

## Tipos de Contenido

Las extensiones de archivo se mapean a tipos MIME en `ContentTypes.EXTENSION_TO_MIME`:

| Extensión | Tipo MIME |
|-----------|-----------|
| `html` | `text/html;charset=utf-8` |
| `css` | `text/css;charset=utf-8` |
| `js` | `text/javascript;charset=utf-8` |
| `json` | `application/json;charset=utf-8` |
| `png` | `image/png` |
| `svg` | `image/svg+xml` |
| `mp4` | `video/mp4` |
| `apk` | `application/vnd.android.package-archive` |

Alias adicionales en `ContentType.EXT_MAP`:
- `jpg` → `JPEG`
- `jpeg` → `JPEG`
- `tiff` → `FILE` (binario genérico)

## StaticFileHandler

```java
new StaticFileHandler("./public");
new StaticFileHandler("./public", "index.html", "index.htm"); // archivos de índice personalizados
```

Características:
- Detección MIME por extensión de archivo
- Servicio de archivos de índice para directorios
- Prevención de path traversal (`../` bloqueado)
- 404 para archivos inexistentes

## Variables de Entorno

No hay dependencias de variables de entorno. Toda la configuración se realiza mediante constructores y llamadas a métodos.

---

[← Anterior](getting-started.md) · [Siguiente →](deployment.md)  
[🇪🇸 Español](configuration.md) · [🇬🇧 English](configuration.en.md)
