# I/O & Networking

## Streams

### InputStream — Lectura de bytes

Clase abstracta base para leer datos binarios.

```java
InputStream in = s.getInputStream();
```

**Métodos clave:**
| Método | Descripción |
|--------|-------------|
| `int read()` | Lee un byte (0-255) o -1 si EOF |
| `int read(byte[], off, len)` | Lee hasta `len` bytes en el buffer |
| `void close()` | Cierra el stream |

### OutputStream — Escritura de bytes

```java
OutputStream out = s.getOutputStream();
```

**Métodos clave:**
| Método | Descripción |
|--------|-------------|
| `void write(int)` | Escribe un byte |
| `void write(byte[])` | Escribe todo el array |
| `void write(byte[], off, len)` | Escribe `len` bytes desde `off` |
| `void flush()` | Fuerza escritura de bytes buffer |
| `void close()` | Cierra el stream |

### BufferedInputStream

Añade un buffer interno para reducir llamadas al sistema operativo.

```java
var reader = new BufferedInputStream(in);
```

**Métodos:**
- Hereda `read()` de `InputStream`
- `int available()` — bytes disponibles sin bloqueo (NO USAR para parsing HTTP — bug conocido)
- Internamente usa un buffer de 8KB por defecto

### BufferedOutputStream / BufferedWriter

Buffer para escritura eficiente.

```java
var writer = new BufferedWriter(
    new OutputStreamWriter(outputStream, StandardCharsets.US_ASCII));
```

### OutputStreamWriter

Puente entre caracteres (Writer) y bytes (OutputStream). Convierte caracteres a bytes usando un charset.

```java
new OutputStreamWriter(outputStream, StandardCharsets.US_ASCII)
```

### ByteArrayOutputStream

OutputStream que escribe en un buffer interno redimensionable (en memoria).

```java
var buf = new ByteArrayOutputStream(256);  // capacidad inicial 256
var body = new ByteArrayOutputStream();
var bos = new ByteArrayOutputStream(data.length / 2);
```

**Métodos:**
| Método | Descripción |
|--------|-------------|
| `write(int)` | Escribe un byte |
| `write(byte[])` | Escribe bytes |
| `toByteArray()` | Devuelve copia del buffer como `byte[]` |
| `size()` | Número de bytes escritos |
| `reset()` | Reinicia el buffer |

Se usa en:
- `HttpDecoder.readLine()` — acumula bytes hasta `\r\n`
- `HttpDecoder.readChunkedBody()` — ensambla chunks
- `GzipMiddleware.gzipCompress()` — buffer para datos comprimidos

---

## File I/O

### File

Clase legacy para representar rutas de archivo.

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

Representa una ruta (reemplaza a `File` para nuevas APIs).

```java
Path apkPath = Path.of(args[0]);  // Java 11+
Path resolved = baseDir.resolve(relative);
Path root = Path.of("data");
```

### Files (java.nio.file.Files)

Operaciones sobre archivos usando `Path`.

```java
byte[] data = Files.readAllBytes(file.toPath());
// Lee todo el archivo en memoria como byte[]
// Lanza IOException (NoSuchFileException si no existe)
```

**No usado pero relevante:**
- `Files.write(path, bytes)`
- `Files.newInputStream(path)`
- `Files.exists(path)`

### FileWriter

Writer que escribe directamente a un archivo.

```java
new FileWriter(filePath, StandardCharsets.UTF_8, true)  // append mode
```

### PrintWriter

Writer con métodos convenientes (`println`, `printf`, `format`). No lanza `IOException` internamente.

```java
new PrintWriter(new FileWriter(filePath, StandardCharsets.UTF_8, true), true)
// segundo parámetro: autoFlush — flush después de cada println
```

**Métodos usados:**
```java
writer.println(line);   // escribe línea + salto de línea
writer.flush();         // fuerza escritura
```

---

## Networking (TCP)

### Socket

Cliente TCP. Conecta a un servidor remoto.

```java
Socket client = socket.accept();  // en el servidor, acepta conexión entrante
```

**Métodos importantes:**
```java
client.getInputStream();           // InputStream para leer datos del cliente
client.getOutputStream();          // OutputStream para escribir al cliente
client.setSoTimeout(10_000);       // timeout en milisegundos para read()
client.close();                    // cierra la conexión
```

**Try-with-resources:**
```java
try (Socket s = client;
     InputStream in = s.getInputStream();
     OutputStream out = s.getOutputStream()) {
    // ... manejar conexión ...
}
```

Las variables en try-with-resources implementan `AutoCloseable` y se cierran automáticamente al salir del bloque.

### ServerSocket

Servidor TCP que escucha en un puerto y acepta conexiones entrantes.

```java
this.socket = new ServerSocket(port);
```

