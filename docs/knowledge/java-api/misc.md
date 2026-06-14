# Misc Utilities

## Optional\<T\>

Contenedor que puede o no contener un valor (evita `null`).

```java
// Creación:
Optional<byte[]> body = Optional.empty();                    // vacío
Optional<byte[]> body = Optional.of(data);                   // no nulo (lanza NPE si null)
Optional<byte[]> body = Optional.ofNullable(data);           // acepta null → empty
```

### Uso en el proyecto

**Campos de request/response:**
```java
private final Optional<byte[]> body;
// body puede estar presente o ausente
```

**Retorno de métodos de parsing:**
```java
public static Optional<HttpRequest> decode(InputStream inputStream) {
    // Optional.empty() si el parsing falla
}

public Optional<String> getHeader(String name) {
    var values = headers.get(name);
    if (values == null || values.isEmpty()) return Optional.empty();
    return Optional.of(String.join(", ", values));
}
```

**Métodos clave:**
```java
body.isPresent()           // → boolean
body.isEmpty()             // → boolean (Java 11+)
body.get()                 // → T (lanza NoSuchElementException si vacío)
body.orElse(defaultValue)  // → T
body.orElseGet(() -> ...)  // → T (lazy)
body.ifPresent(val -> ...) // Consumer
body.map(val -> transform) // → Optional<U>
```

**Ejemplos:**
```java
response.getBody().ifPresent(builder::setBody);   // copiar body si existe

req.bodyAsJson().orElse(Map.of());                 // Map vacío si no hay body

response.getBody().map(b -> (long) b.length).orElse(0L);  // tamaño o 0

body.map(b -> new String(b, StandardCharsets.UTF_8));  // convertir a String
```

---

## StringBuilder

Mutable, eficiente para construir strings concatenando muchas partes.

```java
var sb = new StringBuilder();              // capacidad por defecto (16)
var sb = new StringBuilder(256);           // capacidad inicial personalizada
```

**Métodos clave:**
```java
sb.append("texto");              // añade cualquier tipo
sb.append(42);                   // añade int
sb.append("\n");                 // salto de línea
sb.toString();                   // → String final
sb.setLength(0);                 // reinicia el builder
```

**Uso en el proyecto:**

```java
// Construir log line (AccessLogMiddleware):
var sb = new StringBuilder();
sb.append(remoteAddr).append(" - - [").append(timestamp).append("] ...");

// Construir Set-Cookie header:
var sb = new StringBuilder();
sb.append(name).append("=").append(value);
if (maxAge > 0) sb.append("; Max-Age=").append(maxAge);
if (path != null) sb.append("; Path=").append(path);
if (httpOnly) sb.append("; HttpOnly");

// Construir JSON response (DemoServer):
var sb = new StringBuilder("{");
var it = map.entrySet().iterator();
while (it.hasNext()) {
    var e = it.next();
    sb.append('"').append(e.getKey()).append('"').append(':');
    sb.append('"').append(e.getValue().replace("\"", "\\\"")).append('"');
    if (it.hasNext()) sb.append(", ");
}
sb.append('}');

// Construir APK info (ApkReader):
var sb = new StringBuilder();
sb.append("=== APK Information ===\n");
sb.append("Package: ").append(info.packageName()).append("\n");
// ...
```

**vs String concatenation (`+`):**
- `+` crea múltiples objetos `String` intermedios
- `StringBuilder` las evita — es O(n) vs O(n²)
- El compilador optimiza concatenaciones simples, pero en bucles o construcciones largas, usa `StringBuilder` explícitamente

---

## String Methods

### `String.format()`

```java
String logLine = String.format(Locale.US,
    "%s - - [%s] \"%s %s %s\" %d %d (%dms)",
    remoteAddr, timestamp, method, path, protocol, status, bodySize, ms);
```

