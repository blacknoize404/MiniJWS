# I/O & Networking

## Streams

### InputStream — Reading bytes

Abstract base class for reading binary data.

```java
InputStream in = s.getInputStream();
```

**Key methods:**
| Method | Description |
|--------|-------------|
| `int read()` | Reads one byte (0-255) or -1 if EOF |
| `int read(byte[], off, len)` | Reads up to `len` bytes into the buffer |
| `void close()` | Closes the stream |

### OutputStream — Writing bytes

```java
OutputStream out = s.getOutputStream();
```

**Key methods:**
| Method | Description |
|--------|-------------|
| `void write(int)` | Writes one byte |
| `void write(byte[])` | Writes the entire array |
| `void write(byte[], off, len)` | Writes `len` bytes from `off` |
| `void flush()` | Forces buffered bytes to be written |
| `void close()` | Closes the stream |

### BufferedInputStream

Adds an internal buffer to reduce system calls.

```java
var reader = new BufferedInputStream(in);
```

**Methods:**
- Inherits `read()` from `InputStream`
- `int available()` — bytes available without blocking (DO NOT USE for HTTP parsing — known bug)
- Internally uses an 8KB buffer by default

### BufferedOutputStream / BufferedWriter

Buffer for efficient writing.

```java
var writer = new BufferedWriter(
    new OutputStreamWriter(outputStream, StandardCharsets.US_ASCII));
```

### OutputStreamWriter

Bridge between characters (Writer) and bytes (OutputStream). Converts characters to bytes using a charset.

```java
new OutputStreamWriter(outputStream, StandardCharsets.US_ASCII)
```

### ByteArrayOutputStream

OutputStream that writes to a resizable internal buffer (in memory).

```java
var buf = new ByteArrayOutputStream(256);  // initial capacity 256
var body = new ByteArrayOutputStream();
var bos = new ByteArrayOutputStream(data.length / 2);
```

**Methods:**
| Method | Description |
|--------|-------------|
| `write(int)` | Writes one byte |
| `write(byte[])` | Writes bytes |
| `toByteArray()` | Returns a copy of the buffer as `byte[]` |
| `size()` | Number of bytes written |
| `reset()` | Resets the buffer |

Used in:
- `HttpDecoder.readLine()` — accumulates bytes until `\r\n`
- `HttpDecoder.readChunkedBody()` — assembles chunks
- `GzipMiddleware.gzipCompress()` — buffer for compressed data

---

## File I/O

### File

Legacy class for representing file paths.

```java
File file = resolved.toFile();
file.isFile();
file.isDirectory();
file.exists();
file.getName();
file.getPath();
file.listFiles();
new File(dir, "index.html");
```

### Path (Java 7+ — NIO.2)

Represents a path (replaces `File` for new APIs).

```java
Path apkPath = Path.of(args[0]);  // Java 11+
Path resolved = baseDir.resolve(relative);
Path root = Path.of("data");
```

### Files (java.nio.file.Files)

File operations using `Path`.

```java
byte[] data = Files.readAllBytes(file.toPath());
// Reads the entire file into memory as byte[]
// Throws IOException (NoSuchFileException if not found)
```

**Not used but relevant:**
- `Files.write(path, bytes)`
- `Files.newInputStream(path)`
- `Files.exists(path)`

### FileWriter

Writer that writes directly to a file.

```java
new FileWriter(filePath, StandardCharsets.UTF_8, true)  // append mode
```

### PrintWriter

Writer with convenient methods (`println`, `printf`, `format`). Does not throw `IOException` internally.

```java
new PrintWriter(new FileWriter(filePath, StandardCharsets.UTF_8, true), true)
// second parameter: autoFlush — flush after each println
```

**Methods used:**
```java
writer.println(line);   // writes line + line break
writer.flush();         // forces write
```

---

## Networking (TCP)

### Socket

TCP client. Connects to a remote server.

```java
Socket client = socket.accept();  // on the server, accepts incoming connection
```

**Important methods:**
```java
client.getInputStream();           // InputStream to read data from the client
client.getOutputStream();          // OutputStream to write to the client
client.setSoTimeout(10_000);       // timeout in milliseconds for read()
client.close();                    // closes the connection
```

**Try-with-resources:**
```java
try (Socket s = client;
     InputStream in = s.getInputStream();
     OutputStream out = s.getOutputStream()) {
    // ... handle connection ...
}
```

Variables in try-with-resources implement `AutoCloseable` and are automatically closed when exiting the block.

### ServerSocket

TCP server that listens on a port and accepts incoming connections.

