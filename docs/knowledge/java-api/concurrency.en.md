# Concurrency

## Thread

Basic unit of concurrent execution.

### Creation

```java
new Thread(() -> drainTo(pw), "access-log").start();
```

- **Parameter 1:** `Runnable` (lambda or `Runnable` instance)
- **Parameter 2:** thread name (useful for debugging)

### Daemon Threads

A thread that does not prevent the JVM from exiting. When all non-daemon threads finish, the JVM terminates immediately without waiting for daemon threads.

```java
this.worker = new Thread(() -> drainTo(pw), "access-log");
this.worker.setDaemon(true);
this.worker.start();
```

This allows the logging worker thread to not block the JVM on Ctrl+C.

### Thread Interruption

Cooperative mechanism to notify a thread that it should stop:

```java
worker.interrupt();  // sets the interrupt flag

// In the worker:
try {
    String line = queue.take();  // throws InterruptedException if flag is set
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();  // restores the flag (good practice)
    // exit loop
}
```

### join()

Waits for a thread to finish:

```java
worker.join(2_000);  // waits up to 2 seconds
```

---

## Thread Pools — ExecutorService

### Executors.newFixedThreadPool()

Creates a pool with a fixed number of threads. When all are busy, tasks are queued.

```java
this.threadPool = Executors.newFixedThreadPool(parallelism);
```

**Parameters:**
- `nThreads`: number of threads (default: `2 * CPU cores`)

**Usage in HttpServer:**
```java
threadPool.execute(() -> handleConnection(client));
```

The `execute(Runnable)` method queues the task. A pool thread will execute it when available.

### Pool Shutdown

```java
threadPool.shutdown();                                    // no new tasks, executes pending ones
threadPool.awaitTermination(5, TimeUnit.SECONDS);         // waits up to 5s
threadPool.shutdownNow();                                 // interrupts running tasks
```

---

## AtomicBoolean

Boolean variable with atomic operations and guaranteed visibility between threads.

```java
private final AtomicBoolean running = new AtomicBoolean(false);

running.set(true);              // write with memory barrier
running.get();                  // read with memory barrier
```

Without `AtomicBoolean`, writes to a normal `boolean` might not be visible to other threads (memory visibility problem).

---

## CountDownLatch

Synchronization barrier: one or more threads wait until a counter reaches zero.

```java
private final CountDownLatch shutdownLatch = new CountDownLatch(1);  // initial counter = 1
```

**await():**
```java
public void idle() {
    try {
        shutdownLatch.await();  // blocks until counter reaches 0
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}
```

**countDown():**
```java
public void stop() {
    shutdownLatch.countDown();  // decrements the counter (1 → 0), unblocks await()
    socket.close();
}
```

**Features:**
- Single-use (counter cannot be reset)
- Thread-safe without needing synchronized
- No missed signals or spurious wakeups (unlike `wait()/notify()`)

---

## BlockingQueue & LinkedBlockingQueue

Thread-safe queue that blocks on `take()` when empty.

```java
private final BlockingQueue<String> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
```

**offer():**
```java
queue.offer(logLine);  // non-blocking — returns false if queue is full
```

**take():**
```java
String line = queue.take();  // blocks until an element is available
```

**drainTo():**
```java
var lines = new ArrayList<String>();
queue.drainTo(lines);  // transfers all elements to the list
```

**Capacity:** `16_384` (bounded queue — provides backpressure).

---

## ConcurrentLinkedQueue

CAS-based (Compare-And-Swap) thread-safe queue, non-blocking.

Used by `RateLimitMiddleware` to keep request timestamps per IP:

```java
Queue<Instant> timestamps = new ConcurrentLinkedQueue<>();
timestamps.add(now);    // adds to the tail
timestamps.peek();      // retrieves head without removing
timestamps.poll();      // retrieves and removes head
timestamps.isEmpty();   // checks if empty
```

**vs LinkedBlockingQueue:**
- `ConcurrentLinkedQueue`: non-blocking, no capacity limit
- `LinkedBlockingQueue`: blocking, optional capacity

---

## synchronized

Mutual exclusion mechanism. Guarantees that only one thread executes the block at a time.

```java
synchronized (timestamps) {
    // only one thread at a time can execute this block
    while (!timestamps.isEmpty() && timestamps.peek().isBefore(cutoff)) {
        timestamps.poll();
    }
    if (timestamps.size() >= maxRequests) {
        return rateLimitResponse;
    }
    timestamps.add(now);
}
```

**Lock object:** any Java object can be used as a lock.

**Vs explicit Locks:**
- `synchronized` is simpler, the lock is automatically released when exiting the block
- `ReentrantLock` offers more flexibility (tryLock, fair lock, etc.)
- In `RateLimitMiddleware`, `synchronized` is used on the queue because it has clear semantics

---

## ExecutorService & Callable/Runnable

### Runnable

```java
@FunctionalInterface
public interface Runnable {
    void run();
}
```

Used for tasks without a return value: `Thread`, `threadPool.execute()`.

### Callable (not used directly in the project)

`Callable<V>` is like `Runnable` but can return a value and throw exceptions.

---

## CopyOnWriteArrayList

See [Generics & Collections](generics-collections.md#copyonwritearraylist).

Basically: a thread-safe list where write operations copy the entire array, while reads are lock-free.

---

## Shutdown Hooks

Hook registered in `Runtime` that executes when the JVM terminates (Ctrl+C, `System.exit()`, etc.).

```java
Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
```

**In AccessLogMiddleware:**
```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    this.worker.interrupt();
    try { this.worker.join(2_000); } catch (InterruptedException ignored) {}
    flushRemaining(writer);
}));
```

**Features:**
- Registered with `Runtime.getRuntime().addShutdownHook(Thread)`
- Execute concurrently when the JVM terminates
- Execution order is not guaranteed
- Maximum execution time is not guaranteed (JVM may terminate abruptly)

---

## Thread.currentThread()

Returns the `Thread` object of the current thread.

```java
Thread.currentThread().isInterrupted()  // is it interrupted?
Thread.currentThread().interrupt()      // restores interrupt flag
Thread.currentThread().getName()        // thread name ("access-log")
```

---

## volatile (Brief mention)

`AtomicBoolean` internally uses `volatile` to guarantee visibility. A `volatile` variable:
- Writes are immediately visible to other threads
- Provides a memory barrier (happens-before)
- Does not provide atomicity for compound operations (i++) — for that, use `AtomicInteger`

`volatile` is not used directly in the project (`AtomicBoolean` is preferred).

---

[← Previous](generics-collections.en.md) · [Next →](io-networking.en.md)  
[🇪🇸 Español](concurrency.md) · [🇬🇧 English](concurrency.en.md)
