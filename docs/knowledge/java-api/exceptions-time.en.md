# Exceptions & Time API

## Exceptions

### Exception Hierarchy

```
Throwable
├── Error               (serious errors, not recoverable)
│   └── OutOfMemoryError, StackOverflowError, ...
└── Exception           (recoverable)
    ├── RuntimeException (unchecked — not mandatory to catch)
    │   ├── IllegalArgumentException
    │   ├── NullPointerException
    │   ├── IllegalStateException
    │   └── NumberFormatException
    └── (checked — mandatory to catch or declare throws)
        ├── IOException
        │   ├── EOFException
        │   ├── FileNotFoundException
        │   └── NoSuchFileException
        ├── InterruptedException
        └── WriterException (ZXing)
```

### Checked vs Unchecked

**Checked:** The compiler forces you to catch them (try-catch) or declare them (`throws`).

```java
public static ApkInfo read(Path apkPath) throws IOException { ... }
// Caller must catch IOException.

server.addRoute(HttpMethod.POST, "/api/data", req -> {
    try {
        // code that throws a checked exception
    } catch (IOException e) {
        return errorResponse;
    }
});
```

**Unchecked (RuntimeException):** Not mandatory to catch. Used for programming errors.

```java
throw new IllegalArgumentException("Not a directory: " + rootDirectory);
throw new IllegalStateException("CORS: Cannot set allowCredentials(true) with allowOrigin(\"*\")");
```

### IOException

Checked exception for failed I/O operations.

```java
try (ApkFile apkFile = new ApkFile(apkPath.toFile())) {
    // ...
} catch (IOException e) {
    System.err.println("Error reading APK: " + e.getMessage());
}
```

### EOFException

Subclass of `IOException` — end of file reached unexpectedly.

```java
if (read == -1) throw new EOFException("Unexpected EOF");
```

### NoSuchFileException

Subclass of `IOException` — the file does not exist.

```java
try {
    byte[] data = Files.readAllBytes(file.toPath());
} catch (NoSuchFileException e) {
    // file not found → 404
}
```

### InterruptedException

Thrown when a thread is interrupted while waiting/blocking.

```java
try {
    shutdownLatch.await();
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();  // restore interrupt flag
}

try {
    threadPool.awaitTermination(5, TimeUnit.SECONDS);
} catch (InterruptedException e) {
    threadPool.shutdownNow();
    Thread.currentThread().interrupt();
}
```

**Good practice:** always restore the interrupt flag with `Thread.currentThread().interrupt()`.

### NumberFormatException

RuntimeException — invalid numeric format.

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

RuntimeException — invalid argument.

```java
if (!rootDirectory.toFile().isDirectory()) {
    throw new IllegalArgumentException("Not a directory: " + rootDirectory);
}
```

### IllegalStateException

RuntimeException — invalid object state.

```java
if (allow && "*".equals(allowOrigin)) {
    throw new IllegalStateException(
        "CORS: Cannot set allowCredentials(true) with allowOrigin(\"*\"). ...");
}
```

### WriterException (ZXing)

Checked exception from the ZXing library — error generating QR code.

```java
try {
    String svg = QRCodeGenerator.generateSVG(targetUrl, size);
} catch (WriterException e) {
    System.err.println("[QrStaticSite] QR generation failed for " + targetUrl);
}
```

### Try-With-Resources

Automatically closes `AutoCloseable`:

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

Catch multiple exceptions in a single catch:

```java
catch (IOException | URISyntaxException e) {
    return Optional.empty();
}
```

### Throws in Method Signature

```java
public HttpServer(int port, int parallelism) throws IOException {
    this.socket = new ServerSocket(port);  // may throw IOException
}

public static ApkInfo read(Path apkPath) throws IOException { ... }
```

### Finally (not used directly, but relevant)

`finally` always executes, whether an exception occurs or not. Try-with-resources replaces `finally` for closing resources.

```java
// Before try-with-resources:
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

Modern and comprehensive API for working with dates, times, instants and durations.

### Instant

An instant on the timeline (epoch: 1970-01-01T00:00:00Z).

```java
Instant now = Instant.now();          // current moment (UTC)
Instant cutoff = now.minus(window);   // now - duration
timestamps.peek().isBefore(cutoff);   // temporal comparison
```

**Uses:**
- Marking request timestamps (RateLimitMiddleware)
- Calculating sliding time windows

### Duration

Amount of time in nanoseconds.

```java
Duration window = Duration.ofSeconds(windowSeconds);  // e.g.: 60 seconds
```

**Methods:**
```java
window.toSeconds();     // duration in seconds
now.minus(window);      // subtracts duration from an Instant
```

**Uses:** Rate limiting window, timeouts.

### ZonedDateTime

Date and time with time zone.

```java
ZonedDateTime.now(ZoneOffset.UTC)
```

### ZoneOffset

Offset from UTC.

```java
ZoneOffset.UTC  // +00:00
```

### DateTimeFormatter

Date formatting and parsing.

```java
private static final DateTimeFormatter DATE_FMT =
    DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.US);
// Format: "13/Jun/2026:14:30:00 +0000"

// Usage:
ZonedDateTime.now(ZoneOffset.UTC).format(DATE_FMT)
```

**Common patterns:**
| Pattern | Meaning | Example |
|---------|---------|---------|
| `dd` | Day (2 digits) | `13` |
| `MMM` | Abbreviated month | `Jun` |
| `yyyy` | 4-digit year | `2026` |
| `HH` | Hour (0-23) | `14` |
| `mm` | Minutes | `30` |
| `ss` | Seconds | `00` |
| `Z` | Time zone | `+0000` |
| `z` | Zone name | `UTC` |

### RFC_1123_DATE_TIME

Standard formatter for HTTP dates (RFC 1123):

```java
DateTimeFormatter.RFC_1123_DATE_TIME
// Format: "Sat, 13 Jun 2026 14:30:00 GMT"
```

**Usage in HttpResponse.Builder:**
```java
addHeader("Date", ZonedDateTime.now(ZoneOffset.UTC)
    .format(DateTimeFormatter.RFC_1123_DATE_TIME));
```

### ChronoUnit (mention)

For conversions:

```java
long nanos = ChronoUnit.NANOS.between(start, end);
```

Not used directly in the project (calculations use `System.nanoTime()`).

---

## System.nanoTime()

High-precision timer (nanoseconds), used to measure elapsed time.

```java
long start = System.nanoTime();       // start
long elapsed = System.nanoTime() - start;  // duration (ns)
long ms = elapsed / 1_000_000;        // convert to milliseconds
```

**Do not use** `System.currentTimeMillis()` for short duration measurements — it lacks precision and can jump due to clock adjustments.

---

## TimeUnit

Conversions between time units.

```java
if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
    threadPool.shutdownNow();
}
```

**Constants:** `NANOSECONDS`, `MICROSECONDS`, `MILLISECONDS`, `SECONDS`, `MINUTES`, `HOURS`, `DAYS`.

---

[← Previous](streams-lambdas.en.md) · [Next →](misc.en.md)  
[🇪🇸 Español](exceptions-time.md) · [🇬🇧 English](exceptions-time.en.md)
