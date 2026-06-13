# miniStaticServer

![Java 25+](https://img.shields.io/badge/Java-25+-orange?logo=openjdk&logoColor=white)
![Maven 3.8+](https://img.shields.io/badge/Maven-3.8+-C71A36?logo=apachemaven&logoColor=white)

Servidor de archivos estáticos sobre miniJWS-core con inyección de plantillas y soporte de códigos QR.

## API

### StaticSite

```java
StaticSite site = new StaticSite(8080, Path.of("data"));
site.addTemplate("serverIp", "192.168.1.100");
site.start();
site.idle();
```

Los placeholders en plantillas usan la sintaxis `{{variableName}}`.

### QrStaticSite

```java
QrStaticSite site = new QrStaticSite(80, Path.of("data"));
String localIp = QrStaticSite.getLocalIp();
site.addQrPlaceholder("downloadQR", "http://" + localIp + "/app.apk", 250);
site.start();
```

## Dependencias

| Módulo | Alcance |
|--------|---------|
| `miniJWS-core` | compile |
| `miniQR` | opcional (requerido para QrStaticSite) |

## Compilación

```bash
mvn clean install
```