**Placeholders:**
| Especificador | Tipo |
|---------------|------|
| `%s` | String |
| `%d` | Entero (int, long) |
| `%f` | Decimal (float, double) |
| `%n` | Salto de línea |

### `String.join()`

```java
String.join(", ", allowMethods)     // "GET, POST, PUT, DELETE"
String.join(", ", headers.get(name)) // valores multi-header concatenados
```

### `String.split()`

```java
parts = requestLine.split(" ");           // divide por espacio
"chunked".split("&");                    // parámetros query
headerLine.split(":", 2);                // key: value (límite 2)
"path/to/file".split("/");              // segmentos de ruta
sizeStr.split(";")[0];                   // quitar extensiones de chunk
```

### `String.replace()`

```java
content.replace("{{" + entry.getKey() + "}}", entry.getValue());
path.replace("/index.html", "");
"\"".replace("\"", "\\\"")              // escapar comillas en JSON
```

### `String.toLowerCase()` / `toUpperCase()`

```java
key.equalsIgnoreCase("Content-Length"); // comparación case-insensitive
ext.strip().toLowerCase();              // normalizar extensión
enc.toLowerCase().contains("gzip")      // buscar en cualquier caso
```

### `String.substring()`

```java
headerLine.substring(0, colon).trim();          // key
headerLine.substring(colon + 1).trim();         // value
rawPath.substring(1);                           // quitar primer /
raw.substring(idx + 1);                         // query string
```

### `String.indexOf()`

```java
headerLine.indexOf(':');          // posición del colon
raw.indexOf('?');                 // posición de query string
chunkSizeLine.indexOf(';');       // posición de extensión de chunk
name.lastIndexOf('.');            // última extensión de archivo
```

### `String.isEmpty()` / `isBlank()` (Java 11+)

```java
requestLine.isEmpty()            // true si ""
ext.isBlank()                    // true si "", "  ", etc.
```

### `String.startsWith()` / `endsWith()`

```java
relative.startsWith(baseDir)     // path traversal check
path.endsWith(".html")           // verificar extensión
```

### `String.contains()`

```java
rawPath.contains("..")           // path traversal detection
value.contains("chunked")        // Transfer-Encoding detection
```

### `String.trim()`

```java
data.trim()                      // elimina whitespace inicial/final
headerLine.substring(0, colon).trim()  // trim después de split
```

### `String.valueOf()`

```java
String.valueOf(maxAge)           // int → String
String.valueOf(window.toSeconds()) // long → String
```

### `StringBuilder.reverse()` (no usado, pero útil)

```java
sb.reverse().toString()          // invierte la cadena
```

### Text Blocks (Java 15+)

Strings multilínea con `"""`:

```java
String json = """
    {
        "server": "MiniJWS",
        "version": "1.0-SNAPSHOT"
    }
    """;

String info = """
    Method: %s
    Path: %s
    Headers: %s
    """.formatted(method, path, headers);
```

**Ventajas:**
- Sin escapes de salto de línea
- Indentación automática (strip indent)
- `formatted()` para interpolación (como `String.format()`)

---

## String Comparison

```java
// Case-insensitive:
key.equalsIgnoreCase("Content-Length")

// Null-safe:
Objects.equals(a, b)       // true si ambos null, false si uno null
Objects.requireNonNull(x)  // lanza NPE si null
```

---

## System & Runtime

### System

```java
System.out.println(...)      // stdout
System.err.println(...)      // stderr
System.nanoTime()            // timer de alta precisión
System.currentTimeMillis()   // timestamp epoch (ms)
```

### Runtime

```java
Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
Runtime.getRuntime().availableProcessors()  // CPU cores
```

---

## Locale

```java
Locale.US  // formato: punto decimal, fecha MMM/dd/yyyy
```

Usado en `String.format(Locale.US, ...)` para garantizar formato independiente de la configuración regional:

```java
String.format(Locale.US, "%s - - [%s] \"%s %s %s\" %d %d (%dms)", ...);
```

