# Misc Utilities

## Optional\<T\>

Container that may or may not contain a value (avoids `null`).

```java
// Creation:
Optional<byte[]> body = Optional.empty();                    // empty
Optional<byte[]> body = Optional.of(data);                   // non-null (throws NPE if null)
Optional<byte[]> body = Optional.ofNullable(data);           // accepts null → empty
```

### Usage in the project

**Request/response fields:**
```java
private final Optional<byte[]> body;
// body can be present or absent
```

**Return of parsing methods:**
```java
public static Optional<HttpRequest> decode(InputStream inputStream) {
    // Optional.empty() if parsing fails
}

public Optional<String> getHeader(String name) {
    var values = headers.get(name);
    if (values == null || values.isEmpty()) return Optional.empty();
    return Optional.of(String.join(", ", values));
}
```

**Key methods:**
```java
body.isPresent()           // → boolean
body.isEmpty()             // → boolean (Java 11+)
body.get()                 // → T (throws NoSuchElementException if empty)
body.orElse(defaultValue)  // → T
body.orElseGet(() -> ...)  // → T (lazy)
body.ifPresent(val -> ...) // Consumer
body.map(val -> transform) // → Optional<U>
```

**Examples:**
```java
response.getBody().ifPresent(builder::setBody);   // copy body if exists

req.bodyAsJson().orElse(Map.of());                 // empty Map if no body

response.getBody().map(b -> (long) b.length).orElse(0L);  // size or 0

body.map(b -> new String(b, StandardCharsets.UTF_8));  // convert to String
```

---

## StringBuilder

Mutable, efficient for building strings by concatenating many parts.

```java
var sb = new StringBuilder();              // default capacity (16)
var sb = new StringBuilder(256);           // custom initial capacity
```

**Key methods:**
```java
sb.append("text");              // appends any type
sb.append(42);                   // appends int
sb.append("\n");                 // line break
sb.toString();                   // → final String
sb.setLength(0);                 // resets the builder
```

**Usage in the project:**

```java
// Building log line (AccessLogMiddleware):
var sb = new StringBuilder();
sb.append(remoteAddr).append(" - - [").append(timestamp).append("] ...");

// Building Set-Cookie header:
var sb = new StringBuilder();
sb.append(name).append("=").append(value);
if (maxAge > 0) sb.append("; Max-Age=").append(maxAge);
if (path != null) sb.append("; Path=").append(path);
if (httpOnly) sb.append("; HttpOnly");

// Building JSON response (DemoServer):
var sb = new StringBuilder("{");
var it = map.entrySet().iterator();
while (it.hasNext()) {
    var e = it.next();
    sb.append('"').append(e.getKey()).append('"').append(':');
    sb.append('"').append(e.getValue().replace("\"", "\\\"")).append('"');
    if (it.hasNext()) sb.append(", ");
}
sb.append('}');

// Building APK info (ApkReader):
var sb = new StringBuilder();
sb.append("=== APK Information ===\n");
sb.append("Package: ").append(info.packageName()).append("\n");
// ...
```

**vs String concatenation (`+`):**
- `+` creates multiple intermediate `String` objects
- `StringBuilder` avoids them — O(n) vs O(n²)
- The compiler optimizes simple concatenations, but in loops or long constructions, explicitly use `StringBuilder`

---

## String Methods

### `String.format()`

```java
String logLine = String.format(Locale.US,
    "%s - - [%s] \"%s %s %s\" %d %d (%dms)",
    remoteAddr, timestamp, method, path, protocol, status, bodySize, ms);
```

**Placeholders:**
| Specifier | Type |
|-----------|------|
| `%s` | String |
| `%d` | Integer (int, long) |
| `%f` | Decimal (float, double) |
| `%n` | Line break |

### `String.join()`

```java
String.join(", ", allowMethods)     // "GET, POST, PUT, DELETE"
String.join(", ", headers.get(name)) // multi-header values concatenated
```

### `String.split()`

```java
parts = requestLine.split(" ");           // split by space
"chunked".split("&");                    // query parameters
headerLine.split(":", 2);                // key: value (limit 2)
"path/to/file".split("/");              // path segments
sizeStr.split(";")[0];                   // remove chunk extensions
```

### `String.replace()`

```java
content.replace("{{" + entry.getKey() + "}}", entry.getValue());
path.replace("/index.html", "");
"\"".replace("\"", "\\\"")              // escape quotes in JSON
```

### `String.toLowerCase()` / `toUpperCase()`

```java
key.equalsIgnoreCase("Content-Length"); // case-insensitive comparison
ext.strip().toLowerCase();              // normalize extension
enc.toLowerCase().contains("gzip")      // search in any case
```

### `String.substring()`

```java
headerLine.substring(0, colon).trim();          // key
headerLine.substring(colon + 1).trim();         // value
rawPath.substring(1);                           // remove leading /
raw.substring(idx + 1);                         // query string
```

### `String.indexOf()`

```java
headerLine.indexOf(':');          // colon position
raw.indexOf('?');                 // query string position
chunkSizeLine.indexOf(';');       // chunk extension position
name.lastIndexOf('.');            // last file extension
```

### `String.isEmpty()` / `isBlank()` (Java 11+)

```java
requestLine.isEmpty()            // true if ""
ext.isBlank()                    // true if "", "  ", etc.
```

### `String.startsWith()` / `endsWith()`

