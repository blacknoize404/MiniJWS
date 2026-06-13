# Arquitectura

## Visión General

MiniJWS sigue una arquitectura modular donde cada módulo es un proyecto Maven independiente
con una responsabilidad bien definida. Cada módulo es autocontenido con su propio POM.

## Grafo de Dependencias entre Módulos

```
miniJWS-core (sin dependencias)
    |
    ├── miniJWS-demo ──► miniJWS-core
    ├── miniStaticServer ──► miniJWS-core, miniQR (opcional)
    |
    miniApkReader (usa net.dongliu:apk-parser)
    |
    miniQR (usa com.google.zxing, org.jfree:jfreesvg)
```

## Arquitectura Central (miniJWS-core)

```
┌───────────────────────────────────────────────────────────────┐
│                         HttpServer                            │
│  ┌──────────┐  ┌──────────┐  ┌────────────┐  ┌──────────────┐ │
│  │  Rutas   │  │  Pool    │  │ServerSocket│  │ Lista de     │ │
│  │  (Map)   │  │  Hilos   │  │            │  │ Middleware   │ │
│  └────┬─────┘  └──────────┘  └────────────┘  └──────┬───────┘ │
│       │                                              │        │
│  ┌────▼──────────────────────────────────────────────▼──────┐ │
│  │                  Ciclo de Vida de Solicitud              │ │
│  │                                                          │ │
│  │  ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────┐  │ │
│  │  │ Decoder  │──►│Middleware│──►│  Runner  │──►│Encoder│  │ │
│  │  │ (parsear)│   │ (cadena) │   │  (ruta)  │   │(escr.)│  │ │
│  │  └──────────┘   └──────────┘   └──────────┘   └──────┘  │ │
│  └──────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────┘
```

### Flujo de Solicitud

1. **Aceptar**: `HttpServer` acepta una conexión TCP mediante `ServerSocket`
2. **Bucle Keep-Alive**: Si es HTTP/1.1 sin `Connection: close`, el socket se reutiliza
3. **Decodificar**: `HttpDecoder` parsea la solicitud HTTP cruda en un objeto `HttpRequest`
4. **Cadena de middleware**: Cada middleware registrado se ejecuta en orden (logging, CORS, limitación de tasa, etc.)
5. **Ruta**: Orden de coincidencia: exacta → parámetros de ruta (`:id`) → comodín simple (`*`) → comodín global (`**`)
6. **Ejecutar**: El `RequestRunner` correspondiente se invoca con la solicitud
7. **Codificar**: `HttpEncoder` serializa el `HttpResponse` de vuelta al cliente
8. **Repetir**: Si keep-alive, volver al paso 3; de lo contrario cerrar conexión

### Estructura del Paquete (miniJWS-core)

```
io.github.blacknoize404.miniJWS/
├── HttpServer.java              # Clase principal del servidor
├── DemoServer.java              # Ejemplo básico (legado)
├── primitives/
│   ├── HttpMethod.java          # Enum de métodos HTTP
│   ├── HttpStatusCode.java      # Definiciones de códigos de estado
│   ├── ContentType.java         # Enum de tipos MIME
│   ├── RequestRunner.java       # Interfaz de manejador de ruta
│   ├── Middleware.java          # Interfaz de middleware
│   └── MiddlewareChain.java     # Interfaz de cadena
├── requests/
│   ├── HttpRequest.java         # Modelo de solicitud (Builder, parseo de cuerpo, cookies)
│   └── HttpDecoder.java         # Parseador de solicitudes
├── responses/
│   ├── HttpResponse.java        # Modelo de respuesta (Builder, fábrica redirect, cookies)
│   └── HttpEncoder.java         # Serializador de respuestas
├── middleware/
│   ├── CorsMiddleware.java      # Cabeceras CORS y preflight
│   ├── AccessLogMiddleware.java # Logging de solicitudes estilo Apache
│   ├── GzipMiddleware.java      # Compresión de respuestas
│   └── RateLimitMiddleware.java # Limitación de tasa por IP
├── handlers/
│   └── StaticFileHandler.java   # Servicio de archivos basado en directorios
├── headers/
│   ├── Header.java              # Modelo de cabecera HTTP
│   ├── Field.java               # Parseador de campos de cabecera
│   └── Parameter.java           # Parseador de parámetros de cabecera
└── content/
    └── ContentTypes.java        # Mapeo extensión-a-MIME
```

### Pipeline de Middleware

El middleware se ejecuta en orden de registro. Cada middleware puede:
- Inspeccionar y modificar la solicitud
- Cortocircuitar la cadena (devolver una respuesta inmediatamente)
- Inspeccionar y modificar la respuesta
- Ejecutar código antes y después del manejador

```
Solicitud ──► Middleware 1 ──► Middleware 2 ──► ... ──► Manejador ──► Respuesta
                  │                │                          │
                  ▼                ▼                          ▼
            (log solicitud)  (verificar CORS)          (manejar ruta)
                  │                │                          │
                  ◄────────────────┼──────────────────────────┘
                                   │
                                   ▼
                            (agregar cabeceras CORS)
```

### Conexiones Keep-Alive

Las conexiones HTTP/1.1 son persistentes por defecto. El servidor:
- Reutiliza el mismo `BufferedInputStream` a través de múltiples solicitudes en el mismo socket
- Cierra después de la cabecera `Connection: close`, tiempo de inactividad (10s), o 100 solicitudes
- Establece `Connection: keep-alive` en cada respuesta

### Routing con Comodines

Las rutas pueden usar comodines para coincidencias flexibles:

- `*` — coincide con un segmento de ruta (p.ej. `/*` coincide con `/cualquier-segmento`)
- `**` — coincide con todos los segmentos restantes (p.ej. `/archivos/**` coincide con `/archivos/a/b/c`)

Usa `addStaticRoute()` por conveniencia al servir archivos estáticos con `/*`:

```java
server.addStaticRoute("/*", new StaticFileHandler("./public"));
```

### Seguridad de Hilos

- Las rutas usan `ConcurrentHashMap` — seguras para modificar en tiempo de ejecución
- La lista de middleware usa `CopyOnWriteArrayList` — segura para cargas de trabajo de mucha lectura
- Cada conexión se ejecuta en un hilo separado
- El pool de hilos usa `Executors.newFixedThreadPool()`
- El apagado gradual usa `CountDownLatch` — `idle()` espera el latch, `stop()` lo decrementa
