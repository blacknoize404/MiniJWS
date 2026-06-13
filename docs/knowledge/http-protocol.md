# Cómo funciona HTTP (y cómo lo implementé en MiniJWS)

Cuando empecé este proyecto, mi relación con HTTP era la de cualquier usuario: sabía que existía, escribía URLs en el navegador y aparecían páginas. Pero ¿qué pasa realmente entre que pulsas Enter y ves el resultado? Eso es lo que fui descubriendo mientras escribía MiniJWS, y esto es lo que aprendí.

HTTP es un protocolo de texto. Esto quiere decir que lo que viaja por la red son cadenas de caracteres que cualquier persona puede leer. No hay magia ni binarios crípticos — solo texto con un formato muy estricto.

---

## 1. HTTP vive sobre TCP

Antes de que exista cualquier comunicación HTTP, tiene que haber una conexión TCP. Es como hacer una llamada telefónica: primero marcas, esperas que contesten, y una vez establecida la conexión, hablas. El "marcar" es el *three-way handshake* de TCP.

En Java, abrir un servidor TCP para aceptar llamadas HTTP se hace así:

```java
ServerSocket serverSocket = new ServerSocket(8080);

// Esto se queda bloqueado hasta que alguien se conecta
Socket cliente = serverSocket.accept();

InputStream  entrada = cliente.getInputStream();   // lo que el cliente envía
OutputStream salida  = cliente.getOutputStream();   // lo que el servidor responde
```

Ese `accept()` se queda esperando. Cuando un navegador (o curl, o cualquier cliente) se conecta al puerto 8080, `accept()` devuelve un `Socket`. Sobre ese socket, tanto el cliente como el servidor pueden leer y escribir bytes.

Lo primero que descubrí fue que HTTP es básicamente texto viajando por ese canal de bytes. La implementación más sencilla es leer bytes de a uno hasta encontrar `\r\n`, que es como HTTP marca el final de una línea. En el código de `HttpDecoder` se ve claramente:

```java
// readLine() busca \r\n byte por byte
private String readLine(BufferedInputStream in) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    int b;
    while ((b = in.read()) != -1) {
        if (b == '\r') {
            in.read(); // consume el \n que viene después
            break;
        }
        buffer.write(b);
    }
    return buffer.toString(StandardCharsets.UTF_8);
}
```

¿Por qué uso `BufferedInputStream` y no `BufferedReader`? Porque `BufferedReader.readLine()` no discrimina entre `\n` y `\r\n`, y HTTP exige estrictamente `\r\n`. Me pasé una tarde entera depurando peticiones fallidas hasta que entendí esto.

---

## 2. Anatomía de una solicitud HTTP

Cuando tu navegador quiere pedir una página, envía algo como esto:

```
GET /index.html HTTP/1.1
Host: localhost:8080
User-Agent: Mozilla/5.0
Accept: text/html

```

Fíjate en los detalles:

- **Cada línea termina en `\r\n`** — sí, dos caracteres: retorno de carro + nueva línea. En la representación de arriba no se ven, pero están.
- **La primera línea** se llama *request line* y tiene tres partes separadas por espacios: el método (`GET`), la URI (`/index.html`), y la versión del protocolo (`HTTP/1.1`).
- **Después vienen las cabeceras**, una por línea, con formato `Nombre: Valor`.
- **Una línea vacía** (`\r\n` solitario) marca el final de las cabeceras.
- **Opcionalmente, después viene el cuerpo** (en POST, PUT, etc.).

Lo que hace `HttpDecoder` es:

1. Lee la primera línea → extrae método, URI y versión
2. Sigue leyendo líneas hasta encontrarse una vacía → esas son las cabeceras
3. Si hay cabecera `Content-Length`, lee esa cantidad de bytes → ese es el cuerpo
4. Si hay `Transfer-Encoding: chunked`, lee los chunks hasta encontrar el chunk 0

Y con eso construye un `HttpRequest`. El código no es complejo, pero cada detalle cuenta. Por ejemplo, las cabeceras pueden repetirse. Una petición puede tener múltiples cabeceras `Accept`:

```
Accept: text/html
Accept: application/json
```

