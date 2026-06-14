# Concurrency

## Thread

Unidad básica de ejecución concurrente.

### Creación

```java
new Thread(() -> drainTo(pw), "access-log").start();
```

- **Parámetro 1:** `Runnable` (lambda o instancia de `Runnable`)
- **Parámetro 2:** nombre del hilo (útil para debugging)

### Daemon Threads

Hilo que no impide la salida de la JVM. Cuando todos los hilos no-daemon terminan, la JVM termina inmediatamente sin esperar a los daemon.

```java
this.worker = new Thread(() -> drainTo(pw), "access-log");
this.worker.setDaemon(true);
this.worker.start();
```

Esto permite que el worker thread de logging no bloquee la JVM al hacer Ctrl+C.

### Interrupción de Hilos

Mecanismo cooperativo para notificar a un hilo que debe detenerse:

```java
worker.interrupt();  // establece el flag de interrupción

// En el worker:
try {
    String line = queue.take();  // lanza InterruptedException si el flag está activo
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();  // restaura el flag (buena práctica)
    // salir del bucle
}
```

### join()

Espera a que un hilo termine:

```java
worker.join(2_000);  // espera hasta 2 segundos
```

---

## Thread Pools — ExecutorService

### Executors.newFixedThreadPool()

Crea un pool con un número fijo de hilos. Cuando todos están ocupados, las tareas se encolan.

```java
this.threadPool = Executors.newFixedThreadPool(parallelism);
```

**Parámetros:**
- `nThreads`: número de hilos (default: `2 * CPU cores`)

**Uso en HttpServer:**
```java
threadPool.execute(() -> handleConnection(client));
```

El método `execute(Runnable)` encola la tarea. Un hilo del pool la ejecutará cuando esté disponible.

### Shutdown del Pool

```java
threadPool.shutdown();                                    // no acepta nuevas tareas, ejecuta las pendientes
threadPool.awaitTermination(5, TimeUnit.SECONDS);         // espera hasta 5s
threadPool.shutdownNow();                                 // interrumpe las tareas en ejecución
```

---

## AtomicBoolean

Variable booleana con operaciones atómicas y visibilidad garantizada entre hilos.

```java
private final AtomicBoolean running = new AtomicBoolean(false);

running.set(true);              // escritura con barrera de memoria
running.get();                  // lectura con barrera de memoria
```

Sin `AtomicBoolean`, las escrituras a un `boolean` normal podrían no ser visibles para otros hilos (problema de visibilidad de memoria).

---

## CountDownLatch

Barrera de sincronización: uno o más hilos esperan hasta que un contador llegue a cero.

```java
private final CountDownLatch shutdownLatch = new CountDownLatch(1);  // contador inicial = 1
```

**await():**
```java
public void idle() {
    try {
        shutdownLatch.await();  // bloquea hasta que el contador llegue a 0
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}
```

**countDown():**
```java
public void stop() {
    shutdownLatch.countDown();  // decrementa el contador (1 → 0), desbloquea await()
    socket.close();
}
```

**Características:**
- De un solo uso (el contador no se reinicia)
- Thread-safe sin necesidad de synchronized
- No sufre de missed signals ni spurious wakeups (a diferencia de `wait()/notify()`)

---

## BlockingQueue & LinkedBlockingQueue

Cola thread-safe que bloquea al hacer `take()` cuando está vacía.

```java
private final BlockingQueue<String> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
```

**offer():**
```java
queue.offer(logLine);  // no bloquea — retorna false si la cola está llena
```

**take():**
```java
String line = queue.take();  // bloquea hasta que haya un elemento disponible
```

**drainTo():**
```java
var lines = new ArrayList<String>();
queue.drainTo(lines);  // transfiere todos los elementos a la lista
```

**Capacidad:** `16_384` (bounded queue — proporciona backpressure).

---

## ConcurrentLinkedQueue

Cola thread-safe basada en CAS (Compare-And-Swap), no bloqueante.

Usada por `RateLimitMiddleware` para mantener timestamps de requests por IP:

```java
Queue<Instant> timestamps = new ConcurrentLinkedQueue<>();
timestamps.add(now);    // añade al final
timestamps.peek();      // mira el primero
timestamps.poll();      // saca el primero
timestamps.isEmpty();   // verifica si vacía
```

**vs LinkedBlockingQueue:**
- `ConcurrentLinkedQueue`: no bloqueante, sin límite de capacidad
- `LinkedBlockingQueue`: bloqueante, con capacidad opcional

---

## synchronized

Mecanismo de exclusión mutua. Garantiza que solo un hilo ejecute el bloque a la vez.

```java
synchronized (timestamps) {
    // solo un hilo a la vez puede ejecutar este bloque
    while (!timestamps.isEmpty() && timestamps.peek().isBefore(cutoff)) {
        timestamps.poll();
    }
    if (timestamps.size() >= maxRequests) {
        return rateLimitResponse;
    }
    timestamps.add(now);
}
```

**Objeto lock:** cualquier objeto Java puede ser usado como lock.

**Vs Locks explícitos:**
- `synchronized` es más simple, el lock se libera automáticamente al salir del bloque
- `ReentrantLock` ofrece más flexibilidad (tryLock, fair lock, etc.)
- En `RateLimitMiddleware` se usa `synchronized` sobre la cola porque tiene semántica clara

---

## ExecutorService & Callable/Runnable

### Runnable

```java
@FunctionalInterface
public interface Runnable {
    void run();
}
```

Usado para tareas sin valor de retorno: `Thread`, `threadPool.execute()`.

### Callable (no usado directamente en el proyecto)

`Callable<V>` es como `Runnable` pero puede devolver un valor y lanzar excepciones.

---

## CopyOnWriteArrayList

Ver [Generics & Collections](generics-collections.md#copyon writearraylist).

Básicamente: list thread-safe donde las operaciones de escritura copian el array completo, mientras las lecturas son lock-free.

---

## Shutdown Hooks

Gancho registrado en `Runtime` que se ejecuta cuando la JVM termina (Ctrl+C, `System.exit()`, etc.).

```java
Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
```

**En AccessLogMiddleware:**
```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    this.worker.interrupt();
    try { this.worker.join(2_000); } catch (InterruptedException ignored) {}
    flushRemaining(writer);
}));
```

**Características:**
- Se registran con `Runtime.getRuntime().addShutdownHook(Thread)`
- Se ejecutan concurrentemente cuando la JVM termina
- Orden de ejecución no garantizado
- Tiempo máximo de ejecución no garantizado (JVM puede terminar abruptamente)

---

## Thread.currentThread()

Devuelve el objeto `Thread` del hilo actual.

```java
Thread.currentThread().isInterrupted()  // ¿está interrumpido?
Thread.currentThread().interrupt()      // restaura flag de interrupción
Thread.currentThread().getName()        // nombre del hilo ("access-log")
```

---

## volatile (Breve mención)

`AtomicBoolean` internamente usa `volatile` para garantizar visibilidad. Una variable `volatile`:
- Las escrituras son visibles inmediatamente para otros hilos
- Proporciona barrera de memoria (happens-before)
- No proporciona atomicidad para operaciones compuestas (i++) — para eso está `AtomicInteger`

No se usa `volatile` directamente en el proyecto (se prefiere `AtomicBoolean`).

---

[← Anterior](generics-collections.md) · [Siguiente →](io-networking.md)  
[🇪🇸 Español](concurrency.md) · [🇬🇧 English](concurrency.en.md)
