# Exceptions & Time API

## Excepciones

### Jerarquía de Excepciones

```
Throwable
├── Error               (errores graves, no recuperables)
│   └── OutOfMemoryError, StackOverflowError, ...
└── Exception           (recuperables)
    ├── RuntimeException (unchecked — no obligatorio capturar)
    │   ├── IllegalArgumentException
    │   ├── NullPointerException
    │   ├── IllegalStateException
    │   └── NumberFormatException
    └── (checked — obligatorio capturar o declarar throws)
        ├── IOException
        │   ├── EOFException
        │   ├── FileNotFoundException
        │   └── NoSuchFileException
        ├── InterruptedException
        └── WriterException (ZXing)
```

### Checked vs Unchecked

**Checked:** El compilador obliga a capturarlas (try-catch) o declararlas (`throws`).

```java
public static ApkInfo read(Path apkPath) throws IOException { ... }
// Quien llama debe capturar IOException.

server.addRoute(HttpMethod.POST, "/api/data", req -> {
    try {
        // código que lanza excepción checked
    } catch (IOException e) {
        return errorResponse;
    }
});
```

**Unchecked (RuntimeException):** No obligatorio capturar. Se usan para errores de programación.

```java
throw new IllegalArgumentException("Not a directory: " + rootDirectory);
throw new IllegalStateException("CORS: Cannot set allowCredentials(true) with allowOrigin(\"*\")");
```

### IOException

Excepción checked para operaciones de I/O fallidas.

```java
try (ApkFile apkFile = new ApkFile(apkPath.toFile())) {
    // ...
} catch (IOException e) {
    System.err.println("Error reading APK: " + e.getMessage());
}
```

### EOFException

Subclase de `IOException` — se alcanzó el fin del archivo inesperadamente.

```java
if (read == -1) throw new EOFException("Unexpected EOF");
```

### NoSuchFileException

Subclase de `IOException` — el archivo no existe.

```java
try {
    byte[] data = Files.readAllBytes(file.toPath());
} catch (NoSuchFileException e) {
    // archivo no encontrado → 404
}
```

### InterruptedException

Se lanza cuando un hilo es interrumpido mientras espera/bloquea.

```java
try {
    shutdownLatch.await();
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();  // restaurar flag de interrupción
}

try {
    threadPool.awaitTermination(5, TimeUnit.SECONDS);
} catch (InterruptedException e) {
    threadPool.shutdownNow();
    Thread.currentThread().interrupt();
}
```

**Buena práctica:** siempre restaurar el flag de interrupción con `Thread.currentThread().interrupt()`.

### NumberFormatException

RuntimeException — formato numérico inválido.

```java
try {
    contentLength = Integer.parseInt(value);
} catch (NumberFormatException e) {
    return Optional.empty();
}

try {
    size = Integer.parseInt(sizeStr, 16);  // hexadecimal
} catch (NumberFormatException e) {
    break;
}
```

### IllegalArgumentException

RuntimeException — argumento inválido.

```java
if (!rootDirectory.toFile().isDirectory()) {
    throw new IllegalArgumentException("Not a directory: " + rootDirectory);
}
```

### IllegalStateException

RuntimeException — estado inválido del objeto.

```java
if (allow && "*".equals(allowOrigin)) {
    throw new IllegalStateException(
        "CORS: Cannot set allowCredentials(true) with allowOrigin(\"*\"). ...");
}
```

### WriterException (ZXing)

Checked exception de la librería ZXing — error generando código QR.

```java
try {
    String svg = QRCodeGenerator.generateSVG(targetUrl, size);
} catch (WriterException e) {
    System.err.println("[QrStaticSite] QR generation failed for " + targetUrl);
}
```

### Try-With-Resources

Cierra automáticamente `AutoCloseable`:

```java
try (Socket s = client;
     InputStream in = s.getInputStream();
     OutputStream out = s.getOutputStream()) {
    // ...
} catch (IOException e) {
    System.err.println("Connection error: " + e.getMessage());
}
```

### Multi-Catch (Java 7+)

Captura múltiples excepciones en un solo catch:

```java
catch (IOException | URISyntaxException e) {
    return Optional.empty();
}
```