Por eso en `HttpRequest` las cabeceras se guardan como `Map<String, List<String>>`, no como `Map<String, String>`.

---

## 3. Los métodos HTTP (y cuáles implementé)

La primera palabra de la request line es el método. HTTP/1.1 define varios, aunque en MiniJWS soporto los más comunes:

| Método | Para qué sirve |
|--------|----------------|
| `GET` | Obtener un recurso. No tiene cuerpo (o no debería). |
| `POST` | Enviar datos al servidor para crear algo. |
| `PUT` | Reemplazar un recurso completo. |
| `PATCH` | Modificar parcialmente un recurso. |
| `DELETE` | Borrar un recurso. |
| `HEAD` | Igual que GET pero solo quiere las cabeceras, sin el cuerpo. |
| `OPTIONS` | Preguntar qué métodos acepta el servidor. Lo usa CORS para el preflight. |

Los métodos no son más que strings. En mi código los tengo como `enum HttpMethod` para tener type-safety, pero el decodificador los parsea como texto — `"GET"` → `HttpMethod.GET` — y el encoder los vuelve a escribir como texto en la respuesta.

---

## 4. Códigos de estado: el lenguaje de las respuestas

Cuando el servidor responde, lo primero que dice es un código de tres dígitos. Aprendértelos de memoria no tiene sentido, pero sí entender las familias:

| Familia | Significado | Ejemplos que te encontrarás |
|---------|-------------|----------------------------|
| **1xx** | Informativo | `100 Continue` — el servidor está de acuerdo con la cabecera `Expect` |
| **2xx** | Éxito | `200 OK` (todo bien), `201 Created` (se creó un recurso) |
| **3xx** | Redirección | `301 Moved Permanently` (la URL cambió), `302 Found` (redirección temporal) |
| **4xx** | Error del cliente | `400 Bad Request` (la petición está mal formada), `404 Not Found`, `429 Too Many Requests` |
| **5xx** | Error del servidor | `500 Internal Server Error` (algo explotó) |

En MiniJWS, cada respuesta lleva un `statusCode`. Cuando creas una respuesta usas `setStatusCode(200)`. La razón textual ("OK", "Not Found") la genera `HttpStatusCode` automáticamente a partir del número. Tener un enum con todos los códigos y sus frases fue tedioso de escribir, pero evita errores tontos.

---

## 5. Cabeceras: la negociación entre cliente y servidor

Las cabeceras son el mecanismo por el que cliente y servidor se ponen de acuerdo sobre cómo intercambiar los datos. Funcionan como pares `Clave: Valor` y van tanto en la solicitud como en la respuesta.

A lo largo del desarrollo de MiniJWS, estas son las cabeceras con las que más tuve que lidiar:

### `Content-Type`
Le dice al receptor qué tipo de dato se está enviando. Ejemplos:

```
Content-Type: text/html;charset=utf-8
Content-Type: application/json
Content-Type: image/png
```

En el servidor, cuando envías la respuesta, `HttpEncoder` usa esta cabecera para decidir cómo codificar el cuerpo. Si el content-type es texto (HTML, JSON, CSS...), escribe con un `OutputStreamWriter` en UTF-8. Si es binario (imágenes, vídeos...), escribe los bytes directamente.

### `Content-Length`
Indica cuántos bytes tiene el cuerpo. Es la forma más simple de delimitar el final de un mensaje:

```
Content-Length: 45

{"status":"ok","users":[]}
```

El receptor lee exactamente 45 bytes y sabe que el mensaje terminó. Sencillo, ¿verdad? Pero tiene un problema: no puedes empezar a enviar la respuesta si no sabes cuánto va a medir. Para contenido dinámico, HTTP tiene otra solución...

### `Transfer-Encoding: chunked`

Cuando no sabes el tamaño de antemano (por ejemplo, estás generando HTML sobre la marcha), troceas el cuerpo en chunks:

```
Transfer-Encoding: chunked

1a
{"status":"ok","users":[]}
0

```

Cada chunk empieza con su tamaño en hexadecimal (`1a` = 26 bytes), luego los datos, luego `\r\n`. El chunk `0` marca el final. Implementar esto fue interesante porque tuve que manejar un flujo donde no sabes cuándo vas a terminar de escribir.