Sin `Locale.US`, en países como Alemania `%d` podría usar separador de miles con punto, y `%f` usar coma en vez de punto decimal.

---

## ByteArrayOutputStream (revisited)

Buffer de bytes dinámico en memoria.

```java
var buf = new ByteArrayOutputStream(256);  // capacidad inicial
buf.write(b);                               // escribir byte
buf.toByteArray();                          // obtener bytes
buf.size();                                 // bytes escritos
buf.reset();                                // reiniciar
```

**Usos:**
1. `HttpDecoder.readLine()` — acumular bytes hasta `\r\n`
2. `HttpDecoder.readChunkedBody()` — ensamblar chunks
3. `GzipMiddleware.gzipCompress()` — buffer para GZIPOutputStream

---

## Charset & StandardCharsets

```java
StandardCharsets.UTF_8      // Charset para UTF-8
StandardCharsets.US_ASCII   // Charset para ASCII
StandardCharsets.ISO_8859_1 // Latin-1
```

**Uso:**
```java
new String(body, StandardCharsets.UTF_8);            // bytes → String
text.getBytes(StandardCharsets.UTF_8);                // String → bytes
URLDecoder.decode(kv[0], StandardCharsets.UTF_8);    // URL decode
new OutputStreamWriter(out, StandardCharsets.US_ASCII);  // Writer charset
```

---

## AWT (Abstract Window Toolkit) — java.awt

Usado en `QRCodeGenerator` para renderizar códigos QR como imágenes.

### BufferedImage

Imagen en memoria con acceso a píxeles.

```java
BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
```

**TYPE_INT_RGB:** formato de píxel RGB (8 bits por canal, sin alpha). Tiene 3 bytes por píxel.

### Graphics2D

Contexto gráfico 2D para dibujar formas, texto e imágenes.

```java
Graphics2D graphics = image.createGraphics();  // crear contexto
graphics.setColor(Color.WHITE);                // color de relleno
graphics.fillRect(0, 0, size, size);           // rectángulo relleno
graphics.setColor(Color.BLACK);                // cambiar color
graphics.fillRect(x, y, 1, 1);                // píxel individual
```

### Color

Representa un color.

```java
Color.WHITE  // RGB(255, 255, 255)
Color.BLACK  // RGB(0, 0, 0)
Color.RED    // RGB(255, 0, 0)
```

### Uso completo en QRCodeGenerator

```java
BufferedImage image = new BufferedImage(matrixSize, matrixSize, BufferedImage.TYPE_INT_RGB);
Graphics2D graphics = image.createGraphics();
graphics.setColor(Color.WHITE);
graphics.fillRect(0, 0, matrixSize, matrixSize);   // fondo blanco
graphics.setColor(Color.BLACK);
for (int i = 0; i < matrixSize; i++) {
    for (int j = 0; j < matrixSize; j++) {
        if (byteMatrix.get(i, j)) {                  // píxel del QR negro
            graphics.fillRect(i, j, 1, 1);
        }
    }
}
```

**Nota:** AWT no requiere display ni GPU. `BufferedImage` y `Graphics2D` funcionan headless (sin pantalla).

---

## Math

```java
Math.max(1, Math.min(9, level))  // clamp 1-9
Math.min(a, b)
Math.max(a, b)
```

---

## Random (no usado)

```java
new Random().nextInt(100)  // número aleatorio 0-99
```

---

## Bitwise Operations (en HttpDecoder)

```java
// No se usan, pero son relevantes para protocolo HTTP:
// CR = 13 (0x0D), LF = 10 (0x0A)
```

---

## HashCode & Equals (generados por record)

Los `record` generan `equals()` y `hashCode()` automáticamente basados en todos los campos. Esto permite usar records como keys en hash maps.

---

[← Anterior](exceptions-time.md) · [Siguiente →](../index.md)  
[🇪🇸 Español](misc.md) · [🇬🇧 English](misc.en.md)
