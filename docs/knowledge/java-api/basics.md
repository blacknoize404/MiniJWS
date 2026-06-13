# Java Basics

## Keywords y Estructuras Fundamentales

### `package`

Declara el paquete al que pertenece una clase. Se usa en todos los archivos fuente.

```java
package io.github.blacknoize404.miniJWS;
package io.github.blacknoize404.miniJWS.primitives;
package io.github.blacknoize404.miniJWS.middleware;
```

La convención es dominio invertido: `io.github.blacknoize404.miniJWS.{subpackage}`.

### `import`

Trae clases de otros paquetes al espacio de nombres actual.

```java
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
```

- `import java.util.concurrent.*;` — importa todas las clases del paquete (wildcard import)
- `import` no afecta el rendimiento en tiempo de ejecución, solo es azúcar de compilación

### `class`

Define un tipo de objeto. Puede ser:

- **Pública** (`public class`) — accesible desde cualquier paquete
- **Final** (`public final class`) — no puede ser extendida (ej: `HttpServer`, `AccessLogMiddleware`)
- **Inner class** — clase dentro de otra clase (ej: `HttpResponse.Builder`, `HttpRequest.Builder`)

```java
public final class HttpServer { ... }
public class HttpResponse {
    public static class Builder { ... }
}
```

### `interface`

Define un contrato que las clases implementan. Puede tener métodos abstractos y (desde Java 8) métodos `default` y `static`. Desde Java 8, las interfaces pueden tener `@FunctionalInterface`.

```java
@FunctionalInterface
public interface Middleware {
    HttpResponse run(HttpRequest request, MiddlewareChain chain);
}
```

### `enum`

Tipo seguro para constantes nombradas. Puede tener campos, constructores y métodos.

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

**Características:**
- Instancias singleton predefinidas dentro del enum
- Constructor privado implícito
- `name()` devuelve el nombre del constante como String (ej: `ContentType.JSON.name()` → `"JSON"`)
- `valueOf(String)` convierte String a enum
- `values()` devuelve array de todas las constantes
- `ordinal()` devuelve la posición (0-based)

### `record` (Java 16+)

Clase inmutable compacta. Genera automáticamente: constructor, `equals()`, `hashCode()`, `toString()`, y accessors.

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

**Generado automáticamente:**
- Constructor con todos los parámetros
- `packageName()` — accessor (no `getPackageName()`, sino `packageName()`)
- `equals()` — comparación por valor de todos los campos
- `hashCode()` — hash basado en todos los campos
- `toString()` — representación con todos los campos

**Reglas:**
- Todos los campos son `private final`
- No puede extender otras clases (pero implementa interfaces)
- No puede tener campos instance adicionales (solo los del constructor)
- Puede tener métodos static y métodos instance

### Annotation (`@`)

Metadatos para el compilador, runtime, o procesamiento en tiempo de compilación.

```java
@Override  // verifica que el método sobrescribe uno de superclase/interface
@FunctionalInterface  // verifica que la interfaz tiene exactamente un método abstracto
@SuppressWarnings("unchecked")  // suprime advertencias del compilador
```

### `@FunctionalInterface`

Marca una interfaz como funcional — debe tener exactamente **un** método abstracto. Permite usarla como target de lambdas.

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

Sin esta anotación también funcionan como functional interface (si tienen un solo método abstracto). La anotación es documentación + verificación del compilador.

### `@Override`

Indica que un método sobrescribe un método de superclase o implementa uno de interfaz. El compilador lanza error si no hay tal método.

### `static`

Pertenece a la clase, no a la instancia:

- **Campo static:** una sola copia compartida por todas las instancias
- **Método static:** puede llamarse sin instancia (`HttpEncoder.sendResponse(...)`)
- **Bloque static:** se ejecuta una vez cuando la clase se carga (`ContentType.EXT_MAP` initialization)
- **Inner class static:** `HttpResponse.Builder` puede instanciarse sin instancia de `HttpResponse`

```java
public final class HttpEncoder {
    private HttpEncoder() {}  // constructor privado — clase utilitaria
    public static void sendResponse(HttpResponse response, OutputStream outputStream) { ... }
}

public class HttpResponse {
    public static HttpResponse redirect(String location) { ... }
    public static class Builder { ... }
}
```

### `final`

- **Clase final:** no puede ser extendida (`public final class HttpServer`)
- **Método final:** no puede ser sobrescrito
- **Campo final:** debe ser asignado una vez (en declaración o constructor), no puede reasignarse
- **Variable local final / effectively final:** puede usarse en lambdas

```java
private final AtomicBoolean running = new AtomicBoolean(false);
private final List<Middleware> middlewares = new CopyOnWriteArrayList<>();
```

### Modificadores de Acceso

| Modificador | Clase | Paquete | Subclase | Mundo |
|-------------|-------|---------|----------|-------|
| `public` | ✓ | ✓ | ✓ | ✓ |
| `protected` | ✓ | ✓ | ✓ | ✗ |
| *(default)* | ✓ | ✓ | ✗ | ✗ |
| `private` | ✓ | ✗ | ✗ | ✗ |

### `var` (Java 10+)

Inferencia de tipo local. El compilador deduce el tipo de la expresión del lado derecho.

```java
var sb = new StringBuilder();          // StringBuilder
var reader = new BufferedInputStream(in); // BufferedInputStream
var json = req.bodyAsJson().orElse(Map.of()); // Map<String, String>
var headersCopy = new LinkedHashMap<String, List<String>>();
```

**Restricciones:**
- Solo para variables locales
- No para campos, parámetros, o tipos de retorno
- El tipo se deduce en tiempo de compilación (sigue siendo estáticamente tipado)

### `this`

Referencia a la instancia actual. Usos:

1. **Devolver `this`** para fluent interface: `.setStatusCode(200).setContentType(...)`
2. **Llamar otro constructor:** `this(port, Runtime.getRuntime().availableProcessors() * 2);`
3. **Desambiguar:** `this.port = port`

---

[← Anterior](index.md) · [Siguiente →](oop.md)  
[🇪🇸 Español](basics.md)
