# Clases del Núcleo

## HttpServer

**Paquete:** `io.github.blacknoize404.miniJWS.HttpServer`
**Archivo:** `miniJWS-core/src/main/java/.../HttpServer.java`

El orquestador central. Gestiona el `ServerSocket`, el pool de hilos, la tabla de rutas, la cadena de middleware y el ciclo de vida de las conexiones.

### Campos Clave

| Campo | Tipo | Propósito |
|-------|------|-----------|
| `routes` | `Map<String, RequestRunner>` | `ConcurrentHashMap` — clave = `MÉTODO:/ruta` |
| `middlewares` | `List<Middleware>` | `CopyOnWriteArrayList` — seguro para cargas de lectura intensiva |
| `socket` | `ServerSocket` | Aceptador TCP |
| `threadPool` | `ExecutorService` | Pool de hilos fijo (por defecto: `2 × núcleos CPU`) |
| `running` | `AtomicBoolean` | Controla el bucle de aceptación |
| `shutdownLatch` | `CountDownLatch` | Bloquea `idle()` hasta que `stop()` se dispara |

### Ciclo de Vida

1. **Constructor:** Abre `ServerSocket`, crea el pool de hilos
2. **Configuración:** `addRoute()`, `addStaticRoute()`, `use()` — todos devuelven `this` (fluido)
3. **`run()`:** Establece `running=true`, registra el hook SIGINT, entra en el bucle de aceptación. Cada socket aceptado se envía a `handleConnection()` mediante el pool de hilos.
4. **`stop()`:** Establece `running=false`, decrementa el latch, cierra el socket
5. **`idle()`:** Bloquea esperando el latch (para modo demonio/servidor)
6. **Apagado:** `run()` llama a `shutdown()` después de que el bucle de aceptación termina, drenando el pool de hilos

### Bucle Keep-Alive (`handleConnection()`)

```
accept → for(hasta 100 peticiones):
           decode(request) → cadena de middleware → encode(response)
           if Connection:close o timeout → break
         cerrar socket
```

### Construcción de la Cadena de Middleware

El método `buildChain()` crea una cadena de lambdas anidadas. El último middleware registrado se ejecuta primero (envuelve el terminal). El middleware terminal hace el emparejamiento de rutas:

1. Coincidencia exacta (`routes.get(key)`)
2. Coincidencia por parámetros de ruta (`findRouteWithParams()` — escaneo lineal de todas las rutas)
3. 404

### Emparejamiento de Rutas (`matchPath()`)

```java
static Map<String, String> matchPath(String pattern, String path)
```

Comparación segmento por segmento:
- Segmento estático: debe coincidir exactamente
- `:param`: captura el segmento en el mapa de parámetros
- `*`: coincide con cualquier segmento individual (sin captura)
- `**`: coincide con todos los segmentos restantes, retorna inmediatamente

---

## HttpRequest

**Paquete:** `io.github.blacknoize404.miniJWS.requests.HttpRequest`
**Archivo:** `miniJWS-core/src/main/java/.../requests/HttpRequest.java`

Modelo de petición inmutable construido por la clase interna `Builder`.

### Campos

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `httpMethod` | `HttpMethod` | Enum: GET, POST, etc. |
| `uri` | `URI` | URI parseada (solo ruta, query separada) |
| `protocolVersion` | `String` | p.ej. `HTTP/1.1` |
| `headers` | `Map<String, List<String>>` | Cabeceras multivalor (claves con su formato original) |
| `parameters` | `Map<String, String>` | Parámetros de query + ruta combinados (los de ruta tienen prioridad) |
| `cookies` | `Map<String, String>` | Parseadas de la cabecera `Cookie` en tiempo de construcción |
| `body` | `Optional<byte[]>` | Bytes del cuerpo en bruto |

### Parseo de Cookies

`parseCookies()` se ejecuta durante la construcción. Divide la cabecera `Cookie` por `;`, luego cada par por `=`:

```
Cookie: session=abc123; token=xyz
→ {session: "abc123", token: "xyz"}
```

### Métodos de Parseo del Cuerpo

| Método | Implementación |
|--------|----------------|
| `bodyAsString()` | `new String(body, UTF_8)` |
| `bodyAsForm()` | Divide por `&`, luego `=`, decodifica URL cada parte |
| `bodyAsJson()` | Parser JSON plano escrito a mano (sin librería externa) — pares clave:valor separados por `,`, maneja cadenas escapadas y entrecomilladas |

El parser JSON es intencionadamente **plano** — solo maneja un nivel de `{"clave": "valor", ...}`. Los objetos o arrays anidados se saltan (el seguimiento de profundidad omite contenido dentro de `{}` y `[]`).

---

## HttpResponse

**Paquete:** `io.github.blacknoize404.miniJWS.responses.HttpResponse`
**Archivo:** `miniJWS-core/src/main/java/.../responses/HttpResponse.java`

Modelo de respuesta inmutable construido por la clase interna `Builder`.

### Cabeceras por Defecto

