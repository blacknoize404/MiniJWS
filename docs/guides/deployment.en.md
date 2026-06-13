# Deployment

## Building a Fat JAR

To create an executable JAR with Maven, add the `maven-shade-plugin` to the target module's POM:

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

## Running as a Service (Linux)

Create a systemd service file `/etc/systemd/system/minijws.service`:

```ini
[Unit]
Description=MiniJWS Server
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

## Running as a Service (Windows)

Use the Task Scheduler or create a batch file:

```batch
@echo off
java -jar miniJWS-demo.jar 8080
```

## Middleware Tuning

For production, configure middleware appropriately:

```java
server.use(new AccessLogMiddleware("/var/log/minijws/access.log"));
server.use(new CorsMiddleware()
    .allowOrigin("https://myapp.com")
    .allowMethods("GET", "POST")
    .allowCredentials(true));
server.use(new RateLimitMiddleware(1000, 60));  // generous limit
server.use(new GzipMiddleware(6));              // default level
```

## Port Binding

- Ports < 1024 require root/Administrator privileges
- Use `80` for HTTP, `443` for HTTPS (not yet supported)
- Recommended: use a reverse proxy (nginx, Apache) in front of MiniJWS

## Security Notes

- MiniJWS is an HTTP/1.1 server (no HTTPS yet)
- For production, use a reverse proxy with TLS termination
- Rate limiting helps prevent DoS — tune per your use case
- Path traversal is blocked in `StaticFileHandler`
- Input validation is minimal — validate in your handlers
- CORS can be restricted to specific origins in production

---

[← Previous](configuration.en.md) · [Home](../index.en.md)  
[🇪🇸 Español](deployment.md) · [🇬🇧 English](deployment.en.md)
