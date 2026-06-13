# API HttpServer

`io.github.blacknoize404.miniJWS.HttpServer`

La clase principal del servidor. Acepta conexiones TCP, ejecuta middleware y despacha solicitudes HTTP.

## Constructor

| Constructor | Descripción |
|-------------|-------------|
| `HttpServer(int port)` | Crea el servidor con `2 * núcleos de CPU` hilos |
| `HttpServer(int port, int parallelism)` | Crea el servidor con tamaño de pool personalizado |

## Métodos

| Método | Retorna | Descripción |
|--------|---------|-------------|
| `addRoute(HttpMethod, String, RequestRunner)` | `HttpServer` | Registrar un manejador de ruta (fluent) |
| `addStaticRoute(String, StaticFileHandler)` | `HttpServer` | Registrar ruta `/*` para archivos estáticos (fluent) |
| `removeRoute(HttpMethod, String)` | `void` | Desregistrar una ruta |
| `use(Middleware)` | `HttpServer` | Registrar middleware en el pipeline |
| `run()` | `void` | Iniciar el servidor (bloqueante, registra hook SIGINT) |
| `run(boolean addShutdownHook)` | `void` | Iniciar con hook de apagado opcional |
| `stop()` | `void` | Establecer running=false y cerrar el socket |
| `idle()` | `void` | Poner el hilo principal en espera (modo daemon) |

## Coincidencia de Rutas

Las rutas se emparejan en este orden:
1. **Coincidencia exacta** — `GET:/api/users` coincide con `/api/users`
2. **Parámetros de ruta** — `GET:/users/:id` coincide con `/users/42`
3. **Comodín simple** — `GET:/*` coincide con `/cualquier-segmento`
4. **Comodín global** — `GET:/assets/**` coincide con `/assets/a/b/c`

Los parámetros de ruta se almacenan en `HttpRequest.getParameters()`:
- `request.getParameters().get("id")` → `"42"`
- Los parámetros de query y de ruta se fusionan

### Formato de Clave de Ruta

Las rutas se almacenan internamente como `METHOD:/path`. La ruta se normaliza:
- Se elimina el `/` final (excepto para la raíz `/`)

## Middleware

El middleware se ejecuta en orden de registro. `use()` añade al final de la cadena.

```java
server.use(middleware1);  // se ejecuta primero
server.use(middleware2);  // se ejecuta segundo, tras middleware1 llamar a chain.next()
```

## Keep-Alive

HTTP/1.1 keep-alive está habilitado por defecto:
- Máximo 100 solicitudes por conexión
- Tiempo de inactividad: 10 segundos
- Respeta la cabecera `Connection: close`

## Seguridad de Hilos

- Las rutas usan `ConcurrentHashMap` — seguro modificar en ejecución
- La lista de middleware usa `CopyOnWriteArrayList` — seguro en ejecución
- Cada conexión se ejecuta en un hilo separado
- El pool de hilos usa `Executors.newFixedThreadPool()`

---

[← Inicio](../index.md) · [Siguiente →](http-request.md)  
[🇪🇸 Español](http-server.md) · [🇬🇧 English](http-server.en.md)
