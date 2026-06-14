# Decisiones de Diseño

Este documento recoge la justificación detrás de las decisiones clave de diseño en MiniJWS.

---

## 1. Diseño Plano Multimódulo (Sin POM Raíz)

**Decisión:** Cada módulo (`miniJWS-core`, `miniJWS-demo`, `miniQR`, etc.) es un proyecto Maven independiente con su propio `pom.xml`. No hay un POM raíz ni un directorio `modules/` padre.

**Justificación:**
- Los módulos pueden construirse independientemente sin un agregador padre
- Los pipelines de CI pueden construir solo el módulo modificado
- Cada módulo controla su propia versión, plugins y dependencias
- Importación IDE más simple (cada módulo se abre como su propio proyecto)

**Compromiso:** El orden de construcción debe gestionarse manualmente (`miniJWS-core` → `miniJWS-demo` → `miniQR` → `miniStaticServer` → `miniApkReader`).

---

## 2. Objetos de Petición/Respuesta Inmutables

**Decisión:** `HttpRequest` y `HttpResponse` son inmutables después de la construcción.

**Justificación:**
- Seguridad para hilos sin sincronización — las peticiones fluyen a través de cadenas de middleware en diferentes hilos
- Comportamiento predecible — ningún middleware puede modificar la petición después de que otro middleware la haya leído
- Fácil de cachear o reintentar

**Compromiso:** Copia-al-modificar es necesario cuando el middleware necesita alterar la petición (p.ej., `CorsMiddleware` y `findRouteWithParams` construyen nuevas instancias). Este costo de asignación es aceptable para cargas de trabajo HTTP.

---

## 3. Patrón Builder para Mensajes HTTP

**Decisión:** Usar el patrón Builder en lugar de constructores o setters en objetos mutables.

**Justificación:**
- Los campos opcionales no requieren constructores sobrecargados
- El encadenamiento fluido mejora la legibilidad
- `build()` aplica validación (`Objects.requireNonNull`) en un punto conocido
- El builder es mutable; el objeto construido no — separación limpia entre construcción y uso

---

## 4. Middleware como Cadena Enlazada (Envoltorio)

**Decisión:** Construir la cadena de middleware envolviendo cada middleware alrededor del anterior (último-envuelve-primero), en lugar de iterar un índice a través de una lista.

```java
// buildChain():
MiddlewareChain chain = terminal;
for (int i = middlewares.size() - 1; i >= 0; i--) {
    Middleware mw = middlewares.get(i);
    MiddlewareChain next = chain;
    chain = req -> mw.run(req, next);
}
```

**Justificación:**
- Sin variable de índice que gestionar durante la ejecución
- Funciona naturalmente con lambdas y clausuras
- El middleware puede cortocircuitar devolviendo sin llamar a `next()`
- Cada middleware decide cuándo/si llamar a `next()`, permitiendo procesamiento previo/posterior

---

## 5. Parseo Línea por Línea con BufferedInputStream (No NIO)

**Decisión:** `HttpDecoder` lee la petición HTTP usando semántica `BufferedInputStream.readLine()` implementada manualmente (byte a byte), en lugar de usar Java NIO (`ByteBuffer`, `Channel`) o parseadores de alto nivel.

**Justificación:**
- Semántica de bloqueo simple y predecible
- Sin gestión compleja de búferes
- Funciona correctamente con sockets keep-alive (a diferencia de `available()` que devuelve 0 para peticiones en pipeline rápido)
- Fácil de aplicar límites de longitud de línea y detectar entrada malformada

**Compromiso:** Más lento que NIO para rendimiento muy alto, pero adecuado para cargas de trabajo HTTP moderadas.

---

## 6. Bucle Keep-Alive en el Manejador de Conexión

**Decisión:** El método `handleConnection()` itera sobre hasta 100 peticiones en el mismo socket, en lugar de una petición por conexión.

**Justificación:**
- HTTP/1.1 usa conexiones persistentes por defecto
- Reduce la sobrecarga de handshake TCP
- El mismo hilo maneja todas las peticiones en un socket, mejorando la localidad
- Respeta `Connection: close` y el tiempo de espera de inactividad

**Detalles de implementación:**
- 100 peticiones máximas por conexión (`MAX_KEEPALIVE_REQUESTS`)
- Timeout de socket de 10 segundos (`KEEPALIVE_TIMEOUT_MS`)
- Establece `Connection: keep-alive` o `Connection: close` en cada respuesta
- Sale del bucle en peticiones malformadas, timeout o cabecera de cierre

---

## 7. CopyOnWriteArrayList para Middleware

**Decisión:** El campo `middlewares` usa `CopyOnWriteArrayList` en lugar de `ArrayList` o una lista sincronizada.

