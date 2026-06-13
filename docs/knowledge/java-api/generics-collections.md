# Generics & Collections Framework

## Genéricos

Los genéricos permiten parametrizar tipos, proporcionando seguridad de tipos en tiempo de compilación.

### Type Parameters

```java
// K = Key, V = Value
Map<String, List<String>> headers = new HashMap<>();

// T = Type (convención)
List<Middleware> middlewares = new CopyOnWriteArrayList<>();

// E = Element
Queue<Instant> timestamps = new ConcurrentLinkedQueue<>();
```

### Wildcards

```java
// Unbounded wildcard
List<?> lista = ...;  // lista de cualquier tipo (solo lectura segura)

// Upper-bounded wildcard
void process(List<? extends Number> numbers);  // acepta List<Integer>, List<Double>, etc.
```

### Generic Methods

```java
public static <T> Optional<T> empty() {
    return Optional.empty();
}
```

### Raw Types (evitar)

```java
// MAL — raw type, sin tipo genérico:
List lista = new ArrayList();  // advertencia del compilador

// BIEN:
List<String> lista = new ArrayList<>();
```

---

## Collections Framework

### Map — Diccionario key→value

#### `HashMap<K, V>` — No ordenado, O(1) promedio

```java
Map<String, String> params = new HashMap<>();
params.put("key", "value");
params.get("key");          // → "value"
params.getOrDefault("id", "default");  // → "default" si no existe
params.containsKey("key");
```

#### `LinkedHashMap<K, V>` — Mantiene orden de inserción

```java
Map<String, List<String>> headers = new LinkedHashMap<>();
// El orden de iteración coincide con el orden en que se insertaron las entradas
```

Se usa en `HttpDecoder` para preservar el orden de los headers HTTP.

#### `ConcurrentHashMap<K, V>` — Thread-safe, alta concurrencia

```java
private final Map<String, RequestRunner> routes = new ConcurrentHashMap<>();
private final ConcurrentHashMap<String, Queue<Instant>> requests = new ConcurrentHashMap<>();
```

**Características:**
- No permite `null` key ni value
- Thread-safe sin locks globales (segment locking / CAS)
- `computeIfAbsent(key, fn)` — cálculo atómico si la key no existe
- `putIfAbsent()`, `remove()`, `replace()` con semántica atómica

**Uso en RateLimitMiddleware:**
```java
Queue<Instant> timestamps = requests.computeIfAbsent(ip, k -> {
    totalEntries.incrementAndGet();
    return new ConcurrentLinkedQueue<>();
});
```

#### `Map.entry()` — Par clave-valor inmutable (Java 9+)

```java
Map.entry("key", "value")  // → Map.Entry<String, String> inmutable
```

Usado dentro de `Map.ofEntries()` para crear cada entrada.

#### `Map.ofEntries()` — Factory para mapas inmutables (Java 9+)

```java
public static final Map<Integer, String> STATUS_CODES = Map.ofEntries(
    Map.entry(200, "OK"),
    Map.entry(404, "NOT_FOUND"),
    Map.entry(500, "INTERNAL_SERVER_ERROR")
);
```

#### `Map.of()` — Factory para mapas pequeños (Java 9+)

```java
Map.of();           // mapa vacío inmutable
Map.of("k1", "v1"); // un solo par
Map.of("k1", "v1", "k2", "v2");  // hasta 10 pares
```

### List — Lista ordenada

#### `ArrayList<E>` — Array dinámico

```java
List<String> methods = new ArrayList<>(List.of("GET", "POST", "PUT"));
methods.add("DELETE");
methods.get(0);  // → "GET"
```

**Arrays.asList():**
```java
List<String> fixed = Arrays.asList("a", "b", "c");  // tamaño fijo (no add/remove)
```

#### `List.of()` — Lista inmutable (Java 9+)

```java
List<String> list = List.of("a", "b", "c");  // no permite add/remove/set
```

#### `List.copyOf()` — Copia defensiva inmutable

```java
headersCopy.put(h.getKey(), List.copyOf(h.getValue()));  // snapshot inmutable
```

#### `CopyOnWriteArrayList<E>` — Thread-safe para lectura

```java
private final List<Middleware> middlewares = new CopyOnWriteArrayList<>();
```

**Características:**
- Sin locks en lectura (iteración sobre snapshot)
- Escritura: copia todo el array subyacente
- Ideal para casos read-heavy con pocas escrituras (como middleware list)