**Métodos clave:**
| Método | Descripción |
|--------|-------------|
| `accept()` | Bloquea hasta que llegue una conexión → `Socket` |
| `close()` | Cierra el socket |
| `getLocalPort()` | Puerto en el que escucha |

### DatagramSocket (UDP)

```java
try (var socket = new DatagramSocket()) {
    socket.connect(InetAddress.getByName("8.8.8.8"), 12345);
    return socket.getLocalAddress().getHostAddress();
}
```

Usado en `QrStaticSite.getLocalIp()` para descubrir la IP local de red. El truco: conectar un UDP socket a una IP externa hace que el SO determine la interfaz de red por la que saldrían los paquetes, y `getLocalAddress()` devuelve la IP de esa interfaz.

### InetAddress

Representa una dirección IP.

```java
InetAddress.getByName("8.8.8.8")  // resuelve hostname/IP
```

---

## URI & URL Decoding

### URI

```java
URI uri = new URI(path);  // parsea una URI (lanza URISyntaxException si es inválida)
uri.getRawPath();         // path sin decodificar (/hello/world)
```

### URLDecoder

Decodifica URLs (percent-encoded). Convierte `%20` a espacios, `%2B` a `+`, etc.

```java
URLDecoder.decode(kv[0], StandardCharsets.UTF_8)
```

**Uso:** Decodificar parámetros de query string y cuerpos form-urlencoded.

---

## Try-With-Resources

Cierra automáticamente recursos que implementan `AutoCloseable`.

```java
// Socket, InputStream, OutputStream, ApkFile, etc.
try (ApkFile apkFile = new ApkFile(apkPath.toFile())) {
    // usado y cerrado automáticamente
}

try (Socket s = client;
     InputStream in = s.getInputStream();
     OutputStream out = s.getOutputStream()) {
    // múltiples recursos separados por ;
}
```

Equivalente a:

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

## BufferedReader — Lectura de texto (NO usado, pero contexto)

`BufferedReader` se usa ocasionalmente para leer texto línea por línea. El proyecto implementa su propio `readLine()` byte-by-byte para controlar la codificación (US-ASCII para headers) y los límites de tamaño.

---

## java.util.zip — Compression

### GZIPOutputStream

OutputStream que comprime datos en formato GZIP.

```java
var bos = new ByteArrayOutputStream(data.length / 2);
var gz = new GZIPOutputStream(bos);
gz.write(data);
gz.close();  // importante: escribe el trailer (CRC32 + tamaño)
byte[] compressed = bos.toByteArray();
```

**Constructor:**
```java
GZIPOutputStream(OutputStream out)  // nivel de compresión por defecto (Deflater.DEFAULT_COMPRESSION)
```

**Uso en GzipMiddleware con nivel personalizado:**

```java
var gz = new GZIPOutputStream(bos) {{
    def.setLevel(level);
}};
```

Esto es **double brace initialization**: la primera llave crea una anonymous subclass de `GZIPOutputStream`, la segunda es un instance initializer. Accede al campo `protected Deflater def` de `DeflaterOutputStream` (superclase) para cambiar el nivel de compresión.

### Deflater

Compresor de datos que implementa LZ77 + Huffman coding (deflate algorithm).

```java
Deflater def = new Deflater(level);  // 1-9, 6=default
```

En GzipMiddleware se accede al `Deflater` interno del `GZIPOutputStream`:
```java
// def es un campo protected de DeflaterOutputStream
// accesible desde una anonymous subclass
def.setLevel(level);
```

**Niveles:**
| Nivel | Nombre | Velocidad | Compresión |
|-------|--------|-----------|------------|
| 1 | `BEST_SPEED` | Más rápida | Mínima |
| 6 | `DEFAULT_COMPRESSION` | Balance | Buena |
| 9 | `BEST_COMPRESSION` | Más lenta | Máxima |

### GZIPOutputStream — try-finally

```java
try {
    gz.write(data);
} finally {
    gz.close();  // asegura escribir el trailer incluso si write() falla
}
```

El `close()` escribe el trailer GZIP (CRC32 checksum + tamaño original). Sin él, el stream comprimido está incompleto.

---

## System I/O

```java
System.out.println("Hello");     // PrintStream — salida estándar
System.err.println("Error");     // PrintStream — salida de error
System.in                         // InputStream — entrada estándar
```

`System.out` se pasa como `PrintWriter` en `AccessLogMiddleware`:

```java
new PrintWriter(System.out, true)
```

Notar que `System.out` es `PrintStream`, no `PrintWriter`. El wrapper permite usar la misma interfaz para consola y archivos.

---

[← Anterior](concurrency.md) · [Siguiente →](streams-lambdas.md)  
[🇪🇸 Español](io-networking.md)
