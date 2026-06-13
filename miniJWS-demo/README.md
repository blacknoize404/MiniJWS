# miniJWS-demo

![Java 25+](https://img.shields.io/badge/Java-25+-orange?logo=openjdk&logoColor=white)
![Maven 3.8+](https://img.shields.io/badge/Maven-3.8+-C71A36?logo=apachemaven&logoColor=white)

Servidor de demostración completo mostrando todas las funcionalidades de miniJWS-core.

## Ejecutar

```bash
mvn compile exec:java
```

Abre en http://localhost:8080.

## Rutas

| Ruta | Descripción |
|------|-------------|
| `/` | 302 → `/index.html` |
| `/index.html` | Página estática desde `./public` |
| `/hello` | `¡Hola, Mundo!` |
| `/hello?name=X` | `¡Hola, X!` |
| `/hello/:name` | Parámetro de ruta — `¡Hola, {name}!` |
| `/api/info` | JSON con información del servidor |
| `POST /api/data` | Parsear cuerpo JSON o form |
| `/set-cookie` | Establecer una cookie |
| `/get-cookies` | Leer cookies como JSON |
| `/old-path` | 301 → `/new-path` |
| `/echo` | Eco de detalles de la solicitud |
| `/*` | Archivos estáticos desde `./public` |

## Middleware

| Middleware | Configuración |
|-----------|---------------|
| `AccessLogMiddleware` | Salida estándar |
| `CorsMiddleware` | `allowOrigin("*")` |
| `RateLimitMiddleware` | 200 req / 60 seg por IP |

## Compilación

```bash
mvn clean install
```
