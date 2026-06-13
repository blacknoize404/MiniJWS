# How HTTP Works (and How I Built It in MiniJWS)

When I started this project, my relationship with HTTP was like any other user's: I knew it existed, I typed URLs in the browser, and pages appeared. But what actually happens between pressing Enter and seeing the result? That's what I discovered while writing MiniJWS, and this is what I learned.

HTTP is a text-based protocol. What travels over the network are character strings that anyone can read. No magic, no cryptic binaries — just text with a very strict format.

---

## 1. HTTP runs on top of TCP

Before any HTTP communication can happen, there must be a TCP connection. It's like making a phone call: first you dial, you wait for someone to pick up, and once the connection is established, you talk. The "dialing" is TCP's three-way handshake.

In Java, opening a TCP server to accept HTTP calls looks like this:

```java
ServerSocket serverSocket = new ServerSocket(8080);

// This blocks until someone connects
Socket client = serverSocket.accept();

InputStream  input  = client.getInputStream();   // what the client sends
OutputStream output = client.getOutputStream();   // what the server responds
```

That `accept()` just waits. When a browser (or curl, or any client) connects to port 8080, `accept()` returns a `Socket`. Over that socket, both the client and server can read and write bytes.

The first thing I figured out was that HTTP is basically text traveling over that byte channel. The simplest implementation is reading bytes one at a time until you find `\r\n`, which is how HTTP marks the end of a line. It's clear in the `HttpDecoder` code:

```java
// readLine() looks for \r\n byte by byte
private String readLine(BufferedInputStream in) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    int b;
    while ((b = in.read()) != -1) {
        if (b == '\r') {
            in.read(); // consume the \n that follows
            break;
        }
        buffer.write(b);
    }
    return buffer.toString(StandardCharsets.UTF_8);
}
```

Why `BufferedInputStream` and not `BufferedReader`? Because `BufferedReader.readLine()` doesn't distinguish between `\n` and `\r\n`, and HTTP strictly requires `\r\n`. I spent an entire afternoon debugging failed requests until I understood this.

---

## 2. Anatomy of an HTTP request

When your browser wants to request a page, it sends something like this:

```
GET /index.html HTTP/1.1
Host: localhost:8080
User-Agent: Mozilla/5.0
Accept: text/html

```

Pay attention to the details:

- **Every line ends with `\r\n`** — two characters: carriage return + newline. You can't see them above, but they're there.
- **The first line** is called the *request line* and has three parts separated by spaces: the method (`GET`), the URI (`/index.html`), and the protocol version (`HTTP/1.1`).
- **Then come the headers**, one per line, in `Name: Value` format.
- **An empty line** (a lone `\r\n`) marks the end of the headers.
- **Optionally, the body follows** (for POST, PUT, etc.).

What `HttpDecoder` does:

1. Reads the first line → extracts method, URI, and version
2. Keeps reading lines until it finds an empty one → those are the headers
3. If there's a `Content-Length` header, reads that many bytes → that's the body
4. If there's `Transfer-Encoding: chunked`, reads chunks until it finds chunk 0

And with that, it builds an `HttpRequest`. The code isn't complex, but every detail matters. For example, headers can repeat. A single request can have multiple `Accept` headers:

```
Accept: text/html
Accept: application/json
```

That's why in `HttpRequest` headers are stored as `Map<String, List<String>>`, not `Map<String, String>`.

---

## 3. HTTP methods (and which ones I implemented)

The first word of the request line is the method. HTTP/1.1 defines several; in MiniJWS I support the most common:

| Method | What it's for |
|--------|---------------|
| `GET` | Retrieve a resource. No body (or shouldn't have one). |
| `POST` | Send data to the server to create something. |
| `PUT` | Replace an entire resource. |
| `PATCH` | Partially modify a resource. |
| `DELETE` | Delete a resource. |
| `HEAD` | Same as GET but only wants headers, no body. |
| `OPTIONS` | Ask what methods the server accepts. Used by CORS for preflight. |

Methods are just strings. In my code I have them as `enum HttpMethod` for type safety, but the decoder parses them as text — `"GET"` → `HttpMethod.GET` — and the encoder writes them back as text in the response.

---

## 4. Status codes: the language of responses

When the server responds, the first thing it says is a three-digit code. Memorizing them all isn't useful, but understanding the families is:

| Family | Meaning | Examples you'll encounter |
|--------|---------|--------------------------|
| **1xx** | Informational | `100 Continue` — server agrees with the `Expect` header |
| **2xx** | Success | `200 OK` (all good), `201 Created` (resource created) |
| **3xx** | Redirection | `301 Moved Permanently` (URL changed), `302 Found` (temporary redirect) |
| **4xx** | Client error | `400 Bad Request` (malformed request), `404 Not Found`, `429 Too Many Requests` |
| **5xx** | Server error | `500 Internal Server Error` (something exploded) |

In MiniJWS, every response carries a `statusCode`. When you create a response you use `setStatusCode(200)`. The text reason ("OK", "Not Found") is generated automatically by `HttpStatusCode` from the number. Having an enum with all status codes and their phrases was tedious to write but prevents silly mistakes.

---

## 5. Headers: the negotiation between client and server

Headers are the mechanism by which client and server agree on how to exchange data. They work as `Key: Value` pairs and appear in both requests and responses.

During MiniJWS development, these are the headers I had to deal with the most:

### `Content-Type`
Tells the receiver what type of data is being sent. Examples:

```
Content-Type: text/html;charset=utf-8
Content-Type: application/json
Content-Type: image/png
```

On the server side, when you send the response, `HttpEncoder` uses this header to decide how to encode the body. If the content type is text (HTML, JSON, CSS...), it writes using an `OutputStreamWriter` in UTF-8. If it's binary (images, videos...), it writes the bytes directly.

### `Content-Length`
Tells how many bytes the body has. It's the simplest way to delimit the end of a message:

```
Content-Length: 45

{"status":"ok","users":[]}
```

The receiver reads exactly 45 bytes and knows the message ended. Simple, right? But it has a problem: you can't start sending the response if you don't know its size. For dynamic content, HTTP has another solution...

### `Transfer-Encoding: chunked`

When you don't know the size in advance (e.g., you're generating HTML on the fly), you split the body into chunks:

```
Transfer-Encoding: chunked

1a
{"status":"ok","users":[]}
0

```

Each chunk starts with its size in hex (`1a` = 26 bytes), then the data, then `\r\n`. The `0` chunk marks the end. Implementing this was interesting because I had to handle a stream where you don't know when you'll finish writing.

### `Host` (mandatory in HTTP/1.1)
Since HTTP/1.1, the `Host` header is mandatory. It allows a single server to serve multiple websites on the same IP (virtual hosting). In MiniJWS I don't actively use it since this server is for learning, but I parse and store it anyway.

### `Connection`
Controls whether the TCP connection gets reused:

```
Connection: keep-alive   → reuse (default behavior in HTTP/1.1)
Connection: close        → close after this response
```

### `Cookie` and `Set-Cookie`
Cookies are headers with a peculiar format. The browser sends stored cookies in the `Cookie` header:

```
Cookie: session=abc123; token=xyz
```

And the server sets them with `Set-Cookie`:

```
Set-Cookie: session=abc123; Max-Age=3600; Path=/; HttpOnly
```

Parsing `Cookie` was a headache because the format allows multiple cookies in a single header separated by `;`. Plus each cookie can have optional attributes like `Max-Age`, `Path`, `Domain`, `Secure`, and `HttpOnly`. My implementation is basic but functional.

### `Cache-Control`
The server can tell the browser how to cache the response:

```
Cache-Control: no-cache
Cache-Control: max-age=3600
```

In MiniJWS I don't implement caching logic, but the encoder respects whatever headers you set.

---

## 6. How an HTTP message starts and ends

This seems trivial, but it was one of the hardest problems to solve: how does the server know where one request ends and the next one begins?

In HTTP/1.1 there are four ways to delimit a message, and I implemented each differently:

