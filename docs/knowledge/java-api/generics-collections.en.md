# Generics & Collections Framework

## Generics

Generics allow parameterizing types, providing type safety at compile time.

### Type Parameters

```java
// K = Key, V = Value
Map<String, List<String>> headers = new HashMap<>();

// T = Type (convention)
List<Middleware> middlewares = new CopyOnWriteArrayList<>();

// E = Element
Queue<Instant> timestamps = new ConcurrentLinkedQueue<>();
```

### Wildcards

```java
// Unbounded wildcard
List<?> list = ...;  // list of any type (safe read-only)

// Upper-bounded wildcard
void process(List<? extends Number> numbers);  // accepts List<Integer>, List<Double>, etc.
```

### Generic Methods

```java
public static <T> Optional<T> empty() {
    return Optional.empty();
}
```

### Raw Types (avoid)

```java
// BAD — raw type, no generic type:
List list = new ArrayList();  // compiler warning

// GOOD:
List<String> list = new ArrayList<>();
```

---

## Collections Framework

### Map — Dictionary key→value

#### `HashMap<K, V>` — Not ordered, O(1) average

```java
Map<String, String> params = new HashMap<>();
params.put("key", "value");
params.get("key");          // → "value"
params.getOrDefault("id", "default");  // → "default" if not exists
params.containsKey("key");
```

#### `LinkedHashMap<K, V>` — Maintains insertion order

```java
Map<String, List<String>> headers = new LinkedHashMap<>();
// Iteration order matches the order in which entries were inserted
```

Used in `HttpDecoder` to preserve HTTP header order.

#### `ConcurrentHashMap<K, V>` — Thread-safe, high concurrency

```java
private final Map<String, RequestRunner> routes = new ConcurrentHashMap<>();
private final ConcurrentHashMap<String, Queue<Instant>> requests = new ConcurrentHashMap<>();
```

**Features:**
- Does not allow `null` key or value
- Thread-safe without global locks (segment locking / CAS)
- `computeIfAbsent(key, fn)` — atomic computation if key is absent
- `putIfAbsent()`, `remove()`, `replace()` with atomic semantics

**Usage in RateLimitMiddleware:**
```java
Queue<Instant> timestamps = requests.computeIfAbsent(ip, k -> {
    totalEntries.incrementAndGet();
    return new ConcurrentLinkedQueue<>();
});
```

#### `Map.entry()` — Immutable key-value pair (Java 9+)

```java
Map.entry("key", "value")  // → Map.Entry<String, String> immutable
```

Used inside `Map.ofEntries()` to create each entry.

#### `Map.ofEntries()` — Factory for immutable maps (Java 9+)

```java
public static final Map<Integer, String> STATUS_CODES = Map.ofEntries(
    Map.entry(200, "OK"),
    Map.entry(404, "NOT_FOUND"),
    Map.entry(500, "INTERNAL_SERVER_ERROR")
);
```

#### `Map.of()` — Factory for small maps (Java 9+)

```java
Map.of();           // empty immutable map
Map.of("k1", "v1"); // single pair
Map.of("k1", "v1", "k2", "v2");  // up to 10 pairs
```

### List — Ordered list

#### `ArrayList<E>` — Dynamic array

```java
List<String> methods = new ArrayList<>(List.of("GET", "POST", "PUT"));
methods.add("DELETE");
methods.get(0);  // → "GET"
```

**Arrays.asList():**
```java
List<String> fixed = Arrays.asList("a", "b", "c");  // fixed size (no add/remove)
```

#### `List.of()` — Immutable list (Java 9+)

```java
List<String> list = List.of("a", "b", "c");  // does not allow add/remove/set
```

#### `List.copyOf()` — Defensive immutable copy

```java
headersCopy.put(h.getKey(), List.copyOf(h.getValue()));  // immutable snapshot
```

#### `CopyOnWriteArrayList<E>` — Thread-safe for reading

```java
private final List<Middleware> middlewares = new CopyOnWriteArrayList<>();
```

**Features:**
- No locks on reads (iteration over snapshot)
- Write: copies the entire underlying array
- Ideal for read-heavy scenarios with few writes (like middleware list)

### Queue / Deque

#### `ConcurrentLinkedQueue<E>` — CAS-based thread-safe queue

```java
Queue<Instant> timestamps = new ConcurrentLinkedQueue<>();
timestamps.add(Instant.now());  // adds to the tail
timestamps.peek();              // retrieves head without removing
timestamps.poll();              // retrieves and removes head
timestamps.isEmpty();           // is it empty?
```

Non-blocking (no maximum capacity, no blocking on operations).

### BlockingQueue — Blocking queue

See [Concurrency](concurrency.md) for details on `LinkedBlockingQueue`.

### Iterating over Collections

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
    it.remove();  // safe during iteration
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
// equivalent to:
meta.getUsesPermissions().stream()
    .map(ApkPermission::getName)
    .toList();  // Java 16+
```

### `drainTo()`

Transfers all available elements from a `BlockingQueue` to a collection:

```java
var lines = new java.util.ArrayList<String>();
queue.drainTo(lines);  // empties the queue into the list
```

---

## Functional Interfaces in `java.util.function`

Although not used directly, the project's lambdas implement these interfaces:

| Interface | Method | Usage in project |
|-----------|--------|----------------|
| `Consumer<T>` | `accept(T)` | `forEach()` |
| `Function<T,R>` | `apply(T)` | `stream().map()` |
| `Supplier<T>` | `get()` | `Optional.orElseGet()` |
| `Runnable` | `run()` | `Thread`, shutdown hooks |

---

## `Objects` Utility Class

```java
Objects.requireNonNull(httpMethod, "HTTP method must not be null");
// → throws NullPointerException with the message if null
```

Useful for validating parameters at the start of methods/constructors.

---

### `Hashtable<K, V>` — Legacy thread-safe

Old thread-safe version of `HashMap`. Its methods are synchronized (slower than `ConcurrentHashMap`). Does not allow `null` key or value.

```java
Hashtable<EncodeHintType, Object> hintMap = new Hashtable<>();
hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
```

**Used in:** `QRCodeGenerator.java` — the ZXing `QRCodeWriter.encode()` API accepts `Hashtable` as a hint map.

**⚠ Differences from `HashMap`:**
| Feature | `HashMap` | `Hashtable` |
|---------|-----------|-------------|
| Thread-safe | No | Yes (synchronized) |
| Null keys | Yes | No |
| Null values | Yes | No |
| Iterator | fail-fast | fail-fast |
| Performance | O(1) average | O(1) but slower due to sync |

**⚠ Differences from `ConcurrentHashMap`:**
| Feature | `ConcurrentHashMap` | `Hashtable` |
|---------|---------------------|-------------|
| Strategy | Segment locking / CAS | `synchronized` on entire table |
| Performance | Much better in multithreaded | Poor in multithreaded |
| Null keys/values | No | No |
| `computeIfAbsent()` | Yes | No |

---

## `Arrays` Utility Class

```java
Arrays.asList(...)        // converts array to List (fixed size)
Arrays.toString(array)    // array representation
Arrays.fill(array, val)   // fills array with value
```

---

## `Collections` Utility Class

```java
Collections.emptyList()   // empty immutable list
Collections.singletonList(elem)  // single-element immutable list
Collections.unmodifiableList(list)  // immutable view
```

---

[← Previous](oop.en.md) · [Next →](concurrency.en.md)  
[🇪🇸 Español](generics-collections.md) · [🇬🇧 English](generics-collections.en.md)
