# Primeros Pasos

## Requisitos

- Java 25+
- Maven 3.8+
- Git (opcional)

## Compilar Módulos

Cada módulo es un proyecto Maven independiente. Compilar en orden de dependencias:

```bash
mvn clean install -f miniJWS-core/pom.xml
mvn clean install -f miniJWS-demo/pom.xml
mvn clean install -f miniQR/pom.xml
mvn clean install -f miniStaticServer/pom.xml
mvn clean install -f miniApkReader/pom.xml
```

**Orden de compilación:** `miniJWS-core` → `miniJWS-demo` → `miniQR` → `miniStaticServer` → `miniApkReader`

## Ejecutar la Demo

### Servidor de demostración completo (recomendado)

```bash
# Usa middleware, parámetros de ruta, archivos estáticos, CORS, etc.
mvn compile exec:java -f miniJWS-demo/pom.xml
```

Luego abre http://localhost:8080 en tu navegador.

### Demo integrada (mínima)

```bash
mvn exec:java -f miniJWS-core/pom.xml \
  -Dexec.mainClass="io.github.blacknoize404.miniJWS.DemoServer"
```

### Servidor de sitio estático

```bash
java -cp "miniStaticServer/target/classes;miniJWS-core/target/classes" \
  io.github.blacknoize404.miniStaticServer.StaticSite 8080 public/
```

## Verificar que Funciona

```bash
curl http://localhost:8080/
curl http://localhost:8080/hello
curl http://localhost:8080/hello?name=MiniJWS
curl http://localhost:8080/hello/World
curl http://localhost:8080/api/info
curl http://localhost:8080/echo
```

## Crear un Servidor Personalizado

```java
import io.github.blacknoize404.miniJWS.*;
import io.github.blacknoize404.miniJWS.primitives.*;
import io.github.blacknoize404.miniJWS.responses.HttpResponse;
import io.github.blacknoize404.miniJWS.middleware.*;

public class MiServidor {
    public static void main(String[] args) throws Exception {
        HttpServer server = new HttpServer(8080);

        // Middleware
        server.use(new AccessLogMiddleware());
        server.use(new CorsMiddleware().allowOrigin("*"));

        // Rutas
        server.addRoute(HttpMethod.GET, "/", req ->
            new HttpResponse.Builder()
                .setStatusCode(200)
                .setContentType(ContentType.HTML)
                .setBody("<h1>¡Bienvenido!</h1>")
                .build()
        );

        server.addRoute(HttpMethod.GET, "/hello/:name", req -> {
            String name = req.getParameters().getOrDefault("name", "Mundo");
            return new HttpResponse.Builder()
                .setContentType(ContentType.TEXT)
                .setBody("¡Hola, " + name + "!")
                .build();
        });

        server.run();
    }
}
```

---

[← Inicio](../index.md) · [Siguiente →](configuration.md)  
[🇪🇸 Español](getting-started.md) · [🇬🇧 English](getting-started.en.md)