### Throws en Firma de Método

```java
public HttpServer(int port, int parallelism) throws IOException {
    this.socket = new ServerSocket(port);  // puede lanzar IOException
}

public static ApkInfo read(Path apkPath) throws IOException { ... }
```

### Finally (no usado directamente, pero relevante)

`finally` se ejecuta siempre, haya excepción o no. El try-with-resources reemplaza `finally` para cerrar recursos.

```java
// Antes de try-with-resources:
InputStream in = null;
try {
    in = new FileInputStream("file.txt");
    // ...
} finally {
    if (in != null) in.close();
}
```

---

## Time API (java.time — Java 8+)

API moderna y completa para trabajar con fechas, horas, instantes y duraciones.

### Instant

Un instante en la línea de tiempo (epoch: 1970-01-01T00:00:00Z).

```java
Instant now = Instant.now();          // momento actual (UTC)
Instant cutoff = now.minus(window);   // ahora - duración
timestamps.peek().isBefore(cutoff);   // comparación temporal
```

**Usos:**
- Marcar timestamps de requests (RateLimitMiddleware)
- Calcular ventanas de tiempo deslizantes

### Duration

Cantidad de tiempo en nanosegundos.

```java
Duration window = Duration.ofSeconds(windowSeconds);  // ej: 60 segundos
```

**Métodos:**
```java
window.toSeconds();     // duración en segundos
now.minus(window);      // resta duración a un Instant
```

**Usos:** Ventana de rate limiting, timeouts.

### ZonedDateTime

Fecha y hora con zona horaria.

```java
ZonedDateTime.now(ZoneOffset.UTC)
```

### ZoneOffset

Desplazamiento desde UTC.

```java
ZoneOffset.UTC  // +00:00
```

### DateTimeFormatter

Formateo y parseo de fechas.

```java
private static final DateTimeFormatter DATE_FMT =
    DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.US);
// Formato: "13/Jun/2026:14:30:00 +0000"

// Uso:
ZonedDateTime.now(ZoneOffset.UTC).format(DATE_FMT)
```

**Patrones comunes:**
| Patrón | Significado | Ejemplo |
|--------|-------------|---------|
| `dd` | Día (2 dígitos) | `13` |
| `MMM` | Mes abreviado | `Jun` |
| `yyyy` | Año 4 dígitos | `2026` |
| `HH` | Hora (0-23) | `14` |
| `mm` | Minutos | `30` |
| `ss` | Segundos | `00` |
| `Z` | Zona horaria | `+0000` |
| `z` | Nombre de zona | `UTC` |

### RFC_1123_DATE_TIME

Formateador estándar para fechas HTTP (RFC 1123):

```java
DateTimeFormatter.RFC_1123_DATE_TIME
// Formato: "Sat, 13 Jun 2026 14:30:00 GMT"
```

**Uso en HttpResponse.Builder:**
```java
addHeader("Date", ZonedDateTime.now(ZoneOffset.UTC)
    .format(DateTimeFormatter.RFC_1123_DATE_TIME));
```

### ChronoUnit (mención)

Para conversiones:

```java
long nanos = ChronoUnit.NANOS.between(start, end);
```

No usado directamente en el proyecto (se calcula con `System.nanoTime()`).

---

## System.nanoTime()

Timer de alta precisión (nanosegundos), usado para medir tiempos transcurridos.

```java
long start = System.nanoTime();       // inicio
long elapsed = System.nanoTime() - start;  // duración (ns)
long ms = elapsed / 1_000_000;        // convertir a milisegundos
```

**No usar** `System.currentTimeMillis()` para medir duraciones cortas — no tiene suficiente precisión y puede saltar por ajustes de reloj.

---

## TimeUnit

Conversiones entre unidades de tiempo.

```java
if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
    threadPool.shutdownNow();
}
```

**Constantes:** `NANOSECONDS`, `MICROSECONDS`, `MILLISECONDS`, `SECONDS`, `MINUTES`, `HOURS`, `DAYS`.

---

[← Anterior](streams-lambdas.md) · [Siguiente →](misc.md)  
[🇪🇸 Español](exceptions-time.md)