Toda respuesta incluye automáticamente:
- `Server: MiniJWS` (de `HttpServer.SERVER_NAME`)
- `Date: <marca temporal RFC 1123>` (generada en tiempo de construcción)

### Factoría de Redirección

```java
HttpResponse.redirect("/login");         // 302 Found
HttpResponse.redirect("/new-url", 301);  // 301 Moved Permanently
```

Ambos construyen una respuesta con cabecera `Location` y sin cuerpo.

### Soporte de Cookies

```java
builder.setCookie("name", "value");                         // simple
builder.setCookie("name", "value", 3600, "/", true);        // con Max-Age, Path, HttpOnly
```

Internamente añade una cabecera `Set-Cookie`. La variante completa construye `name=value; Max-Age=3600; Path=/; HttpOnly`.

### Builder

```java
new HttpResponse.Builder()
    .setStatusCode(200)
    .setContentType(ContentType.JSON)
    .addHeader("Cache-Control", "no-cache")
    .setBody("{\"ok\":true}")
    .build();
```

---

## HttpDecoder

**Paquete:** `io.github.blacknoize404.miniJWS.requests.HttpDecoder`
**Archivo:** `miniJWS-core/src/main/java/.../requests/HttpDecoder.java`

Clase utilitaria estática que parsea `InputStream → Optional<HttpRequest>`.

### Algoritmo de Parseo

1. **Línea de petición:** `readLine()` → dividir por espacio → `HttpMethod`, `URI`, protocolo
2. **Cabeceras:** Bucle `readLine()` hasta línea vacía. Cada línea se divide por `:`. `Content-Length` y `Transfer-Encoding` se rastrean durante el parseo de cabeceras.
3. **Obs-fold:** Las líneas que empiezan con espacio/tabulador son continuación del valor de la cabecera anterior.
4. **Cuerpo:**
   - Si `Transfer-Encoding: chunked` → `readChunkedBody()` (parsear tamaño hex, quitar extensiones, leer chunk, saltar CRLF)
   - Si `Content-Length > 0` → `readExactBody()` (lectura bloqueante de bytes exactos)
5. **URI:** la porción de query se parsea por separado → `parseQueryParams()` con `URLDecoder.decode(UTF_8)`

### Implementación de readLine()

Bucle byte a byte acumulando bytes hasta encontrar `\r\n`:

```java
while ((b = in.read()) != -1) {
    if (b == CR) { crFound = true; }
    else if (b == LF) { return buf.toString(US_ASCII); }
    else { if (crFound) buf.write(CR); buf.write(b); }
}
```

Longitud máxima de línea: `8_192` bytes (devuelve `null` si se excede).

### Límites de Tamaño

- `MAX_CHUNK_SIZE`: 10 MiB por chunk
- `MAX_CONTENT_LENGTH`: 50 MiB total del cuerpo
- `Content-Length` duplicado → rechazar (devolver vacío)

---

## HttpEncoder

**Paquete:** `io.github.blacknoize404.miniJWS.responses.HttpEncoder`
**Archivo:** `miniJWS-core/src/main/java/.../responses/HttpEncoder.java`

Clase utilitaria estática que serializa `HttpResponse → OutputStream`.

### Orden de Serialización

1. Línea de estado: `HTTP/1.1 200 OK\r\n`
2. Todas las cabeceras: `Clave: valor\r\n`
3. Cabecera Content-Length (si hay cuerpo)
4. Línea vacía `\r\n`
5. Bytes del cuerpo (escritos directamente al `OutputStream`, no a través del escritor de texto)

**Detalle crítico:** Las cabeceras se escriben mediante un `BufferedWriter(OutputStreamWriter(outputStream, US_ASCII))` y se vacían antes de que el cuerpo se escriba directamente vía `outputStream.write(data)`. Esto evita que el escritor US-ASCII corrompa contenido binario (gzip, imágenes, secuencias UTF-8 multibyte).

---

## StaticFileHandler

**Paquete:** `io.github.blacknoize404.miniJWS.handlers.StaticFileHandler`
**Archivo:** `miniJWS-core/src/main/java/.../handlers/StaticFileHandler.java`

Implementa `RequestRunner` para servir archivos desde un directorio.

### Seguridad

Protección de tres capas contra path traversal:
1. **Verificación de cadena:** `rawPath.contains("..")` → 400
2. **Verificación de normalización:** `baseDir.resolve(relative).normalize().startsWith(baseDir)` → 403
3. **Condición de carrera:** `NoSuchFileException` capturada → 404 en lugar de 500

### Características

- Archivos de índice de directorio (por defecto: `index.html`, configurable)
- Detección MIME mediante `ContentType.fromExtension()`
- Soporte de archivos binarios (bytes sin procesar)

### Resolución Archivo → Content-Type

Usa `ContentType.fromExtension(ext)` que mapea la extensión del archivo al tipo MIME mediante el `EXT_MAP` en `ContentType`. Las extensiones desconocidas por defecto usan `application/octet-stream`.

---

[← Anterior](decisions.md) · [Siguiente →](classes-middleware.md)  
[🇪🇸 Español](classes-core.md) · [🇬🇧 English](classes-core.en.md)
