# Getting Started

## Prerequisites

- Java 25+
- Maven 3.8+
- Git (optional)

## Build Modules

Each module is a standalone Maven project. Build them in dependency order:

```bash
mvn clean install -f miniJWS-core/pom.xml
mvn clean install -f miniJWS-demo/pom.xml
mvn clean install -f miniQR/pom.xml
mvn clean install -f miniStaticServer/pom.xml
mvn clean install -f miniApkReader/pom.xml
```

**Build order:** `miniJWS-core` → `miniJWS-demo` → `miniQR` → `miniStaticServer` → `miniApkReader`

## Running the Demo

### Full demo server (recommended)

```bash
# Uses middleware, path params, static files, CORS, etc.
mvn compile exec:java -f miniJWS-demo/pom.xml
```

Then open http://localhost:8080 in your browser.

### Built-in demo (minimal)

```bash
mvn exec:java -f miniJWS-core/pom.xml \
  -Dexec.mainClass="io.github.blacknoize404.miniJWS.DemoServer"
```

### Static site server

```bash
java -cp "miniStaticServer/target/classes;miniJWS-core/target/classes" \
  io.github.blacknoize404.miniStaticServer.StaticSite 8080 public/
```

## Verify It Works

```bash
curl http://localhost:8080/
curl http://localhost:8080/hello
curl http://localhost:8080/hello?name=MiniJWS
curl http://localhost:8080/hello/World
curl http://localhost:8080/api/info
curl http://localhost:8080/echo
```

## Creating a Custom Server

```java
import io.github.blacknoize404.miniJWS.*;
import io.github.blacknoize404.miniJWS.primitives.*;
import io.github.blacknoize404.miniJWS.responses.HttpResponse;
import io.github.blacknoize404.miniJWS.middleware.*;

public class MyServer {
    public static void main(String[] args) throws Exception {
        HttpServer server = new HttpServer(8080);

        // Middleware
        server.use(new AccessLogMiddleware());
        server.use(new CorsMiddleware().allowOrigin("*"));

        // Routes
        server.addRoute(HttpMethod.GET, "/", req ->
            new HttpResponse.Builder()
                .setStatusCode(200)
                .setContentType(ContentType.HTML)
                .setBody("<h1>Welcome!</h1>")
                .build()
        );

        server.addRoute(HttpMethod.GET, "/hello/:name", req -> {
            String name = req.getParameters().getOrDefault("name", "World");
            return new HttpResponse.Builder()
                .setContentType(ContentType.TEXT)
                .setBody("Hello, " + name + "!")
                .build();
        });

        server.run();
    }
}
```

---

[← Home](../index.en.md) · [Next →](configuration.en.md)  
[🇪🇸 Español](getting-started.md) · [🇬🇧 English](getting-started.en.md)
