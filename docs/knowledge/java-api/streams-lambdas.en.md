# Streams & Lambdas

## Lambdas (Java 8+)

Expressions that represent an anonymous function. Syntax: `(params) -> { body }` or `(params) -> expression`.

### Usage as FunctionalInterface Implementation

Where a single abstract method interface is expected:

```java
// Lambda as RequestRunner (functional interface):
server.addRoute(HttpMethod.GET, "/hello", req ->
    new HttpResponse.Builder()
        .setContentType(ContentType.TEXT)
        .setBody("Hello, World!")
        .build()
);

// Lambda with block:
server.addRoute(HttpMethod.GET, "/hello/:name", req -> {
    String name = req.getParameters().get("name");
    return new HttpResponse.Builder()
        .setContentType(ContentType.TEXT)
        .setBody("Hello, " + name + "!")
        .build();
});
```

### Lambda as Middleware

```java
server.use((request, chain) -> {
    long start = System.nanoTime();
    HttpResponse response = chain.next(request);
    long ms = (System.nanoTime() - start) / 1_000_000;
    System.out.println("Took " + ms + "ms");
    return response;
});
```

### Lambda in Middleware Chain

```java
chain = req -> mw.run(req, next);
```

Here `req` is the parameter of the `MiddlewareChain.next()` method. The lambda captures `mw` and `next` (external variables — see closure below).

### Lambda in Threads

```java
new Thread(() -> drainTo(pw), "access-log").start();
threadPool.execute(() -> handleConnection(client));
```

### Lambda in Shutdown Hook

```java
Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
```

---

## Method References (::)

Shortcut for lambdas that only call an existing method.

| Form | Syntax | Lambda Equivalent |
|------|--------|------------------|
| Static method | `ClassName::staticMethod` | `args → ClassName.staticMethod(args)` |
| Instance method | `instance::method` | `args → instance.method(args)` |
| Constructor | `ClassName::new` | `args → new ClassName(args)` |

**Examples in the project:**

```java
// Static method reference (ApkReader):
.map(p -> p.getName())  // → could be .map(ApkPermission::getName)
// but the project uses an explicit lambda.

// Instance method reference (shutdown hook):
Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
// Equivalent to: new Thread(() -> this.stop())

// Constructor reference:
// (not used directly, but valid)
```

---

## Closures (Variable Capture)

Lambdas can access variables from the scope where they are defined, as long as they are **effectively final** (not reassigned after capture).

```java
// In buildChain():
for (int i = middlewares.size() - 1; i >= 0; i--) {
    Middleware mw = middlewares.get(i);
    MiddlewareChain next = chain;       // effectively final
    chain = req -> mw.run(req, next);   // captures mw and next
}
// Each iteration captures its own mw and next variables (different for each lambda)
```

Another example — capturing `chain` and then `middleware`:

```java
// CorsMiddleware as fluent expression:
server.use(new CorsMiddleware().allowOrigin("*"));
// The registered lambda captures the middleware configuration.
```

---

## Stream API (Java 8+)

Sequence of elements supporting functional-style map/filter/reduce operations.

### stream()

Converts a collection into a stream:

```java
meta.getUsesPermissions().stream()
    .map(p -> p.getName())
    .collect(Collectors.toList());
```

### map()

Transforms each element:

```java
.stream()
.map(p -> p.getName())           // ApkPermission → String
```

### filter()

Selects elements that satisfy a condition:

```java
.stream()
.filter(k -> k.equalsIgnoreCase("Content-Encoding"))
```

### forEach()

Executes an action for each element:

```java
info.permissions().forEach(p -> sb.append("  - ").append(p).append("\n"));
// Direct usage on Collection (does not need .stream() for forEach)
```

### collect()

Converts the stream back to a collection:

```java
.collect(Collectors.toList())  // → List
```

### anyMatch() — Short-circuit

```java
boolean alreadyEncoded = response.getHeaders().keySet().stream()
    .anyMatch(k -> k.equalsIgnoreCase("Content-Encoding"));
// → true if any header matches
```

### Stream in HttpDecoder

```java
// (implicit) — iterating over headers map
for (var entry : response.getHeaders().entrySet()) {
    builder.addHeader(entry.getKey(), entry.getValue());
}
```

### toList() (Java 16+)

Direct alternative to `collect(Collectors.toList())`:

```java
stream.toList()  // → immutable list
```

---

## Collector & Collectors

### Collectors.toList()

Accumulates elements into a `List`:

```java
.collect(Collectors.toList())
```

### Collectors.joining()

Concatenates strings:

```java
String.join(", ", allowMethods)  // alternative without stream
```

---

## Double-Colon Operator (::) for Iteration

```java
// In AccessLogMiddleware.drainTo:
queue.drainTo(lines);  // not a stream, but uses the concept
lines.forEach(line -> writer.println(line));  // Consumer lambda
```

---

## Additional Notes

### Lambda vs Anonymous Class

Lambdas are more concise and do not create a separate .class file. Syntactically equivalent to anonymous classes of functional interfaces.

### Effectively Final

Since Java 8, captured variables in lambdas do not need to be explicitly declared `final` — they just cannot be reassigned after capture.

### Streams are Lazy

Intermediate operations (`map`, `filter`) are lazy — they do not execute until a terminal operation (`collect`, `forEach`, `anyMatch`) triggers them.

---

[← Previous](io-networking.en.md) · [Next →](exceptions-time.en.md)  
[🇪🇸 Español](streams-lambdas.md) · [🇬🇧 English](streams-lambdas.en.md)
