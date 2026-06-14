# Clases de Soporte y Otros Módulos

## Enum ContentType

**Archivo:** `miniJWS-core/src/main/java/.../primitives/ContentType.java`

Mapea tipos MIME a un enum con búsqueda por extensión.

### Valores del Enum

| Constante | Tipo MIME                        |
|-----------|----------------------------------|
| `FILE`    | `application/octet-stream`       |
| `JSON`    | `application/json;charset=utf-8` |
| `CSS`     | `text/css;charset=utf-8`         |
| `JS`      | `text/javascript;charset=utf-8`  |
| `HTML`    | `text/html;charset=utf-8`        |
| `XML`     | `text/xml;charset=utf-8`         |
| `TEXT`    | `text/plain;charset=utf-8`       |
| `SVG`     | `image/svg+xml`                  |
| `ICO`     | `image/x-icon`                   |
| `PNG`     | `image/png`                      |
| `JPEG`    | `image/jpeg`                     |
| `GIF`     | `image/gif`                      |
| `WEBP`    | `image/webp`                     |
| `MP4`     | `video/mp4`                      |
| `WEBM`    | `video/webm`                     |
| `WOFF2`   | `font/woff2`                     |
| `TTF`     | `font/ttf`                       |
| `PDF`     | `application/pdf`                |
| `ZIP`     | `application/zip`                |

### EXT_MAP (Alias de Extensiones)

```java
EXT_MAP.put("jpg", JPEG);    // el enum es "JPEG", la extensión "jpg"
EXT_MAP.put("jpeg", JPEG);
EXT_MAP.put("tiff", FILE);   // no hay enum TIFF, mapear a binario genérico
```

Construido desde `name().toLowerCase()` para todos los valores del enum, luego sobrescrituras para alias.

### Métodos Clave

```java
// Búsqueda por extensión de archivo
ContentType.fromExtension("html") → Optional(HTML)
ContentType.fromExtension("jpg")  → Optional(JPEG)

// Búsqueda inversa por cadena MIME
ContentType.fromMime("image/jpeg") → JPEG
```

---

## Enum HttpMethod

**Archivo:** `miniJWS-core/src/main/java/.../primitives/HttpMethod.java`

Métodos HTTP estándar:

```java
public enum HttpMethod {
    GET, HEAD, POST, PUT, DELETE,
    CONNECT, OPTIONS, TRACE, PATCH
}
```

Usado como componente de clave de ruta (`method + ":" + path`) y en `HttpRequest`.

---

## HttpStatusCode

**Archivo:** `miniJWS-core/src/main/java/.../primitives/HttpStatusCode.java`

Mapeo estático de códigos de estado a frases de razón:

| Estado | Frase                 |
|--------|-----------------------|
| 100    | CONTINUE              |
| 200    | OK                    |
| 204    | NO_CONTENT            |
| 301    | MOVED_PERMANENTLY     |
| 302    | FOUND                 |
| 400    | BAD_REQUEST           |
| 401    | UNAUTHORIZED          |
| 403    | FORBIDDEN             |
| 404    | NOT_FOUND             |
| 429    | TOO_MANY_REQUESTS     |
| 500    | INTERNAL_SERVER_ERROR |
| ...    | ...                   |

Usado por `HttpEncoder` para escribir la línea de estado: `HTTP/1.1 200 OK\r\n`.

---

## Interfaz RequestRunner

**Archivo:** `miniJWS-core/src/main/java/.../primitives/RequestRunner.java`

```java
@FunctionalInterface
public interface RequestRunner {
    HttpResponse run(HttpRequest request);
}
```

Los manejadores de ruta son instancias de `RequestRunner`. Generalmente se expresan como lambdas:

```java
server.addRoute(HttpMethod.GET, "/", req ->
    new HttpResponse.Builder()
        .setStatusCode(200)
        .setBody("OK")
        .build()
);
```

---

## Interfaces Middleware y MiddlewareChain

**Archivos:**
- `miniJWS-core/src/main/java/.../primitives/Middleware.java`
- `miniJWS-core/src/main/java/.../primitives/MiddlewareChain.java`

```java
@FunctionalInterface
public interface Middleware {
    HttpResponse run(HttpRequest request, MiddlewareChain chain);
}

@FunctionalInterface
public interface MiddlewareChain {
    HttpResponse next(HttpRequest request);
}
```

`MiddlewareChain` es el "siguiente eslabón" de la cadena. Un middleware llama a `chain.next(request)` para pasar el control hacia abajo, modificando opcionalmente la petición o procesando la respuesta al regresar.

---

## ContentTypes (Extensión a MIME)

**Archivo:** `miniJWS-core/src/main/java/.../content/ContentTypes.java`

Clase legacy (incluida para compatibilidad hacia atrás). Define un `Map<String, String>` estático que mapea extensiones de archivo a tipos MIME. Se solapa con la funcionalidad del enum `ContentType` pero proporciona búsquedas directas basadas en cadenas.

