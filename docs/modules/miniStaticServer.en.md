# Module: miniStaticServer

Static file server module built on top of miniJWS-core.

## Dependencies

- `miniJWS-core` (required)
- `miniQR` (optional — required only for QrStaticSite)

## Components

### StaticSite

A static file server that serves files from a directory.
Supports template variable substitution.

```java
StaticSite site = new StaticSite(8080, Path.of("data"));
site.addTemplate("serverIp", "192.168.1.100");
site.start();
site.idle();
```

Template placeholders in HTML use `{{variableName}}` syntax.

### QrStaticSite

Extends the static server concept with QR code generation support.
Replaces `{{placeholder}}` in HTML files with QR code SVGs.

```java
QrStaticSite site = new QrStaticSite(80, Path.of("data"));
String localIp = QrStaticSite.getLocalIp();
site.addQrPlaceholder("downloadQR", "http://" + localIp + "/app.apk", 250);
site.start();
```

This will scan the `data/` directory, create routes for all files,
and replace `{{downloadQR}}` in HTML with an inline QR code SVG.

## Features

- Automatic route generation from directory structure
- MIME type detection from file extensions
- Template variable substitution
- QR code injection for mobile-friendly downloads
- Thread-safe

---

[← Previous](miniQR.en.md) · [Next →](miniApkReader.en.md)  
[🇪🇸 Español](miniStaticServer.md) · [🇬🇧 English](miniStaticServer.en.md)
