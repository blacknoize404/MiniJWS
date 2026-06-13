# Module: miniQR

QR code generation library using ZXing and JFreeSVG.

## Dependencies

- `com.google.zxing:core:3.5.3`
- `org.jfree:jfreesvg:3.4`

## API

### QRCodeGenerator

| Method | Description |
|--------|-------------|
| `generateQRCodeImage(text, size)` | Returns a `BufferedImage` QR code |
| `convertToSVG(image, width, height)` | Converts image to SVG string |
| `generateSVG(text, size)` | Direct QR → SVG generation |

### Usage

```java
// Generate QR as image
BufferedImage qr = QRCodeGenerator.generateQRCodeImage("https://example.com", 300);

// Generate QR as SVG string
String svg = QRCodeGenerator.generateSVG("https://example.com", 300);

// Use with miniStaticServer for template injection
QrStaticSite site = new QrStaticSite(8080, Path.of("data"));
site.addQrPlaceholder("myQR", "https://example.com", 300);
site.start();
```

## Error Correction

Uses `ErrorCorrectionLevel.L` (low) by default for maximum data capacity.

---

[← Previous](miniJWS-demo.en.md) · [Next →](miniStaticServer.en.md)  
[🇪🇸 Español](miniQR.md) · [🇬🇧 English](miniQR.en.md)