```java
relative.startsWith(baseDir)     // path traversal check
path.endsWith(".html")           // check extension
```

### `String.contains()`

```java
rawPath.contains("..")           // path traversal detection
value.contains("chunked")        // Transfer-Encoding detection
```

### `String.trim()`

```java
data.trim()                      // removes leading/trailing whitespace
headerLine.substring(0, colon).trim()  // trim after split
```

### `String.valueOf()`

```java
String.valueOf(maxAge)           // int → String
String.valueOf(window.toSeconds()) // long → String
```

### `StringBuilder.reverse()` (not used, but useful)

```java
sb.reverse().toString()          // reverses the string
```

### Text Blocks (Java 15+)

Multiline strings with `"""`:

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

**Advantages:**
- No line break escapes
- Automatic indentation (strip indent)
- `formatted()` for interpolation (like `String.format()`)

---

## String Comparison

```java
// Case-insensitive:
key.equalsIgnoreCase("Content-Length")

// Null-safe:
Objects.equals(a, b)       // true if both null, false if one null
Objects.requireNonNull(x)  // throws NPE if null
```

---

## System & Runtime

### System

```java
System.out.println(...)      // stdout
System.err.println(...)      // stderr
System.nanoTime()            // high-precision timer
System.currentTimeMillis()   // epoch timestamp (ms)
```

### Runtime

```java
Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
Runtime.getRuntime().availableProcessors()  // CPU cores
```

---

## Locale

```java
Locale.US  // format: decimal point, date MMM/dd/yyyy
```

Used in `String.format(Locale.US, ...)` to guarantee locale-independent formatting:

```java
String.format(Locale.US, "%s - - [%s] \"%s %s %s\" %d %d (%dms)", ...);
```

Without `Locale.US`, in countries like Germany `%d` could use period as thousands separator, and `%f` could use comma instead of decimal point.

---

## ByteArrayOutputStream (revisited)

Dynamic byte buffer in memory.

```java
var buf = new ByteArrayOutputStream(256);  // initial capacity
buf.write(b);                               // write byte
buf.toByteArray();                          // get bytes
buf.size();                                 // bytes written
buf.reset();                                // reset
```

**Uses:**
1. `HttpDecoder.readLine()` — accumulate bytes until `\r\n`
2. `HttpDecoder.readChunkedBody()` — assemble chunks
3. `GzipMiddleware.gzipCompress()` — buffer for GZIPOutputStream

---

## Charset & StandardCharsets

```java
StandardCharsets.UTF_8      // Charset for UTF-8
StandardCharsets.US_ASCII   // Charset for ASCII
StandardCharsets.ISO_8859_1 // Latin-1
```

**Usage:**
```java
new String(body, StandardCharsets.UTF_8);            // bytes → String
text.getBytes(StandardCharsets.UTF_8);                // String → bytes
URLDecoder.decode(kv[0], StandardCharsets.UTF_8);    // URL decode
new OutputStreamWriter(out, StandardCharsets.US_ASCII);  // Writer charset
```

---

## AWT (Abstract Window Toolkit) — java.awt

Used in `QRCodeGenerator` to render QR codes as images.

### BufferedImage

In-memory image with pixel access.

```java
BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
```

**TYPE_INT_RGB:** RGB pixel format (8 bits per channel, no alpha). Uses 3 bytes per pixel.

### Graphics2D

2D graphics context for drawing shapes, text and images.

```java
Graphics2D graphics = image.createGraphics();  // create context
graphics.setColor(Color.WHITE);                // fill color
graphics.fillRect(0, 0, size, size);           // filled rectangle
graphics.setColor(Color.BLACK);                // change color
graphics.fillRect(x, y, 1, 1);                 // individual pixel
```

### Color

Represents a color.

```java
Color.WHITE  // RGB(255, 255, 255)
Color.BLACK  // RGB(0, 0, 0)
Color.RED    // RGB(255, 0, 0)
```

### Full usage in QRCodeGenerator

```java
BufferedImage image = new BufferedImage(matrixSize, matrixSize, BufferedImage.TYPE_INT_RGB);
Graphics2D graphics = image.createGraphics();
graphics.setColor(Color.WHITE);
graphics.fillRect(0, 0, matrixSize, matrixSize);   // white background
graphics.setColor(Color.BLACK);
for (int i = 0; i < matrixSize; i++) {
    for (int j = 0; j < matrixSize; j++) {
        if (byteMatrix.get(i, j)) {                  // black QR pixel
            graphics.fillRect(i, j, 1, 1);
        }
    }
}
```

**Note:** AWT does not require a display or GPU. `BufferedImage` and `Graphics2D` work headless (no screen).

---

## Math

```java
Math.max(1, Math.min(9, level))  // clamp 1-9
Math.min(a, b)
Math.max(a, b)
```

---

## Random (not used)

```java
new Random().nextInt(100)  // random number 0-99
```

---

## Bitwise Operations (in HttpDecoder)

```java
// Not used, but relevant for HTTP protocol:
// CR = 13 (0x0D), LF = 10 (0x0A)
```

---

## HashCode & Equals (generated by record)

`record` generates `equals()` and `hashCode()` automatically based on all fields. This allows using records as keys in hash maps.

---

[← Previous](exceptions-time.en.md) · [Next →](../index.en.md)  
[🇪🇸 Español](misc.md) · [🇬🇧 English](misc.en.md)