---

## Clases de Parseo de Cabeceras

### Header
**Archivo:** `miniJWS-core/src/main/java/.../headers/Header.java`

Clase modelo para el parseo clave-valor de cabeceras HTTP.

### Field
**Archivo:** `miniJWS-core/src/main/java/.../headers/Field.java`

Parser para campos de cabecera estructurados (p.ej., `Content-Type: text/html; charset=utf-8`).

### Parameter
**Archivo:** `miniJWS-core/src/main/java/.../headers/Parameter.java`

Parser para parámetros de cabecera (las partes `clave=valor` después de `;`).

---

## Otros Módulos

### miniQR (Generador de Códigos QR)

**Paquete:** `io.github.blacknoize404.miniQR`

Usa ZXing para generación de QR y JFreeSVG para salida SVG.

| Método | Descripción |
|--------|-------------|
| `generateQRCodeImage(text, size)` | `BufferedImage` mediante `QRCodeWriter` |
| `convertToSVG(image, w, h)` | `BufferedImage` → cadena SVG mediante `SVGGraphics2D` |
| `generateSVG(text, size)` | Texto a SVG directo (combina los dos anteriores) |

Corrección de errores: `ErrorCorrectionLevel.L` (bajo — 7% de recuperación, máxima capacidad de datos).

**Dependencias:** `com.google.zxing:core:3.5.3`, `org.jfree:jfreesvg:3.4`

---

### miniStaticServer

**Paquete:** `io.github.blacknoize404.miniStaticServer`

Envuelve `HttpServer` para servir archivos estáticos por directorio con inyección de plantillas.

#### StaticSite

Escanea un directorio y añade rutas de archivo automáticamente. Soporta sustitución de plantillas `{{variable}}` en archivos HTML.

```java
StaticSite site = new StaticSite(8080, Path.of("data"));
site.addTemplate("serverIp", "192.168.1.100");
site.start();
site.idle();
```

#### QrStaticSite

Extiende el concepto del servidor de archivos estáticos con inyección de códigos QR. Reemplaza `{{placeholder}}` en HTML con elementos SVG de código QR en línea.

```java
QrStaticSite site = new QrStaticSite(80, Path.of("data"));
site.addQrPlaceholder("downloadQR", "https://example.com/app.apk", 250);
site.start();
```

**Método utilitario:** `QrStaticSite.getLocalIp()` — descubre la IP de red local mediante socket UDP a `8.8.8.8:12345`.

---

### miniApkReader

**Paquete:** `io.github.blacknoize404.miniApkReader`

Extracción de metadatos de APK Android.

#### Record ApkInfo

```java
public record ApkInfo(
    String packageName,       // com.example.app
    String versionName,       // 1.0.0
    long versionCode,         // 42
    String minSdkVersion,     // 26
    String targetSdkVersion,  // 33
    List<String> permissions, // [INTERNET, ...]
    List<String> features,    // [android.hardware.camera, ...]
    String label,             // "Mi App"
    String icon               // res/mipmap/ic_launcher.png
) {}
```

#### ApkReader

```java
ApkInfo info = ApkReader.read(Path.of("app.apk"));
String formatted = ApkReader.printInfo(info);
```

#### ApkInfoExtractor

Envoltorio CLI para `ApkReader.read()`:

```bash
mvn compile exec:java \
  -Dexec.mainClass="io.github.blacknoize404.miniApkReader.ApkInfoExtractor" \
  -Dexec.args="ruta/a/app.apk"
```

**Dependencia:** `net.dongliu:apk-parser:2.6.10`

---

### miniJWS-demo

**Paquete:** `io.github.blacknoize404.miniJWS.demo`

Servidor de demostración completo que ejercita todos los tipos de middleware y rutas. Ver [DemoServer.java](../../miniJWS-demo/src/main/java/io/github/blacknoize404/miniJWS/demo/DemoServer.java).

| Característica Demo | Descripción |
|---------------------|-------------|
| Middleware | AccessLog, CORS (`*`), RateLimit (200/60s) |
| Archivos estáticos | `./public` mediante `addStaticRoute("/*", "./public")` |
| Parámetros de ruta | `/hello/:name` |
| Parámetros de query | `/hello?name=X` |
| Parseo de cuerpo | `POST /api/data` con JSON y formulario |
| Cookies | `/set-cookie`, `/get-cookies` |
| Redirección | `/old-path` → 301 → `/new-path` |
| Echo | `/echo` muestra todos los detalles de la petición |
| Keep-alive | Por defecto (HTTP/1.1) |

---

[← Anterior](classes-middleware.md) · [Siguiente →](java-api/index.md)  
[🇪🇸 Español](classes-support.md) · [🇬🇧 English](classes-support.en.md)
