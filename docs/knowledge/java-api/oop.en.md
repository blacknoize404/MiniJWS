# OOP — Object-Oriented Programming

## Inner Classes

### Static Inner Class

Has no reference to the outer class instance. Typically used for Builders.

```java
public class HttpResponse {
    // ... HttpResponse fields and methods ...

    public static class Builder {
        private int statusCode = 200;

        public Builder setStatusCode(int code) {
            this.statusCode = code;
            return this;
        }

        public HttpResponse build() {
            return new HttpResponse(headers, statusCode, method, protocolVersion, body);
        }
    }
}

// Usage:
new HttpResponse.Builder()
    .setStatusCode(200)
    .setContentType(ContentType.JSON)
    .build();
```

**Features:**
- Instantiated without an outer class instance: `new HttpResponse.Builder()`
- Can access `private` static members of the outer class
- `HttpResponse` constructor is private, but `Builder` (being an inner class) can call it

### Anonymous Class

Nameless class defined and instantiated in a single expression. Used in the project for:

1. **Shutdown hook:**

```java
Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
```

2. **GZIPOutputStream with custom level:**

```java
var gz = new GZIPOutputStream(bos) {{
    def.setLevel(level);
}};
```

This is a **double brace initialization** — the first brace creates an anonymous subclass, the second is an instance initializer.

3. **Worker thread in AccessLogMiddleware:**

```java
this.worker = new Thread(() -> drainTo(pw), "access-log");
```

(Here the lambda avoids the anonymous class, but in Java 8+ it's equivalent.)

### Double Brace Initialization

Pattern that combines anonymous class + instance initializer. The first `{` creates an anonymous subclass, the second `{` is an instance initializer block.

```java
// In GzipMiddleware — customize compression level:
var gz = new GZIPOutputStream(bos) {{
    def.setLevel(level);
}};
```

**Equivalent without double brace:**
```java
var gz = new GZIPOutputStream(bos) {
    {
        // instance initializer — executes after the constructor
        def.setLevel(level);
    }
};
```

**⚠ Warning:** Double brace initialization creates an additional anonymous class on each use, which can increase memory usage in loops. In GzipMiddleware it's used once per compressed request, which is acceptable.

## Private Constructor

Used for:
1. **Utility classes** (only static methods, should not be instantiated):

```java
public final class HttpEncoder {
    private HttpEncoder() {}
    public static void sendResponse(...) { ... }
}
```

2. **Singleton pattern** (although not used in the project)
3. **Force Builder usage** — `HttpResponse` and `HttpRequest` constructors are private, only the Builder can construct them:

```java
public class HttpRequest {
    private HttpRequest(HttpMethod method, URI uri, String protocolVersion,
                        Map<String, List<String>> headers,
                        Map<String, String> parameters,
                        Optional<byte[]> body) {
        // ...
    }
}
```

## Static Members

### Static Fields

Single copy shared by all instances (or accessible without an instance):

```java
// In HttpServer:
public static final String SERVER_NAME = "MiniJWS";
private static final int MAX_KEEPALIVE_REQUESTS = 100;
private static final int KEEPALIVE_TIMEOUT_MS = 10_000;

// In HttpStatusCode:
public static final Map<Integer, String> STATUS_CODES = Map.ofEntries(...);
```

### Static Methods

Operate at the class level, do not require an instance:

```java
HttpResponse.redirect("/login");           // factory method
HttpDecoder.decode(inputStream);           // parser utility
HttpEncoder.sendResponse(response, out);   // serializer utility
HttpServer.matchPath(pattern, path);       // package-private helper
```

### Static Initializer

Block executed once when the class is loaded. Useful for initializing static structures:

```java
public enum ContentType {
    // ...
    private static final java.util.Map<String, ContentType> EXT_MAP = new java.util.HashMap<>();

    static {
        for (var type : values()) {
            EXT_MAP.put(type.name().toLowerCase(), type);
        }
        EXT_MAP.put("jpg", JPEG);
        EXT_MAP.put("jpeg", JPEG);
    }
}
```

## Method Overriding

Overriding a superclass or interface method requires:
- Same signature (name + parameters)
- Same return type or covariant
- Visibility no more restrictive

Example: no class in the project extends other classes, but all implement interfaces:

```java
@Override
public HttpResponse run(HttpRequest request, MiddlewareChain chain) {
    // specific middleware implementation
}
```

## Composition over Inheritance

The project favors composition over inheritance. Clear example: `StaticSite` contains an `HttpServer` instead of extending it:

```java
public final class StaticSite {
    private final HttpServer server;  // composition

    public StaticSite(int port, Path rootDirectory) throws IOException {
        this.server = new HttpServer(port);
        // ...
    }

    public void start() {
        new Thread(server::run).start();  // delegates
    }
}
```

## Fluent Interface (Method Chaining)

Each setter method returns `this` to allow chaining:

```java
// In HttpServer:
public HttpServer addRoute(HttpMethod method, String path, RequestRunner runner) {
    routes.put(key, runner);
    return this;
}

public HttpServer use(Middleware middleware) {
    middlewares.add(middleware);
    return this;
}

// Usage:
server.use(new AccessLogMiddleware())
      .use(new CorsMiddleware())
      .addRoute(HttpMethod.GET, "/", handler);
```

## Package-Private (Default Access)

Methods without access modifier — visible within the same package:

```java
// In HttpServer — package-private for testing:
static Map<String, String> matchPath(String pattern, String path) {
    // ...
}
```

This allows tests in the same package to access without making the method public.

---

[← Previous](basics.en.md) · [Next →](generics-collections.en.md)  
[🇪🇸 Español](oop.md) · [🇬🇧 English](oop.en.md)
