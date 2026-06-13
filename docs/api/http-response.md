# API HttpResponse

`io.github.blacknoize404.miniJWS.responses.HttpResponse`

Modelo de respuesta HTTP inmutable construido mediante el patrón Builder.

## Propiedades

| Propiedad | Tipo | Descripción |
|-----------|------|-------------|
| `statusCode` | `int` | Código de estado HTTP (200, 404, etc.) |
| `protocolVersion` | `String` | p.ej. `HTTP/1.1` |
| `method` | `HttpMethod` | Método de la solicitud original |
| `headers` | `Map<String, List<String>>` | Cabeceras de respuesta |
| `body` | `Optional<byte[]>` | Cuerpo de la respuesta |

## Métodos Fábrica Estáticos

| Método | Retorna | Descripción |
|--------|---------|-------------|
| `redirect(String)` | `HttpResponse` | Redirección 302 a una ubicación |
| `redirect(String, int)` | `HttpResponse` | Redirección con estado personalizado (301, 302, etc.) |

## Métodos del Builder

| Método | Descripción |
|--------|-------------|
| `setStatusCode(int)` | Establecer código de estado HTTP |
| `setProtocolVersion(String)` | Establecer versión del protocolo |
| `setMethod(HttpMethod)` | Establecer método HTTP |
| `setContentType(ContentType)` | Establecer Content-Type desde enum |
| `setContentType(String)` | Establecer Content-Type como MIME crudo |
| `addHeader(String, String)` | Añadir cabecera de un solo valor |
| `addHeader(String, List<String>)` | Añadir cabecera multi-valor |
| `setCookie(String, String)` | Establecer cookie simple |
| `setCookie(String, String, int, String, boolean)` | Establecer cookie con Max-Age, Path, HttpOnly |
| `setBody(String)` | Establecer cuerpo desde String UTF-8 |
| `setBody(byte[])` | Establecer cuerpo desde bytes crudos |
| `build()` | Construir el HttpResponse |

## Ejemplo

```java
new HttpResponse.Builder()
    .setStatusCode(200)
    .setContentType(ContentType.JSON)
    .setCookie("session", "abc123", 3600, "/", true)
    .setBody("{\"status\":\"ok\"}")
    .build();

// Redirección
HttpResponse.redirect("/login");
HttpResponse.redirect("/new-url", 301);
```

## Cabeceras por Defecto

Toda respuesta incluye automáticamente:
- `Server: MiniJWS`
- `Date: <marca de tiempo RFC 1123>`

## HttpEncoder

`HttpEncoder.sendResponse(HttpResponse, OutputStream)`

Serializa una respuesta a un flujo de salida. Gestiona:
- Línea de estado
- Cabeceras (incluyendo Connection: keep-alive)
- Cálculo de Content-Length
- Codificación de texto vs binario según Content-Type

---

[← Anterior](http-request.md) · [Siguiente →](routing.md)  
[🇪🇸 Español](http-response.md) · [🇬🇧 English](http-response.en.md)
