# Clases de Middleware

Todos los middleware implementan la interfaz funcional `Middleware`:

```java
@FunctionalInterface
public interface Middleware {
    HttpResponse run(HttpRequest request, MiddlewareChain chain);
}
```

Se registran mediante `server.use(middleware)` y se ejecutan en orden de registro.

---

## AccessLogMiddleware

**Archivo:** `miniJWS-core/src/main/java/.../middleware/AccessLogMiddleware.java`

Logging en formato Apache Common Log con E/S asíncrona.

### Arquitectura

```
Hilo de Petición              Hilo Trabajador (daemon)
    │                                │
    ├── formatLogLine()              │
    ├── queue.offer(line) ──────────►├── queue.take() (bloquea)
    │                                ├── writer.println(line)
    │                                ├── writer.flush()
    │                                └── bucle
    └── devuelve respuesta
```

### Opciones del Constructor

| Constructor | Destino de Salida |
|-------------|-------------------|
| `AccessLogMiddleware()` | `System.out` (envuelto en `PrintWriter`) |
| `AccessLogMiddleware(String filePath)` | Archivo (append, UTF-8) |
| `AccessLogMiddleware(PrintWriter writer)` | Escritor personalizado |

### Formato del Log

```
127.0.0.1 - - [13/Jun/2026:14:30:00 +0000] "GET /hello HTTP/1.1" 200 13 (2ms)
```

Campos: IP remota (respeta `X-Forwarded-For`), marca temporal, línea de petición, código de estado, tamaño del cuerpo, ms transcurridos.

### Puntos Clave de Diseño

- `BlockingQueue<String>` (capacidad 16_384) desacopla el manejo de peticiones de la E/S de log
- `queue.offer()` nunca bloquea (devuelve false si está llena, la línea se descarta)
- El trabajador es un hilo daemon (no impide la salida de la JVM)
- El hook de apagado drena las entradas restantes con `flushRemaining()`

### Hook de Apagado

```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    worker.interrupt();
    worker.join(2_000);  // espera hasta 2s para vaciar
    flushRemaining(writer);
}));
```

---

## CorsMiddleware

**Archivo:** `miniJWS-core/src/main/java/.../middleware/CorsMiddleware.java`

Implementación de CORS (Cross-Origin Resource Sharing).

### Métodos de Configuración

| Método | Por Defecto | Descripción |
|--------|-------------|-------------|
| `allowOrigin(String)` | `*` | Origen permitido |
| `allowMethods(String...)` | GET, POST, PUT, DELETE, OPTIONS, PATCH | Métodos permitidos |
| `allowHeaders(String...)` | Content-Type, Authorization | Cabeceras permitidas |
| `allowCredentials(boolean)` | `false` | Si enviar credenciales |
| `maxAge(int)` | `-1` | Duración de caché preflight |

### Flujo de Petición

1. **Sin cabecera Origin** → saltar (pasar al siguiente middleware)
2. **Petición OPTIONS** → devolver 204 con cabeceras CORS (preflight)
3. **Petición normal** → llamar `chain.next()`, añadir cabeceras CORS a la respuesta

### Manejo Especial: `*` + Credenciales

La especificación CORS prohíbe `Access-Control-Allow-Origin: *` cuando `Access-Control-Allow-Credentials: true`. Cuando se llama a `allowCredentials(true)` con `allowOrigin("*")`:
- `allowCredentials(true)` **lanza** `IllegalStateException` inmediatamente (fail-fast)
- Solución: usar `allowOrigin("https://dominio.especifico")` con credenciales, o llamar a `allowCredentials(true)` antes de `allowOrigin("*")` (el orden importa porque `allowCredentials()` valida en el momento de la llamada — puedes establecer credenciales primero, luego origen a `*` para saltar la validación)

El orden importa: `allowCredentials(true)` lanza si hay `*`. Pero `allowCredentials(false)` → luego `allowOrigin("*")` funciona. Y luego `allowCredentials(true)` lanzaría. Así que la secuencia segura es `allowCredentials(true)` primero, luego `allowOrigin("dominio-especifico")`.

### Copia de Cabeceras