```java
this.socket = new ServerSocket(port);
```

**Key methods:**
| Method | Description |
|--------|-------------|
| `accept()` | Blocks until a connection arrives → `Socket` |
| `close()` | Closes the socket |
| `getLocalPort()` | Port it is listening on |

### DatagramSocket (UDP)

```java
try (var socket = new DatagramSocket()) {
    socket.connect(InetAddress.getByName("8.8.8.8"), 12345);
    return socket.getLocalAddress().getHostAddress();
}
```

Used in `QrStaticSite.getLocalIp()` to discover the local network IP. The trick: connecting a UDP socket to an external IP makes the OS determine the network interface packets would go through, and `getLocalAddress()` returns the IP of that interface.

### InetAddress

Represents an IP address.

```java
InetAddress.getByName("8.8.8.8")  // resolves hostname/IP
```

---

## URI & URL Decoding

### URI

```java
URI uri = new URI(path);  // parses a URI (throws URISyntaxException if invalid)
uri.getRawPath();         // path without decoding (/hello/world)
```

### URLDecoder

Decodes URLs (percent-encoded). Converts `%20` to spaces, `%2B` to `+`, etc.

```java
URLDecoder.decode(kv[0], StandardCharsets.UTF_8)
```

**Usage:** Decode query string parameters and form-urlencoded bodies.

---

## Try-With-Resources

Automatically closes resources that implement `AutoCloseable`.

```java
// Socket, InputStream, OutputStream, ApkFile, etc.
try (ApkFile apkFile = new ApkFile(apkPath.toFile())) {
    // used and automatically closed
}

try (Socket s = client;
     InputStream in = s.getInputStream();
     OutputStream out = s.getOutputStream()) {
    // multiple resources separated by ;
}
```

Equivalent to:

```java
Socket s = null;
try {
    s = client;
    InputStream in = s.getInputStream();
    // ...
} finally {
    if (s != null) s.close();
}
```

## BufferedReader — Reading text (NOT used, but context)

`BufferedReader` is occasionally used to read text line by line. The project implements its own `readLine()` byte-by-byte to control encoding (US-ASCII for headers) and size limits.

---

## java.util.zip — Compression

### GZIPOutputStream

OutputStream that compresses data in GZIP format.

```java
var bos = new ByteArrayOutputStream(data.length / 2);
var gz = new GZIPOutputStream(bos);
gz.write(data);
gz.close();  // important: writes the trailer (CRC32 + size)
byte[] compressed = bos.toByteArray();
```

**Constructor:**
```java
GZIPOutputStream(OutputStream out)  // default compression level (Deflater.DEFAULT_COMPRESSION)
```

**Usage in GzipMiddleware with custom level:**

```java
var gz = new GZIPOutputStream(bos) {{
    def.setLevel(level);
}};
```

This is **double brace initialization**: the first brace creates an anonymous subclass of `GZIPOutputStream`, the second is an instance initializer. It accesses the `protected Deflater def` field of `DeflaterOutputStream` (superclass) to change the compression level.

### Deflater

Data compressor implementing LZ77 + Huffman coding (deflate algorithm).

```java
Deflater def = new Deflater(level);  // 1-9, 6=default
```

In GzipMiddleware the internal `Deflater` of `GZIPOutputStream` is accessed:
```java
// def is a protected field of DeflaterOutputStream
// accessible from an anonymous subclass
def.setLevel(level);
```

**Levels:**
| Level | Name | Speed | Compression |
|-------|------|-------|-------------|
| 1 | `BEST_SPEED` | Fastest | Minimum |
| 6 | `DEFAULT_COMPRESSION` | Balanced | Good |
| 9 | `BEST_COMPRESSION` | Slowest | Maximum |

### GZIPOutputStream — try-finally

```java
try {
    gz.write(data);
} finally {
    gz.close();  // ensures the trailer is written even if write() fails
}
```

`close()` writes the GZIP trailer (CRC32 checksum + original size). Without it, the compressed stream is incomplete.

---

## System I/O

```java
System.out.println("Hello");     // PrintStream — standard output
System.err.println("Error");     // PrintStream — error output
System.in                         // InputStream — standard input
```

`System.out` is passed as `PrintWriter` in `AccessLogMiddleware`:

```java
new PrintWriter(System.out, true)
```

Note that `System.out` is `PrintStream`, not `PrintWriter`. The wrapper allows using the same interface for console and files.

---

[← Previous](concurrency.en.md) · [Next →](streams-lambdas.en.md)  
[🇪🇸 Español](io-networking.md) · [🇬🇧 English](io-networking.en.md)