### `Host` (obligatoria en HTTP/1.1)
Desde HTTP/1.1, la cabecera `Host` es obligatoria. Permite que un mismo servidor sirva múltiples sitios web en la misma IP (virtual hosting). En MiniJWS no la uso activamente porque el servidor es para aprendizaje, pero la parseo y la almaceno igual.

### `Connection`
Controla si la conexión TCP se reutiliza:

```
Connection: keep-alive   → reutilizar (comportamiento por defecto en HTTP/1.1)
Connection: close        → cerrar después de esta respuesta
```

### `Cookie` y `Set-Cookie`
Las cookies son cabeceras con un formato peculiar. El navegador envía las cookies almacenadas en la cabecera `Cookie`:

```
Cookie: session=abc123; token=xyz
```

Y el servidor las establece con `Set-Cookie`:

```
Set-Cookie: session=abc123; Max-Age=3600; Path=/; HttpOnly
```

Parsear `Cookie` fue un dolor de cabeza porque el formato permite varias cookies en una misma cabecera separadas por `;`. Además cada cookie puede tener atributos opcionales como `Max-Age`, `Path`, `Domain`, `Secure` e `HttpOnly`. Mi implementación es básica pero funcional.

### `Cache-Control`
El servidor puede decirle al navegador cómo cachear la respuesta:

```
Cache-Control: no-cache
Cache-Control: max-age=3600
```

En MiniJWS no implemento lógica de caching, pero el encoder respeta las cabeceras que pongas.

---

## 6. Cómo empieza y termina un mensaje HTTP

Esto parece trivial, pero fue uno de los problemas más difíciles de resolver: ¿cómo sabe el servidor dónde termina una petición y empieza la siguiente?

En HTTP/1.1 hay cuatro formas de delimitar el mensaje, y cada una la implementé de manera distinta:

1. **`Content-Length`** — leo N bytes y ya. Simple y elegante.
2. **`Transfer-Encoding: chunked`** — leo chunks hasta encontrar el de tamaño 0.
3. **Conexión `close`** — leo hasta que el socket se cierra. No es eficiente pero es la única opción cuando no hay ni `Content-Length` ni `chunked`.
4. **Mensaje sin cuerpo** — los GET y HEAD no tienen cuerpo. Después de la línea vacía, el mensaje terminó.

Me costó encontrar un error donde el servidor se quedaba colgado porque el cliente enviaba `Transfer-Encoding: chunked` pero la implementación esperaba `Content-Length`. La lección: el servidor no puede asumir nada, tiene que inspeccionar las cabeceras para decidir cómo leer el cuerpo.

---

## 7. Keep-Alive: por qué HTTP/1.1 es más rápido

En HTTP/1.0, cada petición abría una nueva conexión TCP. El proceso era:

1. Abrir conexión TCP
2. Enviar GET /index.html
3. Recibir respuesta
4. Cerrar conexión
5. Repetir para GET /style.css
6. Repetir para GET /script.js
7. Repetir para GET /logo.png...

Una página con 10 recursos implicaba 10 conexiones TCP. Cada una requería su *three-way handshake*. Mucho tiempo perdido en establecer conexiones.

HTTP/1.1 introdujo las conexiones persistentes: el socket se reutiliza para varias peticiones. En MiniJWS, el `ConnectionHandler` hace esto:

```java
int requests = 0;
boolean keepAlive = true;

while (keepAlive && requests < MAX_KEEPALIVE_REQUESTS) {
    Optional<HttpRequest> request = HttpDecoder.decode(bufferedInput);
    if (request.isEmpty()) break;

    HttpResponse response = processRequest(request.get());
    HttpEncoder.sendResponse(response, output);

    keepAlive = isKeepAlive(request.get());
    requests++;
}
```

¿Y si el cliente envía 5 peticiones y luego se queda callado 11 segundos? El bucle se queda bloqueado en `HttpDecoder.decode()` esperando más datos. Para eso está el timeout:

```java
cliente.setSoTimeout(10000); // 10 segundos, después salta SocketTimeoutException
```

