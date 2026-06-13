# OOP — Object-Oriented Programming

## Inner Classes (Clases Internas)

### Static Inner Class

No tiene referencia a la instancia de la clase externa. Se usa típicamente para Builders.

```java
public class HttpResponse {
    // ... campos y métodos de HttpResponse ...

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

// Uso:
new HttpResponse.Builder()
    .setStatusCode(200)
    .setContentType(ContentType.JSON)
    .build();
```

**Características:**
- Se instancia sin instancia de la clase externa: `new HttpResponse.Builder()`
- Puede acceder a miembros `private` estáticos de la clase externa
- El constructor de `HttpResponse` es privado, pero `Builder` (siendo inner class) puede llamarlo

### Anonymous Class

Clase sin nombre definida e instanciada en una sola expresión. Usado en el proyecto para:

1. **Shutdown hook:**

```java
Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
```

2. **GZIPOutputStream con nivel personalizado:**

```java
var gz = new GZIPOutputStream(bos) {{
    def.setLevel(level);
}};
```

Este es un **double brace initialization** — la primera llave crea una anonymous subclass, la segunda es un instance initializer.

3. **Hilo worker en AccessLogMiddleware:**

```java
this.worker = new Thread(() -> drainTo(pw), "access-log");
```

(Aquí la lambda evita la anonymous class, pero en Java 8+ es equivalente.)

### Double Brace Initialization

Patrón que combina anonymous class + instance initializer. La primera `{` crea una subclase anónima, la segunda `{` es un bloque inicializador de instancia.

```java
// En GzipMiddleware — personalizar nivel de compresión:
var gz = new GZIPOutputStream(bos) {{
    def.setLevel(level);
}};
```

**Equivalente sin double brace:**
```java
var gz = new GZIPOutputStream(bos) {
    {
        // instance initializer — se ejecuta después del constructor
        def.setLevel(level);
    }
};
```

**⚠ Advertencia:** Double brace initialization crea una clase anónima adicional en cada uso, lo que puede aumentar el uso de memoria en bucles. En GzipMiddleware se usa una sola vez por request comprimido, es aceptable.

## Private Constructor

Se usa para:
1. **Clases utilitarias** (solo métodos estáticos, no deben instanciarse):

```java
public final class HttpEncoder {
    private HttpEncoder() {}
    public static void sendResponse(...) { ... }
}
```

2. **Singleton pattern** (aunque no usado en el proyecto)
3. **Forzar uso del Builder** — el constructor de `HttpResponse` y `HttpRequest` es privado, solo el Builder puede construirlos:

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

Una copia compartida por todas las instancias (o accesible sin instancia):

```java
// En HttpServer:
public static final String SERVER_NAME = "MiniJWS";
private static final int MAX_KEEPALIVE_REQUESTS = 100;
private static final int KEEPALIVE_TIMEOUT_MS = 10_000;

// En HttpStatusCode:
public static final Map<Integer, String> STATUS_CODES = Map.ofEntries(...);
```

### Static Methods

Operan a nivel de clase, no requieren instancia:

```java
HttpResponse.redirect("/login");           // factory method
HttpDecoder.decode(inputStream);           // parser utility
HttpEncoder.sendResponse(response, out);   // serializer utility
HttpServer.matchPath(pattern, path);       // package-private helper
```

### Static Initializer

Bloque ejecutado una vez al cargar la clase. Útil para inicializar estructuras estáticas:

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

Sobrescribir un método de superclase o interfaz requiere:
- Misma firma (nombre + parámetros)
- Mismo tipo de retorno o covariante
- Visibilidad no más restrictiva

Ejemplo: ninguna clase en el proyecto extiende otras clases, pero todas implementan interfaces:

```java
@Override
public HttpResponse run(HttpRequest request, MiddlewareChain chain) {
    // implementación específica del middleware
}
```

## Composition over Inheritance

El proyecto favorece composición sobre herencia. Ejemplo claro: `StaticSite` contiene un `HttpServer` en lugar de extenderlo:

```java
public final class StaticSite {
    private final HttpServer server;  // composición

    public StaticSite(int port, Path rootDirectory) throws IOException {
        this.server = new HttpServer(port);
        // ...
    }

    public void start() {
        new Thread(server::run).start();  // delega
    }
}
```

## Fluent Interface (Method Chaining)

Cada método setter devuelve `this` para permitir encadenamiento:

```java
// En HttpServer:
public HttpServer addRoute(HttpMethod method, String path, RequestRunner runner) {
    routes.put(key, runner);
    return this;
}

public HttpServer use(Middleware middleware) {
    middlewares.add(middleware);
    return this;
}

// Uso:
server.use(new AccessLogMiddleware())
      .use(new CorsMiddleware())
      .addRoute(HttpMethod.GET, "/", handler);
```

## Package-Private (Default Access)

Métodos sin modificador de acceso — visibles dentro del mismo paquete:

```java
// En HttpServer — package-private para testing:
static Map<String, String> matchPath(String pattern, String path) {
    // ...
}
```

Esto permite que tests en el mismo paquete accedan sin hacer el método público.

---

[← Anterior](basics.md) · [Siguiente →](generics-collections.md)  
[🇪🇸 Español](oop.md)