**Justificación:**
- El middleware normalmente se registra al inicio y rara vez se modifica en tiempo de ejecución
- Las operaciones de lectura superan con creces a las de escritura una vez que el servidor está en ejecución
- CopyOnWriteArrayList proporciona lecturas sin bloqueo para todas las evaluaciones de middleware
- El costo de copia-al-escribir se paga solo durante las llamadas `use()`, que es insignificante

---

## 8. CountDownLatch para Apagado Gradual

**Decisión:** Usar `CountDownLatch(1)` en `idle()`/`stop()` en lugar de `wait()`/`notify()`.

**Justificación:**
- `CountDownLatch` es un primitivo de concurrencia moderno de Java con semántica clara
- Sin riesgo de notificaciones perdidas o despertares espurios
- `idle()` bloquea el hilo principal hasta que se llama a `stop()` (mediante SIGINT o programáticamente)
- El latch es de un solo uso (cuenta regresiva 1→0), coincidiendo con el ciclo de vida

---

## 9. Logging Asíncrono con BlockingQueue

**Decisión:** `AccessLogMiddleware` escribe logs de forma asíncrona mediante una `BlockingQueue` y un hilo daemon dedicado.

**Justificación:**
- La E/S de log (especialmente a archivos) puede bloquear el hilo de la petición
- La cola acotada (`16_384`) proporciona contrapresión
- El hilo daemon no impide la salida de la JVM
- Un hook de apagado vacía las entradas restantes al cerrar

---

## 10. Enrutamiento con Comodines (`*` y `**`)

**Decisión:** Dos niveles de comodín — `*` para un segmento individual, `**` para todos los segmentos restantes.

**Justificación:**
- `*` se asigna limpiamente a manejadores de archivo (`/*` coincide con cualquier ruta de un nivel)
- `**` es necesario para servir recursivamente (`/assets/**` coincide con `/assets/css/main.css`)
- Orden de coincidencia: exacta → parámetros de ruta → `*` → `**` — asegura resolución predecible

---

## 11. Protección CORS `*` + Credenciales

**Decisión:** `allowCredentials(true)` lanza `IllegalStateException` si `allowOrigin("*")` está establecido.

**Justificación:**
- La especificación CORS prohíbe explícitamente `Access-Control-Allow-Origin: *` con credenciales
- Aplicar esto en tiempo de configuración (fail-fast) es mejor que producir silenciosamente cabeceras CORS inválidas en tiempo de ejecución

---

## 12. Bytes Sin Procesar para la Codificación del Cuerpo

**Decisión:** `HttpEncoder` escribe el cuerpo como bytes sin procesar directamente al `OutputStream`, no a través del escritor ASCII.

**Justificación:**
- El `BufferedWriter` con juego de caracteres `US-ASCII` corrompe bytes no ASCII (UTF-8 multibyte, bytes comprimidos gzip)
- Las cabeceras son seguras en ASCII, pero los cuerpos pueden ser datos binarios arbitrarios
- Solución: escribir cabeceras a través del escritor, vaciar, luego escribir bytes del cuerpo directamente mediante `outputStream.write(data)`

---

## 13. StaticFileHandler: Defensa en Profundidad

**Decisión:** La prevención de path traversal se implementa en dos niveles.

**Justificación:**
- Verificación explícita de `..` en la cadena de ruta sin procesar
- Verificación `Path.normalize()` + `startsWith(baseDir)` después de la resolución
- Captura de `NoSuchFileException` para condiciones de carrera (archivo eliminado entre la verificación `isFile()` y `readAllBytes()`)

---

## 14. Sin Dependencias Externas para el Núcleo

**Decisión:** `miniJWS-core` depende solo de `org.jetbrains:annotations` (para `@Nullable`/`@NotNull`). El núcleo tiene cero dependencias en tiempo de ejecución.

**Justificación:**
- El núcleo sin dependencias es más fácil de auditar, incrustar y distribuir
- Minimiza conflictos de classpath
- Las implementaciones de middleware y manejadores son opcionales (incluidas en el núcleo por conveniencia pero no como librerías separadas)

---

## 15. Pool de Hilos: Tamaño Fijo

**Decisión:** Usar `Executors.newFixedThreadPool()` con `2 * núcleos de CPU` como valor por defecto.

**Justificación:**
- El pool fijo evita el crecimiento ilimitado de hilos bajo carga
- 2× núcleos de CPU es un buen valor por defecto para servidores HTTP con E/S intensiva
- El tamaño del pool es configurable mediante el segundo parámetro del constructor
- El apagado usa `awaitTermination(5s)` antes de `shutdownNow()`

---

[← Anterior](patterns.md) · [Siguiente →](classes-core.md)  
[🇪🇸 Español](decisions.md) · [🇬🇧 English](decisions.en.md)
