# Design Patterns Used in MiniJWS

I didn't start this project thinking "I'm going to use these patterns". The patterns emerged as the natural solution to problems I encountered along the way. Here are the ones I ended up using, with real code examples so you can see how they apply in practice.

## Builder

**Where:** `HttpRequest.Builder`, `HttpResponse.Builder`

The problem was simple: an HTTP request has many optional fields (headers, body, cookies, parameters...). Having a constructor with 10 parameters was unworkable, and having 20 setters without order wasn't much better.

The Builder solves this: each setter returns `this`, and at the end `build()` constructs the immutable object:

```java
new HttpResponse.Builder()
    .setStatusCode(200)
    .setContentType(ContentType.JSON)
    .setCookie("session", "abc123", 3600, "/", true)
    .setBody("{\"status\":\"ok\"}")
    .build();  // → returns an HttpResponse, immutable from now on
```

The `build()` method calls the private constructor and performs validations (e.g., `Objects.requireNonNull` in the `HttpRequest` builder).

**Why it worked:** Because HTTP requests and responses are immutable once built, and the Builder allows building them step by step without exposing a half-built object.

---

## Chain of Responsibility (Middleware)

**Where:** `Middleware` + `MiddlewareChain` + `buildChain()` in `HttpServer`

This is my favourite pattern in the project. The idea is that each middleware is a link in a chain. Each one receives the request and a `chain` object to pass control to the next:

```
Request → Middleware1 → next() → Middleware2 → next() → Handler → Response
               ↑                                           ↓
               └─────── post-processing ←──────────────────┘
```

I arrived at the implementation after several attempts. I ended up building the chain in reverse (the last registered middleware wraps the handler, the second-last wraps the last, etc.):

```java
// HttpServer.java:171-199
MiddlewareChain terminal = req -> { /* route matching */ };
MiddlewareChain chain = terminal;
for (int i = middlewares.size() - 1; i >= 0; i--) {
    Middleware mw = middlewares.get(i);
    MiddlewareChain next = chain;
    chain = req -> mw.run(req, next);
}
return chain;
```

It's a simple but powerful idea: each middleware can:
- Short-circuit the chain (return a response without calling `next()`)
- Modify the request before passing it on
- Modify the response on the way back
- Measure times, log, check permissions...

If I used a simple list of interceptors, I couldn't easily do things like measure the handler's execution time (I need code BEFORE and AFTER `next()`).

**The interfaces ended up like this:**

```java
@FunctionalInterface
interface Middleware {
    HttpResponse run(HttpRequest request, MiddlewareChain chain);
}

@FunctionalInterface
interface MiddlewareChain {
    HttpResponse next(HttpRequest request);
}
```

Being `@FunctionalInterface`, you can write middleware with lambdas:

```java
server.use((request, chain) -> {
    long start = System.nanoTime();
    HttpResponse response = chain.next(request);
    long ms = (System.nanoTime() - start) / 1_000_000;
    System.out.println(request.getUri() + " → " + response.getStatusCode() + " (" + ms + "ms)");
    return response;
});
```

---

## Strategy

**Where:** `RequestRunner`

Route handlers are interchangeable. They all implement the same interface:

```java
@FunctionalInterface
interface RequestRunner {
    HttpResponse run(HttpRequest request);
}
```

Each route stores a `RequestRunner`. The server chooses which one to execute based on the URL and method, but they are all used the same way. This allows middleware to treat all handlers equally and lets you test each route in isolation.

---

## Immutability

**Where:** `HttpRequest`, `HttpResponse`

Both are effectively immutable after construction:
- All fields are `final`
- Collections are not modified after construction
- There are no setters on the constructed object
- `body` is `Optional<byte[]>` — the array is modifiable in theory, but never exposed for writing

**Why immutable?** Because `HttpRequest` travels through the entire middleware chain. If a middleware modified the request, the following ones would receive changed data without knowing it. With immutable objects, if a middleware wants to change something, it creates a copy (using the Builder) and passes that. The original remains intact.

Additionally, immutability eliminates concurrency bugs: multiple threads can read the same `HttpRequest` without synchronization.

---

## Factory Methods

**Where:** `HttpResponse.redirect()`

```java
public static HttpResponse redirect(String location) {
    return redirect(location, 302);
}

public static HttpResponse redirect(String location, int statusCode) {
    return new HttpResponse.Builder()
            .setStatusCode(statusCode)
            .addHeader("Location", location)
            .build();
}
```

I could have used a constructor or a generic static method, but `redirect()` is more expressive. When you read `HttpResponse.redirect("/login")` you know exactly what it does without having to check the parameters.

---

## Fluent Interface

**Everywhere:** setters return `this`

```java
server.use(new CorsMiddleware().allowOrigin("*").allowMethods("GET", "POST"));
server.addRoute(HttpMethod.GET, "/", handler).addRoute(HttpMethod.POST, "/", handler);
```

It's not a fancy named pattern, but it greatly reduces boilerplate code and makes configuration read almost like natural language.

---

## Singleton (limited)

**Where:** `HttpEncoder`, `HttpDecoder`, `HttpStatusCode`

They aren't textbook singletons (no single-instance management), but they have private constructors and only static methods. They are stateless utility classes. The private constructor prevents meaningless instantiation.

---

## Producer-Consumer

**Where:** `AccessLogMiddleware` — async logging

Request logging should not slow down responses. To achieve this, the middleware puts log lines into a queue and a separate thread writes them to the file:

```java
private final BlockingQueue<String> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
private final Thread worker;

// Request thread (producer):
queue.offer(logLine);

// Worker thread (consumer):
while (!interrupted) {
    String line = queue.take();  // blocks if queue is empty
    writer.println(line);
}
```

The bounded queue (`16,384` entries) prevents the producer from getting too far ahead of the consumer (backpressure). If the queue fills up, `offer()` returns false and the line is dropped, but the request thread is not blocked.

---

## Template Method / Inversion of Control

**Where:** `StaticSite` / `QrStaticSite`

The base class `StaticSite` scans a directory and builds routes automatically. `QrStaticSite` extends that behaviour by adding QR code injection. The scanning loop is the same; what changes is how each route is built. The base class calls a method that subclasses override (or, in our case, receive lambdas that act as callbacks).

---

[← Previous](http-protocol.en.md) · [Next →](decisions.en.md)  
[🇪🇸 Español](patterns.md) · [🇬🇧 English](patterns.en.md)