Cuando salta el timeout, cerramos la conexión. Esto me enseñó que los timeouts no son opcionales — sin ellos, una conexión podría quedar colgada para siempre.

---

## 8. Redirecciones

Una redirección es una respuesta con código 3xx y una cabecera `Location`:

```
HTTP/1.1 302 Found
Location: /nueva-pagina
```

En MiniJWS, `HttpResponse.redirect("/nueva-url")` es un *factory method* que construye esa respuesta por ti. Puedes pasarle 301 (permanente) o 302 (temporal) según el caso:

```java
HttpResponse.redirect("/login");        // 302 Found (temporal)
HttpResponse.redirect("/nuevo", 301);   // 301 Moved Permanently
```

La diferencia práctica: si es 301, los navegadores cachean la redirección y la próxima vez ni preguntan, van directo a la nueva URL. Con 302, siempre preguntan al servidor original primero.

---

## 9. Cookies

Las cookies permiten que el servidor guarde datos en el navegador. El ciclo es:

1. El servidor responde con `Set-Cookie: session=abc123`
2. El navegador guarda la cookie
3. En siguientes peticiones, el navegador envía `Cookie: session=abc123`

Mi `HttpRequest` parsea la cabecera `Cookie` en un `Map<String, String>`:

```java
request.getCookies().get("session"); // → "abc123"
```

Y el builder de `HttpResponse` permite establecer cookies:

```java
new HttpResponse.Builder()
    .setCookie("session", "abc123")                         // cookie simple
    .setCookie("token", "xyz", 3600, "/", true)             // con Max-Age, Path, HttpOnly
    .build();
```

El último booleano es `HttpOnly`: si es true, el JavaScript del navegador no puede acceder a la cookie. Es una medida de seguridad contra XSS.

---

## 10. CORS (Cross-Origin Resource Sharing)

CORS no es parte del protocolo HTTP base, pero aparece cuando haces aplicaciones web con frontend y backend separados. Sin CORS, un servidor en `http://localhost:8080` no puede recibir peticiones desde `http://127.0.0.1:5500` (o cualquier otro origen).

El navegador, antes de permitir la petición real, envía una petición `OPTIONS` (preflight) preguntando:

```
OPTIONS /api/data HTTP/1.1
Origin: http://127.0.0.1:5500
Access-Control-Request-Method: POST
```

El servidor debe responder con qué orígenes, métodos y cabeceras permite:

```
HTTP/1.1 204 No Content
Access-Control-Allow-Origin: http://127.0.0.1:5500
Access-Control-Allow-Methods: POST, GET, OPTIONS
Access-Control-Allow-Headers: Content-Type
Access-Control-Max-Age: 3600
```

Implementé `CorsMiddleware` como un middleware más. Se coloca en la cadena y, si detecta una petición `OPTIONS` con cabecera `Origin`, responde automáticamente con los permisos configurados.

Detalle importante: si usas `allowOrigin("*")` no puedes usar `allowCredentials(true)` al mismo tiempo. El estándar lo prohíbe explícitamente — si permites cualquier origen, no puedes enviar credenciales (cookies, auth headers). El middleware lanza una excepción si detectas esta combinación.

---

## 11. Content Negotiation (lo básico)

El cliente puede decirle al servidor qué tipo de respuesta prefiere:

```
Accept: text/html, application/json;q=0.9, */*;q=0.8
```

El `q` es la calidad (0.0 a 1.0). En MiniJWS no implemento negociación de contenido automática — el servidor devuelve siempre el tipo que el handler decida. Pero el decodificador parsea correctamente la cabecera `Accept`.

Lo mismo con `Accept-Encoding`:

```
Accept-Encoding: gzip, deflate
```

El `GzipMiddleware` comprueba esta cabecera. Si el cliente acepta gzip, el middleware comprime el cuerpo y añade `Content-Encoding: gzip` a la respuesta. Si no, pasa la respuesta sin comprimir.

---

## 12. Versiones de HTTP

HTTP ha evolucionado. En MiniJWS implementé HTTP/1.1 porque es la versión que mejor equilibrio ofrece entre complejidad de implementación y utilidad real:

