# API HttpRequest

`io.github.blacknoize404.miniJWS.requests.HttpRequest`

Modelo de solicitud HTTP inmutable construido mediante el patrón Builder.

## Propiedades

| Propiedad | Tipo | Descripción |
|-----------|------|-------------|
| `httpMethod` | `HttpMethod` | GET, POST, PUT, DELETE, etc. |
| `uri` | `URI` | URI de la solicitud (solo ruta, sin query) |
| `protocolVersion` | `String` | p.ej. `HTTP/1.1` |
| `headers` | `Map<String, List<String>>` | Cabeceras de solicitud (multi-valor) |
| `parameters` | `Map<String, String>` | Query string + parámetros de ruta |
| `cookies` | `Map<String, String>` | Parseado de la cabecera Cookie |
| `body` | `Optional<byte[]>` | Cuerpo de la solicitud |

## Métodos de Acceso

| Método | Retorna | Descripción |
|--------|---------|-------------|
| `getUri()` | `URI` | URI de la solicitud |
| `getHttpMethod()` | `HttpMethod` | Método HTTP |
| `getHeaders()` | `Map<String, List<String>>` | Todas las cabeceras |
| `getParameters()` | `Map<String, String>` | Parámetros de query + ruta |
| `getCookies()` | `Map<String, String>` | Cookies de la solicitud |
| `getProtocolVersion()` | `String` | Versión del protocolo |
| `getBody()` | `Optional<byte[]>` | Bytes crudos del cuerpo |
| `getHeader(String)` | `Optional<String>` | Valor de una cabecera |

## Parseo del Cuerpo

| Método | Retorna | Descripción |
|--------|---------|-------------|
| `bodyAsString()` | `Optional<String>` | Cuerpo como String UTF-8 |
| `bodyAsForm()` | `Optional<Map<String, String>>` | Parsea `application/x-www-form-urlencoded` |
| `bodyAsJson()` | `Optional<Map<String, String>>` | Parsea objeto JSON plano |

## Métodos del Builder

| Método | Descripción |
|--------|-------------|
| `setHttpMethod(HttpMethod)` | Establecer el método HTTP |
| `setUri(URI)` | Establecer la URI de solicitud |
| `setProtocolVersion(String)` | Establecer versión del protocolo |
| `setHeaders(Map)` | Establecer todas las cabeceras |
| `addHeader(String, List<String>)` | Añadir una cabecera |
| `setParameters(Map)` | Establecer parámetros de query/ruta |
| `addParameter(String, String)` | Añadir un parámetro |
| `setBody(byte[])` | Establecer el cuerpo de la solicitud |
| `build()` | Construir el HttpRequest |

## HttpDecoder

```java
HttpDecoder.decode(InputStream)              → Optional<HttpRequest>
HttpDecoder.decode(BufferedInputStream)       → Optional<HttpRequest>
```

Parsea HTTP crudo desde un InputStream. Gestiona:
- Línea de solicitud (método, URI, protocolo)
- Cabeceras multi-valor
- Extracción de parámetros de query
- Cuerpo con Content-Length
- Transferencia codificada chunked
- Cabecera Cookie

---

[← Anterior](http-server.md) · [Siguiente →](http-response.md)  
[🇪🇸 Español](http-request.md) · [🇬🇧 English](http-request.en.md)
