# Módulo: miniQR

Biblioteca de generación de códigos QR usando ZXing y JFreeSVG.

## Dependencias

- `com.google.zxing:core:3.5.3`
- `org.jfree:jfreesvg:3.4`

## API

### QRCodeGenerator

| Método | Descripción |
|--------|-------------|
| `generateQRCodeImage(text, size)` | Devuelve un `BufferedImage` del código QR |
| `convertToSVG(image, width, height)` | Convierte imagen a cadena SVG |
| `generateSVG(text, size)` | Generación directa QR → SVG |

### Uso

```java
// Generar QR como imagen
BufferedImage qr = QRCodeGenerator.generateQRCodeImage("https://example.com", 300);

// Generar QR como cadena SVG
String svg = QRCodeGenerator.generateSVG("https://example.com", 300);

// Usar con miniStaticServer para inyección en plantillas
QrStaticSite site = new QrStaticSite(8080, Path.of("data"));
site.addQrPlaceholder("myQR", "https://example.com", 300);
site.start();
```

## Corrección de Errores

Usa `ErrorCorrectionLevel.L` (bajo) por defecto para máxima capacidad de datos.

---

[← Anterior](miniJWS-demo.md) · [Siguiente →](miniStaticServer.md)  
[🇪🇸 Español](miniQR.md) · [🇬🇧 English](miniQR.en.md)
