# Módulo: miniStaticServer

Módulo de servidor de archivos estáticos construido sobre miniJWS-core.

## Dependencias

- `miniJWS-core` (requerido)
- `miniQR` (opcional — requerido solo para QrStaticSite)

## Componentes

### StaticSite

Un servidor de archivos estáticos que sirve archivos desde un directorio.
Soporta sustitución de variables en plantillas.

```java
StaticSite site = new StaticSite(8080, Path.of("data"));
site.addTemplate("serverIp", "192.168.1.100");
site.start();
site.idle();
```

Los placeholders en plantillas HTML usan la sintaxis `{{variableName}}`.

### QrStaticSite

Extiende el concepto de servidor estático con soporte de generación de códigos QR.
Reemplaza `{{placeholder}}` en archivos HTML con SVGs de códigos QR.

```java
QrStaticSite site = new QrStaticSite(80, Path.of("data"));
String localIp = QrStaticSite.getLocalIp();
site.addQrPlaceholder("downloadQR", "http://" + localIp + "/app.apk", 250);
site.start();
```

Esto escaneará el directorio `data/`, creará rutas para todos los archivos,
y reemplazará `{{downloadQR}}` en HTML con un SVG de código QR en línea.

## Características

- Generación automática de rutas desde la estructura de directorios
- Detección de tipos MIME desde extensiones de archivo
- Sustitución de variables en plantillas
- Inyección de códigos QR para descargas compatibles con móviles
- Seguro para hilos

---

[← Anterior](miniQR.md) · [Siguiente →](miniApkReader.md)  
[🇪🇸 Español](miniStaticServer.md) · [🇬🇧 English](miniStaticServer.en.md)
