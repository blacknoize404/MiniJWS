# Patrones de Diseño Usados en MiniJWS

No empecé este proyecto pensando "voy a usar estos patrones". Los patrones fueron apareciendo como la solución natural a problemas que me iba encontrando. Aquí están los que terminé usando, con ejemplos reales del código para que veas cómo se aplican en la práctica.

## Builder

**Dónde:** `HttpRequest.Builder`, `HttpResponse.Builder`

El problema era simple: una petición HTTP tiene muchos campos opcionales (cabeceras, cuerpo, cookies, parámetros...). Hacer un constructor con 10 parámetros era inviable, y tener 20 setters sin orden no era mucho mejor.

El Builder resuelve esto: cada setter devuelve `this`, y al final `build()` construye el objeto inmutable:

```java
new HttpResponse.Builder()
    .setStatusCode(200)
    .setContentType(ContentType.JSON)
    .setCookie("session", "abc123", 3600, "/", true)
    .setBody("{\"status\":\"ok\"}")
    .build();  // → devuelve un HttpResponse, ya no se puede modificar
```

El método `build()` llama al constructor privado y hace validaciones (por ejemplo, `Objects.requireNonNull` en el builder de `HttpRequest`).

**¿Por qué funcionó?** Porque las peticiones y respuestas HTTP son inmutables una vez construidas, y el Builder permite construirlas paso a paso sin exponer un objeto a medias.

---

## Chain of Responsibility (Middleware)

**Dónde:** `Middleware` + `MiddlewareChain` + `buildChain()` en `HttpServer`

Este es el patrón que más me gusta del proyecto. La idea es que cada middleware es un eslabón de una cadena. Cada uno recibe la petición y un objeto `chain` para pasar el control al siguiente:

```
Petición → Middleware1 → next() → Middleware2 → next() → Manejador → Respuesta
               ↑                                           ↓
               └─────── post-procesamiento ←──────────────┘
```

La implementación la encontré después de varios intentos. Terminé construyendo la cadena al revés (el último middleware registrado envuelve al manejador, el penúltimo envuelve al último, etc.):

```java
// HttpServer.java:171-199
MiddlewareChain terminal = req -> { /* route matching */ };
MiddlewareChain chain = terminal;
for (int i = middlewares.size() - 1; i >= 0; i--) {
    Middleware mw = middlewares.get(i);
    MiddlewareChain next = chain;
    chain = req -> mw.run(req, next);
}
return chain;
```

Es una idea sencilla pero potente: cada middleware puede:
- Cortocircuitar la cadena (devolver una respuesta sin llamar a `next()`)
- Modificar la petición antes de pasarla
- Modificar la respuesta al regresar
- Medir tiempos, loguear, comprobar permisos...

Si usara una lista simple de interceptores, no podría hacer fácilmente cosas como medir el tiempo de ejecución del manejador (necesito código antes Y después de `next()`).

**Las interfaces quedaron así:**

```java
@FunctionalInterface
interface Middleware {
    HttpResponse run(HttpRequest request, MiddlewareChain chain);
}

@FunctionalInterface
interface MiddlewareChain {
    HttpResponse next(HttpRequest request);
}
```

Al ser `@FunctionalInterface`, puedes escribir middleware con lambdas:

```java
server.use((request, chain) -> {
    long start = System.nanoTime();
    HttpResponse response = chain.next(request);
    long ms = (System.nanoTime() - start) / 1_000_000;
    System.out.println(request.getUri() + " → " + response.getStatusCode() + " (" + ms + "ms)");
    return response;
});
```

---

## Strategy

**Dónde:** `RequestRunner`

Los manejadores de ruta son intercambiables. Todos implementan la misma interfaz:

```java
@FunctionalInterface
interface RequestRunner {
    HttpResponse run(HttpRequest request);
}
```

Cada ruta almacena un `RequestRunner`. El servidor elige cuál ejecutar según la URL y el método, pero todos se usan igual. Esto permite que el middleware trate a todos los manejadores por igual y que puedas testear cada ruta de forma aislada.

---

## Inmutabilidad

**Dónde:** `HttpRequest`, `HttpResponse`

Ambas son efectivamente inmutables después de construirse:
- Todos los campos son `final`
- Las colecciones no se modifican tras la construcción
- No hay setters en el objeto construido
- `body` es `Optional<byte[]>` — el array es modificable en teoría, pero nunca se expone para escritura

**¿Por qué inmutables?** Porque el `HttpRequest` viaja por toda la cadena de middleware. Si un middleware modificara la petición, los siguientes recibirían datos cambiados sin saberlo. Con objetos inmutables, si un middleware quiere cambiar algo, crea una copia (usando el Builder) y pasa esa. El original queda intacto.

Además, la inmutabilidad elimina bugs de concurrencia: varios hilos pueden leer el mismo `HttpRequest` sin sincronización.

---

## Factory Methods

**Dónde:** `HttpResponse.redirect()`

```java
public static HttpResponse redirect(String location) {
    return redirect(location, 302);
}

public static HttpResponse redirect(String location, int statusCode) {
    return new HttpResponse.Builder()
            .setStatusCode(statusCode)
            .addHeader("Location", location)
            .build();
}
```

Podría haber puesto un constructor o un método estático genérico, pero `redirect()` es más expresivo. Cuando lees `HttpResponse.redirect("/login")` sabes exactamente qué hace sin tener que mirar los parámetros.

---

## Fluent Interface

**En todas partes:** los setters devuelven `this`

```java
server.use(new CorsMiddleware().allowOrigin("*").allowMethods("GET", "POST"));
server.addRoute(HttpMethod.GET, "/", handler).addRoute(HttpMethod.POST, "/", handler);
```

No es un patrón con nombre elegante, pero reduce mucho el código repetitivo y hace que la configuración se lea casi como lenguaje natural.

---

## Singleton (limitado)

**Dónde:** `HttpEncoder`, `HttpDecoder`, `HttpStatusCode`

No son singletons de manual de diseño (no hay gestión de instancia única), pero tienen constructores privados y solo métodos estáticos. Son clases utilitarias sin estado. El constructor privado evita que alguien las instancie sin sentido.

---

## Producer-Consumer

**Dónde:** `AccessLogMiddleware` — logging asíncrono

El logging de peticiones no debería ralentizar las respuestas. Para eso, el middleware mete las líneas de log en una cola y un hilo aparte las escribe en el archivo:

```java
private final BlockingQueue<String> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
private final Thread worker;

// Hilo de la petición (productor):
queue.offer(logLine);

// Hilo trabajador (consumidor):
while (!interrupted) {
    String line = queue.take();  // se bloquea si la cola está vacía
    writer.println(line);
}
```

La cola acotada (`16.384` entradas) evita que el productor se adelante demasiado al consumidor (backpressure). Si la cola se llena, `offer()` devuelve false y la línea se pierde, pero el hilo de la petición no se bloquea.

---

## Template Method / Inversion of Control

**Dónde:** `StaticSite` / `QrStaticSite`

La clase base `StaticSite` escanea un directorio y construye rutas automáticamente. `QrStaticSite` extiende ese comportamiento añadiendo la inyección de códigos QR. El bucle de escaneo es el mismo; lo que cambia es cómo se construye cada ruta. La clase base llama a un método que las subclases sobrescriben (o, en nuestro caso, reciben lambdas que hacen de callbacks).

---

[← Anterior](http-protocol.md) · [Siguiente →](decisions.md)  
[🇪🇸 Español](patterns.md) · [🇬🇧 English](patterns.en.md)
