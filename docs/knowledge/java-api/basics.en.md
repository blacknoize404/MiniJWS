# Java Basics

## Keywords and Fundamental Structures

### `package`

Declares the package to which a class belongs. Used in all source files.

```java
package io.github.blacknoize404.miniJWS;
package io.github.blacknoize404.miniJWS.primitives;
package io.github.blacknoize404.miniJWS.middleware;
```

The convention is reversed domain: `io.github.blacknoize404.miniJWS.{subpackage}`.

### `import`

Brings classes from other packages into the current namespace.

```java
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
```

- `import java.util.concurrent.*;` — imports all classes from the package (wildcard import)
- `import` does not affect runtime performance, it's only compile-time sugar

### `class`

Defines an object type. Can be:

- **Public** (`public class`) — accessible from any package
- **Final** (`public final class`) — cannot be extended (e.g., `HttpServer`, `AccessLogMiddleware`)
- **Inner class** — class inside another class (e.g., `HttpResponse.Builder`, `HttpRequest.Builder`)

```java
public final class HttpServer { ... }
public class HttpResponse {
    public static class Builder { ... }
}
```

### `interface`

Defines a contract that classes implement. Can have abstract methods and (since Java 8) `default` and `static` methods. Since Java 8, interfaces can have `@FunctionalInterface`.

```java
@FunctionalInterface
public interface Middleware {
    HttpResponse run(HttpRequest request, MiddlewareChain chain);
}
```

### `enum`

Type-safe way for named constants. Can have fields, constructors and methods.

```java
public enum HttpMethod {
    GET, HEAD, POST, PUT, DELETE,
    CONNECT, OPTIONS, TRACE, PATCH
}

public enum ContentType {
    FILE("application/octet-stream"),
    JSON("application/json;charset=utf-8"),
    HTML("text/html;charset=utf-8");

    private final String mime;

    ContentType(String mime) {
        this.mime = mime;
    }

    public String mime() { return mime; }
}
```

**Features:**
- Predefined singleton instances within the enum
- Implicit private constructor
- `name()` returns the constant name as String (e.g., `ContentType.JSON.name()` → `"JSON"`)
- `valueOf(String)` converts String to enum
- `values()` returns array of all constants
- `ordinal()` returns position (0-based)

### `record` (Java 16+)

Compact immutable class. Automatically generates: constructor, `equals()`, `hashCode()`, `toString()`, and accessors.

```java
public record ApkInfo(
    String packageName,
    String versionName,
    long versionCode,
    String minSdkVersion,
    String targetSdkVersion,
    List<String> permissions,
    List<String> features,
    String label,
    String icon
) {}
```

**Automatically generated:**
- Constructor with all parameters
- `packageName()` — accessor (not `getPackageName()`, but `packageName()`)
- `equals()` — value comparison of all fields
- `hashCode()` — hash based on all fields
- `toString()` — representation with all fields

**Rules:**
- All fields are `private final`
- Cannot extend other classes (but can implement interfaces)
- Cannot have additional instance fields (only those in the constructor)
- Can have static methods and instance methods

### Annotation (`@`)

Metadata for the compiler, runtime, or compile-time processing.

```java
@Override  // verifies that the method overrides a superclass/interface method
@FunctionalInterface  // verifies that the interface has exactly one abstract method
@SuppressWarnings("unchecked")  // suppresses compiler warnings
```

### `@FunctionalInterface`

Marks an interface as functional — it must have exactly **one** abstract method. Allows it to be used as a lambda target.

```java
@FunctionalInterface
public interface RequestRunner {
    HttpResponse run(HttpRequest request);
}

@FunctionalInterface
public interface Middleware {
    HttpResponse run(HttpRequest request, MiddlewareChain chain);
}

@FunctionalInterface
public interface MiddlewareChain {
    HttpResponse next(HttpRequest request);
}
```

Without this annotation they also work as functional interfaces (if they have a single abstract method). The annotation is documentation + compiler verification.

### `@Override`

Indicates that a method overrides a superclass method or implements an interface method. The compiler throws an error if no such method exists.

### `static`

Belongs to the class, not the instance:

- **Static field:** a single copy shared by all instances
- **Static method:** can be called without an instance (`HttpEncoder.sendResponse(...)`)
- **Static block:** executed once when the class is loaded (`ContentType.EXT_MAP` initialization)
- **Static inner class:** `HttpResponse.Builder` can be instantiated without an instance of `HttpResponse`

```java
public final class HttpEncoder {
    private HttpEncoder() {}  // private constructor — utility class
    public static void sendResponse(HttpResponse response, OutputStream outputStream) { ... }
}

public class HttpResponse {
    public static HttpResponse redirect(String location) { ... }
    public static class Builder { ... }
}
```

### `final`

- **Final class:** cannot be extended (`public final class HttpServer`)
- **Final method:** cannot be overridden
- **Final field:** must be assigned once (in declaration or constructor), cannot be reassigned
- **Local final / effectively final variable:** can be used in lambdas

```java
private final AtomicBoolean running = new AtomicBoolean(false);
private final List<Middleware> middlewares = new CopyOnWriteArrayList<>();
```

### Access Modifiers

| Modifier | Class | Package | Subclass | World |
|----------|-------|---------|----------|-------|
| `public` | ✓ | ✓ | ✓ | ✓ |
| `protected` | ✓ | ✓ | ✓ | ✗ |
| *(default)* | ✓ | ✓ | ✗ | ✗ |
| `private` | ✓ | ✗ | ✗ | ✗ |

### `var` (Java 10+)

Local type inference. The compiler deduces the type from the right-hand side expression.

```java
var sb = new StringBuilder();          // StringBuilder
var reader = new BufferedInputStream(in); // BufferedInputStream
var json = req.bodyAsJson().orElse(Map.of()); // Map<String, String>
var headersCopy = new LinkedHashMap<String, List<String>>();
```

**Restrictions:**
- Only for local variables
- Not for fields, parameters, or return types
- Type is deduced at compile time (still statically typed)

### `this`

Reference to the current instance. Uses:

1. **Return `this`** for fluent interface: `.setStatusCode(200).setContentType(...)`
2. **Call another constructor:** `this(port, Runtime.getRuntime().availableProcessors() * 2);`
3. **Disambiguate:** `this.port = port`

---

[← Previous](index.en.md) · [Next →](oop.en.md)  
[🇪🇸 Español](basics.md) · [🇬🇧 English](basics.en.md)