| Versión | Novedades principales |
|---------|----------------------|
| **HTTP/0.9** | Solo GET, sin cabeceras, sin códigos de estado. Era básicamente: "dame esta página" → te la mando. |
| **HTTP/1.0** | Añadió cabeceras, códigos de estado, POST, Content-Type. Cada petición abre y cierra conexión. |
| **HTTP/1.1** | Conexiones persistentes, chunked transfer, cabecera Host obligatoria, más métodos. Es lo que implementa MiniJWS. |
| **HTTP/2** | Binario (no texto), multiplexación de peticiones por una sola conexión TCP, compresión de cabeceras. No es compatible con HTTP/1.1 a nivel de bytes — es otro protocolo. |
| **HTTP/3** | Usa QUIC (sobre UDP) en vez de TCP. Reduce la latencia de conexión a casi cero. |

Si estás estudiando HTTP, te recomiendo dominar bien HTTP/1.1 antes de mirar HTTP/2 o 3. Los conceptos (petición, respuesta, cabeceras, estado) son los mismos, y entender cómo se hacían las cosas en 1.1 te da perspectiva para apreciar las optimizaciones de las versiones posteriores.

---

## 13. HTTPS y por qué MiniJWS no lo tiene

HTTPS es HTTP sobre TLS (antes SSL). Los mensajes HTTP son exactamente los mismos, pero viajan cifrados. Para implementar HTTPS en Java necesitas un `SSLServerSocket` en lugar de un `ServerSocket`, y gestionar certificados.

No lo he implementado porque:
- Añade complejidad (gestión de certificados, keystores, cifrado) que distrae del objetivo de aprender HTTP
- Para producción, lo recomendable es poner un proxy inverso (nginx, Apache) con TLS delante de MiniJWS
- `SSLServerSocket` en Java funciona, pero configurarlo bien requiere conocimientos de seguridad que escapan del alcance educativo del proyecto

Si quieres añadirlo, la estructura del servidor lo permite: solo cambiaría cómo se acepta la conexión inicial.

---

## 14. Mapeo MIME: de extensión a Content-Type

El servidor necesita saber qué `Content-Type` asignar a cada archivo. Por ejemplo, `style.css` → `text/css`, `script.js` → `text/javascript`. En MiniJWS tengo `ContentTypes.EXTENSION_TO_MIME`, que es un `Map<String, String>` con las asociaciones:

```
"html" → "text/html;charset=utf-8"
"json" → "application/json;charset=utf-8"
"png"  → "image/png"
"apk"  → "application/vnd.android.package-archive"
...
```

El `StaticFileHandler` usa este mapa cuando sirve archivos. Si la extensión no está mapeada, devuelve `application/octet-stream` (binario genérico).

---

## 15. Lo que aprendí en el proceso

Puede sonar a frase hecha, pero implementar HTTP desde cero cambió mi forma de entender la web. Cosas que antes daba por sentadas ("poner Content-Type, poner status code...") ahora sé exactamente qué hacen y por qué están ahí.

Los conceptos universitarios que aparecen en este proyecto:

- **TCP/IP y Sockets** — cómo se conectan dos máquinas en una red
- **Protocolos de texto** — cómo se estructuran mensajes legibles sobre bytes
- **Máquinas de estado** — el decodificador HTTP es básicamente una máquina de estados que pasa de "leyendo línea de solicitud" a "leyendo cabeceras" a "leyendo cuerpo"
- **Concurrencia con pool de hilos** — cómo atender N clientes con M hilos
- **Patrones de diseño** — Builder, Chain of Responsibility, Strategy, Producer-Consumer
- **Seguridad** — path traversal, rate limiting, CORS, HttpOnly cookies
- **Cacheo y rendimiento** — keep-alive, compression, redirecciones 301 vs 302

Si estás estudiando informática y quieres entender de verdad cómo funciona la web, te recomiendo hacer tu propio servidor HTTP aunque sea mínimo. No hace falta que tengas todas las funcionalidades que tiene MiniJWS — con un servidor que sirva un HTML ya aprendes más que leyendo tres capítulos de cualquier libro.

---

[← Anterior](index.md) · [Siguiente →](patterns.md)  
[🇪🇸 Español](http-protocol.md) · [🇬🇧 English](http-protocol.en.md)