### Queue / Deque — Colas

#### `ConcurrentLinkedQueue<E>` — Cola thread-safe basada en CAS

```java
Queue<Instant> timestamps = new ConcurrentLinkedQueue<>();
timestamps.add(Instant.now());  // añade al final
timestamps.peek();              // mira el primero sin sacarlo
timestamps.poll();              // saca y devuelve el primero
timestamps.isEmpty();           // ¿vacía?
```

No bloqueante (sin capacidad máxima, sin bloqueo en operaciones).

### BlockingQueue — Cola con bloqueo

Ver [Concurrency](concurrency.md) para detalles de `LinkedBlockingQueue`.

### Iteración sobre Colecciones

#### For-Each (enhanced for loop)

```java
for (var entry : routes.entrySet()) { ... }
for (String permission : permissions) { ... }
for (int i = 0; i < data.length(); i++) { ... }
```

#### EntrySet Iteration

```java
for (var it = requests.entrySet().iterator(); it.hasNext();) {
    var entry = it.next();
    // ...
    it.remove();  // seguro durante iteración
}
```

#### Stream.forEach()

```java
info.permissions().forEach(p -> sb.append("  - ").append(p).append("\n"));
```

### Stream → Collections

```java
meta.getUsesPermissions().stream()
    .map(p -> p.getName())
    .collect(Collectors.toList());
// equivalente a:
meta.getUsesPermissions().stream()
    .map(ApkPermission::getName)
    .toList();  // Java 16+
```

### `drainTo()`

Transfiere todos los elementos disponibles de una `BlockingQueue` a una colección:

```java
var lines = new java.util.ArrayList<String>();
queue.drainTo(lines);  // vacía la queue en la lista
```

---

## Interfaces Funcionales en `java.util.function`

Aunque no se usan directamente, las lambdas del proyecto implementan estas interfaces:

| Interfaz | Método | Uso en proyecto |
|----------|--------|----------------|
| `Consumer<T>` | `accept(T)` | `forEach()` |
| `Function<T,R>` | `apply(T)` | `stream().map()` |
| `Supplier<T>` | `get()` | `Optional.orElseGet()` |
| `Runnable` | `run()` | `Thread`, shutdown hooks |

---

## `Objects` Utility Class

```java
Objects.requireNonNull(httpMethod, "HTTP method must not be null");
// → lanza NullPointerException con el mensaje si es null
```

Útil para validar parámetros al inicio de métodos/constructores.

---

### `Hashtable<K, V>` — Legado thread-safe

Versión antigua y thread-safe de `HashMap`. Sus métodos están sincronizados (más lento que `ConcurrentHashMap`). No permite `null` key ni value.

```java
Hashtable<EncodeHintType, Object> hintMap = new Hashtable<>();
hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
```

**Usado en:** `QRCodeGenerator.java` — la API de ZXing `QRCodeWriter.encode()` acepta `Hashtable` como hint map.

**⚠ Diferencias con `HashMap`:**
| Característica | `HashMap` | `Hashtable` |
|---------------|-----------|-------------|
| Thread-safe | No | Sí (sincronizado) |
| Null keys | Sí | No |
| Null values | Sí | No |
| Iterador | fail-fast | fail-fast |
| Rendimiento | O(1) promedio | O(1) pero más lento por sync |

**⚠ Diferencias con `ConcurrentHashMap`:**
| Característica | `ConcurrentHashMap` | `Hashtable` |
|---------------|---------------------|-------------|
| Estrategia | Segment locking / CAS | `synchronized` en toda la tabla |
| Rendimiento | Mucho mejor en multihilo | Bajo en multihilo |
| Null keys/values | No | No |
| `computeIfAbsent()` | Sí | No |

---

## `Arrays` Utility Class

```java
Arrays.asList(...)        // convierte array a List (tamaño fijo)
Arrays.toString(array)    // representación del array
Arrays.fill(array, val)   // llena array con valor
```

---

## `Collections` Utility Class

```java
Collections.emptyList()   // lista vacía inmutable
Collections.singletonList(elem)  // lista de un elemento inmutable
Collections.unmodifiableList(list)  // vista inmutable
```

---

[← Anterior](oop.md) · [Siguiente →](concurrency.md)  
[🇪🇸 Español](generics-collections.md)
