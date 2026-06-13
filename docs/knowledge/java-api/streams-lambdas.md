# Streams & Lambdas

## Lambdas (Java 8+)

Expresiones que representan una función anónima. Sintaxis: `(params) -> { cuerpo }` o `(params) -> expresión`.

### Uso como Implementación de FunctionalInterface

Donde se espera una interfaz con un solo método abstracto:

```java
// Lambda como RequestRunner (interfaz funcional):
server.addRoute(HttpMethod.GET, "/hello", req ->
    new HttpResponse.Builder()
        .setContentType(ContentType.TEXT)
        .setBody("Hello, World!")
        .build()
);

// Lambda con bloque:
server.addRoute(HttpMethod.GET, "/hello/:name", req -> {
    String name = req.getParameters().get("name");
    return new HttpResponse.Builder()
        .setContentType(ContentType.TEXT)
        .setBody("Hello, " + name + "!")
        .build();
});
```

### Lambda como Middleware

```java
server.use((request, chain) -> {
    long start = System.nanoTime();
    HttpResponse response = chain.next(request);
    long ms = (System.nanoTime() - start) / 1_000_000;
    System.out.println("Took " + ms + "ms");
    return response;
});
```

### Lambda en la Cadena de Middleware

```java
chain = req -> mw.run(req, next);
```

Aquí `req` es el parámetro del método `MiddlewareChain.next()`. La lambda captura `mw` y `next` (variables externas — ver closure más abajo).

### Lambda en Threads

```java
new Thread(() -> drainTo(pw), "access-log").start();
threadPool.execute(() -> handleConnection(client));
```

### Lambda en Shutdown Hook

```java
Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
```

---

## Method References (::)

Atajo para lambdas que solo llaman a un método existente.

| Forma | Sintaxis | Equivalente Lambda |
|-------|----------|-------------------|
| Static method | `ClassName::staticMethod` | `args → ClassName.staticMethod(args)` |
| Instance method | `instance::method` | `args → instance.method(args)` |
| Constructor | `ClassName::new` | `args → new ClassName(args)` |

**Ejemplos en el proyecto:**

```java
// Static method reference (ApkReader):
.map(p -> p.getName())  // → podría ser .map(ApkPermission::getName)
// pero en el proyecto se usa lambda explícita.

// Instance method reference (shutdown hook):
Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
// Equivalente a: new Thread(() -> this.stop())

// Constructor reference:
// (no usado directamente, pero válido)
```

---

## Closures (Captura de Variables)

Las lambdas pueden acceder a variables del ámbito donde se definen, siempre que sean **effectively final** (no reasignadas después de la captura).

```java
// En buildChain():
for (int i = middlewares.size() - 1; i >= 0; i--) {
    Middleware mw = middlewares.get(i);
    MiddlewareChain next = chain;       // effectively final
    chain = req -> mw.run(req, next);   // captura mw y next
}
// Cada iteración captura sus propias variables mw y next (diferentes para cada lambda)
```

Otro ejemplo — captura de `chain` y luego `middleware`:

```java
// CorsMiddleware como expresión fluida:
server.use(new CorsMiddleware().allowOrigin("*"));
// La lambda registrada captura la configuración del middleware.
```

---

## Stream API (Java 8+)

Secuencia de elementos que soporta operaciones funcionales estilo map/filter/reduce.

### stream()

Convierte una colección en un stream:

```java
meta.getUsesPermissions().stream()
    .map(p -> p.getName())
    .collect(Collectors.toList());
```

### map()

Transforma cada elemento:

```java
.stream()
.map(p -> p.getName())           // ApkPermission → String
```

### filter()

Selecciona elementos que cumplen una condición:

```java
.stream()
.filter(k -> k.equalsIgnoreCase("Content-Encoding"))
```

### forEach()

Ejecuta una acción por cada elemento:

```java
info.permissions().forEach(p -> sb.append("  - ").append(p).append("\n"));
// Uso directo en Collection (no necesita .stream() para forEach)
```

### collect()

Convierte el stream de vuelta a una colección:

```java
.collect(Collectors.toList())  // → List
```

### anyMatch() — Short-circuit

```java
boolean alreadyEncoded = response.getHeaders().keySet().stream()
    .anyMatch(k -> k.equalsIgnoreCase("Content-Encoding"));
// → true si algún header coincide
```

### Stream en HttpDecoder

```java
// (implícito) — recorrer headers map
for (var entry : response.getHeaders().entrySet()) {
    builder.addHeader(entry.getKey(), entry.getValue());
}
```

### toList() (Java 16+)

Alternativa directa a `collect(Collectors.toList())`:

```java
stream.toList()  // → lista inmutable
```

---

## Collector & Collectors

### Collectors.toList()

Acumula elementos en un `List`:

```java
.collect(Collectors.toList())
```

### Collectors.joining()

Concatena strings:

```java
String.join(", ", allowMethods)  // alternativa sin stream
```

---

## Double-Colon Operator (::) for Iteration

```java
// En AccessLogMiddleware.drainTo:
queue.drainTo(lines);  // no es stream, pero usa el concepto
lines.forEach(line -> writer.println(line));  // Consumer lambda
```

---

## Notas Adicionales

### Lambda vs Anonymous Class

Las lambdas son más concisas y no crean un archivo .class separado. Sintácticamente equivalentes a anonymous classes de functional interfaces.

### Effectively Final

Desde Java 8, las variables capturadas en lambdas no necesitan ser declaradas `final` explícitamente — solo no pueden reasignarse después de la captura.

### Streams son Lazy

Las operaciones intermedias (`map`, `filter`) son perezosas — no se ejecutan hasta que una operación terminal (`collect`, `forEach`, `anyMatch`) las dispara.

---

[← Anterior](io-networking.md) · [Siguiente →](exceptions-time.md)  
[🇪🇸 Español](streams-lambdas.md)