1. **`Content-Length`** — I read N bytes and done. Simple and clean.
2. **`Transfer-Encoding: chunked`** — I read chunks until I find the size-0 one.
3. **Connection `close`** — I read until the socket closes. Not efficient but the only option when there's neither `Content-Length` nor `chunked`.
4. **Bodyless message** — GET and HEAD have no body. After the empty line, the message is done.

I once spent hours debugging a hanging server because the client sent `Transfer-Encoding: chunked` but my implementation expected `Content-Length`. The lesson: the server can't assume anything — it has to inspect the headers to decide how to read the body.

---

## 7. Keep-Alive: why HTTP/1.1 is faster

In HTTP/1.0, each request opened a new TCP connection. The process was:

1. Open TCP connection
2. Send GET /index.html
3. Receive response
4. Close connection
5. Repeat for GET /style.css
6. Repeat for GET /script.js
7. Repeat for GET /logo.png...

A page with 10 resources meant 10 TCP connections. Each one required its own three-way handshake. A lot of time wasted just establishing connections.

HTTP/1.1 introduced persistent connections: the socket gets reused for multiple requests. In MiniJWS, the `ConnectionHandler` does this:

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

What if the client sends 5 requests and then stays quiet for 11 seconds? The loop blocks on `HttpDecoder.decode()` waiting for more data. That's why there's a timeout:

```java
client.setSoTimeout(10000); // 10 seconds, then SocketTimeoutException fires
```

When the timeout fires, we close the connection. This taught me that timeouts aren't optional — without them, a connection could hang forever.

---

## 8. Redirects

A redirect is a response with a 3xx status code and a `Location` header:

```
HTTP/1.1 302 Found
Location: /new-page
```

In MiniJWS, `HttpResponse.redirect("/new-url")` is a factory method that builds that response for you. You can pass 301 (permanent) or 302 (temporary):

```java
HttpResponse.redirect("/login");        // 302 Found (temporary)
HttpResponse.redirect("/new", 301);     // 301 Moved Permanently
```

The practical difference: with 301, browsers cache the redirect and next time they go straight to the new URL. With 302, they always ask the original server first.

---

## 9. Cookies

Cookies let the server store data in the browser. The cycle:

1. Server responds with `Set-Cookie: session=abc123`
2. Browser saves the cookie
3. On subsequent requests, browser sends `Cookie: session=abc123`

My `HttpRequest` parses the `Cookie` header into a `Map<String, String>`:

```java
request.getCookies().get("session"); // → "abc123"
```

And the `HttpResponse` builder lets you set cookies:

```java
new HttpResponse.Builder()
    .setCookie("session", "abc123")                         // simple cookie
    .setCookie("token", "xyz", 3600, "/", true)             // with Max-Age, Path, HttpOnly
    .build();
```

The last boolean is `HttpOnly`: if true, the browser's JavaScript can't access the cookie. It's a security measure against XSS.

---

## 10. CORS (Cross-Origin Resource Sharing)

CORS isn't part of the base HTTP protocol, but it shows up when you build web apps with separate frontend and backend. Without CORS, a server at `http://localhost:8080` can't receive requests from `http://127.0.0.1:5500` (or any other origin).

Before allowing the actual request, the browser sends an `OPTIONS` preflight:

```
OPTIONS /api/data HTTP/1.1
Origin: http://127.0.0.1:5500
Access-Control-Request-Method: POST
```

The server must respond with which origins, methods, and headers it allows:

```
HTTP/1.1 204 No Content
Access-Control-Allow-Origin: http://127.0.0.1:5500
Access-Control-Allow-Methods: POST, GET, OPTIONS
Access-Control-Allow-Headers: Content-Type
Access-Control-Max-Age: 3600
```

I implemented `CorsMiddleware` as a regular middleware. It sits in the chain and, if it detects an `OPTIONS` request with an `Origin` header, automatically responds with the configured permissions.

Important detail: if you use `allowOrigin("*")` you can't use `allowCredentials(true)` at the same time. The standard explicitly forbids it — if you allow any origin, you can't send credentials (cookies, auth headers). The middleware throws an exception if it detects this combination.

---

## 11. Content Negotiation (the basics)

The client can tell the server what kind of response it prefers:

```
Accept: text/html, application/json;q=0.9, */*;q=0.8
```

The `q` is quality (0.0 to 1.0). In MiniJWS I don't implement automatic content negotiation — the server always returns whatever type the handler decides. But the decoder correctly parses the `Accept` header.

Same with `Accept-Encoding`:

```
Accept-Encoding: gzip, deflate
```

The `GzipMiddleware` checks this header. If the client accepts gzip, it compresses the body and adds `Content-Encoding: gzip` to the response. If not, it passes the response through uncompressed.

---

## 12. HTTP versions

HTTP has evolved. I implemented HTTP/1.1 in MiniJWS because it's the version that offers the best balance between implementation complexity and real-world usefulness:

| Version | Main new features |
|---------|-------------------|
| **HTTP/0.9** | Only GET, no headers, no status codes. It was basically: "give me this page" → here it is. |
| **HTTP/1.0** | Added headers, status codes, POST, Content-Type. Each request opens and closes a connection. |
| **HTTP/1.1** | Persistent connections, chunked transfer, mandatory Host header, more methods. This is what MiniJWS implements. |
| **HTTP/2** | Binary (not text), request multiplexing over a single TCP connection, header compression. Not wire-compatible with HTTP/1.1. |
| **HTTP/3** | Uses QUIC (over UDP) instead of TCP. Reduces connection latency to nearly zero. |

If you're studying HTTP, I recommend mastering HTTP/1.1 before looking at HTTP/2 or 3. The concepts (request, response, headers, status) are the same, and understanding how things were done in 1.1 gives you perspective to appreciate the optimizations in later versions.

---

## 13. HTTPS and why MiniJWS doesn't have it

HTTPS is HTTP over TLS (formerly SSL). The HTTP messages are exactly the same, but they travel encrypted. To implement HTTPS in Java you need an `SSLServerSocket` instead of a `ServerSocket`, plus certificate management.

I haven't implemented it because:
- It adds complexity (certificate management, keystores, encryption) that distracts from the goal of learning HTTP
- For production, the recommended approach is to put a reverse proxy (nginx, Apache) with TLS in front of MiniJWS
- `SSLServerSocket` in Java works, but configuring it properly requires security knowledge beyond the educational scope of this project

If you want to add it, the server structure allows it — only the initial connection acceptance would change.

---

## 14. MIME mapping: from extension to Content-Type

The server needs to know what `Content-Type` to assign to each file. For example, `style.css` → `text/css`, `script.js` → `text/javascript`. In MiniJWS I have `ContentTypes.EXTENSION_TO_MIME`, a `Map<String, String>` with the associations:

```
"html" → "text/html;charset=utf-8"
"json" → "application/json;charset=utf-8"
"png"  → "image/png"
"apk"  → "application/vnd.android.package-archive"
...
```

`StaticFileHandler` uses this map when serving files. If the extension isn't mapped, it returns `application/octet-stream` (generic binary).

---

## 15. What I learned in the process

It might sound cliché, but implementing HTTP from scratch changed how I understand the web. Things I used to take for granted ("just set Content-Type, just set status code...") I now know exactly what they do and why they exist.

The university concepts that appear in this project:

- **TCP/IP and Sockets** — how two machines connect over a network
- **Text-based protocols** — how readable messages are structured over bytes
- **State machines** — the HTTP decoder is basically a state machine that goes from "reading request line" to "reading headers" to "reading body"
- **Thread pool concurrency** — how to serve N clients with M threads
- **Design patterns** — Builder, Chain of Responsibility, Strategy, Producer-Consumer
- **Security** — path traversal, rate limiting, CORS, HttpOnly cookies
- **Caching and performance** — keep-alive, compression, 301 vs 302 redirects

If you're studying computer science and want to really understand how the web works, I recommend building your own HTTP server, even if it's minimal. You don't need all the features MiniJWS has — with a server that serves one HTML page, you'll learn more than reading three textbook chapters.

---

[← Previous](index.md) · [Next →](patterns.md)  
[🇪🇸 Español](http-protocol.md) · [🇬🇧 English](http-protocol.en.md)
