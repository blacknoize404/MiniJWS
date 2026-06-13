# Despliegue

## Compilar un Fat JAR

Para crear un JAR ejecutable con Maven, añade `maven-shade-plugin` al POM del módulo objetivo:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.5.1</version>
    <executions>
        <execution>
            <phase>package</phase>
            <goals><goal>shade</goal></goals>
            <configuration>
                <transformers>
                    <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                        <mainClass>io.github.blacknoize404.miniJWS.demo.DemoServer</mainClass>
                    </transformer>
                </transformers>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## Ejecutar como Servicio (Linux)

Crea un archivo de servicio systemd `/etc/systemd/system/minijws.service`:

```ini
[Unit]
Description=Servidor MiniJWS
After=network.target

[Service]
Type=simple
User=minijws
WorkingDirectory=/opt/minijws
ExecStart=/usr/bin/java -jar /opt/minijws/miniJWS-demo.jar 8080
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

## Ejecutar como Servicio (Windows)

Usa el Programador de Tareas o crea un archivo batch:

```batch
@echo off
java -jar miniJWS-demo.jar 8080
```

## Ajuste de Middleware

Para producción, configura el middleware adecuadamente:

```java
server.use(new AccessLogMiddleware("/var/log/minijws/access.log"));
server.use(new CorsMiddleware()
    .allowOrigin("https://miapp.com")
    .allowMethods("GET", "POST")
    .allowCredentials(true));
server.use(new RateLimitMiddleware(1000, 60));  // límite generoso
server.use(new GzipMiddleware(6));              // nivel por defecto
```

## Enlace de Puertos

- Puertos < 1024 requieren privilegios de root/Administrador
- Usa `80` para HTTP, `443` para HTTPS (aún no soportado)
- Recomendado: usar un proxy inverso (nginx, Apache) frente a MiniJWS

## Notas de Seguridad

- MiniJWS es un servidor HTTP/1.1 (sin HTTPS aún)
- Para producción, usa un proxy inverso con terminación TLS
- La limitación de tasa ayuda a prevenir DoS — ajusta según tu caso de uso
- El path traversal está bloqueado en `StaticFileHandler`
- La validación de entrada es mínima — valida en tus manejadores
- CORS puede restringirse a orígenes específicos en producción

---

[← Anterior](configuration.md) · [Inicio](../index.md)  
[🇪🇸 Español](deployment.md) · [🇬🇧 English](deployment.en.md)
