# miniStaticServer

Static file server on top of miniJWS-core with template injection and QR code support.

## API

### StaticSite

```java
StaticSite site = new StaticSite(8080, Path.of("data"));
site.addTemplate("serverIp", "192.168.1.100");
site.start();
site.idle();
```

Template placeholders use `{{variableName}}` syntax.

### QrStaticSite

```java
QrStaticSite site = new QrStaticSite(80, Path.of("data"));
String localIp = QrStaticSite.getLocalIp();
site.addQrPlaceholder("downloadQR", "http://" + localIp + "/app.apk", 250);
site.start();
```

## Dependencies

| Module | Scope |
|--------|-------|
| `miniJWS-core` | compile |
| `miniQR` | optional (required for QrStaticSite) |

## Build

```bash
mvn clean install
```