Al añadir cabeceras CORS a una respuesta existente, el middleware reconstruye la respuesta (copia todas las cabeceras, estado, método, cuerpo) para preservar la inmutabilidad.

---

## GzipMiddleware

**Archivo:** `miniJWS-core/src/main/java/.../middleware/GzipMiddleware.java`

Compresión de respuestas para clientes que aceptan codificación gzip.

### Algoritmo

1. Verificar cabecera `Accept-Encoding` — si no hay `gzip`, pasar
2. Llamar a `chain.next(request)` para obtener la respuesta
3. Omitir si: cuerpo vacío, ya tiene `Content-Encoding`, o cuerpo < 256 bytes
4. Comprimir con `GZIPOutputStream`
5. Omitir si comprimido ≥ tamaño original
6. Construir nueva respuesta con `Content-Encoding: gzip`

### Ajuste de Compresión

```java
new GzipMiddleware();    // nivel por defecto 6
new GzipMiddleware(9);   // compresión máxima (más lento)
new GzipMiddleware(1);   // más rápido, menos compresión
```

### Establecimiento del Nivel

```java
private final int level;
public GzipMiddleware(int level) {
    this.level = Math.max(1, Math.min(9, level));
}
```

El nivel se aplica mediante una subclase anónima:

```java
var gz = new GZIPOutputStream(bos) {{
    def.setLevel(level);
}};
```

### Protección contra Doble Compresión

```java
boolean alreadyEncoded = response.getHeaders().keySet().stream()
    .anyMatch(k -> k.equalsIgnoreCase("Content-Encoding"));
if (alreadyEncoded) return response;
```

Evita comprimir una respuesta ya comprimida.

### try-finally en GZIPOutputStream

```java
try {
    gz.write(data);
} finally {
    gz.close();  // asegura que el trailer se escribe incluso si write falla
}
```

La llamada `close()` escribe el trailer gzip (CRC32 + tamaño). Sin ella, el flujo queda incompleto.

---

## RateLimitMiddleware

**Archivo:** `miniJWS-core/src/main/java/.../middleware/RateLimitMiddleware.java`

Limitación de tasa por IP usando ventana deslizante.

### Estructura de Datos

```java
ConcurrentHashMap<String, Queue<Instant>> requests = new ConcurrentHashMap<>();
AtomicInteger totalEntries = new AtomicInteger(0);
```

Cada IP tiene una `ConcurrentLinkedQueue<Instant>` de marcas temporales de peticiones.

### Algoritmo

1. Extraer IP del cliente (respeta `X-Forwarded-For`, `X-Real-IP`)
2. Obtener o crear la cola de marcas temporales para esa IP
3. `synchronized(queue)`:
   - Eliminar marcas temporales más antiguas que la ventana
   - Si el tamaño de la cola ≥ `maxRequests` → devolver 429
   - Si no, añadir la marca temporal actual
4. Si `totalEntries > CLEANUP_THRESHOLD` (10_000), activar `cleanupStaleEntries()`

### Limpieza (Prevención de Fugas de Memoria)

```java
private void cleanupStaleEntries(Instant cutoff) {
    for (var it = requests.entrySet().iterator(); it.hasNext();) {
        var entry = it.next();
        Queue<Instant> q = entry.getValue();
        synchronized (q) {
            while (!q.isEmpty() && q.peek().isBefore(cutoff)) {
                q.poll();
            }
            if (q.isEmpty()) {
                it.remove();         // eliminar IP del mapa
                removed++;
            }
        }
    }
    totalEntries.addAndGet(-removed);
}
```

Sin esta limpieza, las IPs inactivas se acumulan en el `ConcurrentHashMap` para siempre, causando una fuga de memoria. La limpieza basada en umbral (`> 10_000` entradas) asegura un uso de memoria acotado.

### Respuesta 429

```java
return new HttpResponse.Builder()
    .setStatusCode(429)
    .setContentType(ContentType.TEXT)
    .addHeader("Retry-After", String.valueOf(window.toSeconds()))
    .setBody("429 - Too Many Requests")
    .build();
```

---

[← Anterior](classes-core.md) · [Siguiente →](classes-support.md)  
[🇪🇸 Español](classes-middleware.md) · [🇬🇧 English](classes-middleware.en.md)
