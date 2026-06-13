# miniQR

QR code generation module using ZXing and JFreeSVG.

## API

```java
// QR as BufferedImage
BufferedImage qr = QRCodeGenerator.generateQRCodeImage("https://example.com", 300);

// QR as SVG string
String svg = QRCodeGenerator.generateSVG("https://example.com", 300);

// Use with miniStaticServer for template injection
QrStaticSite site = new QrStaticSite(8080, Path.of("data"));
site.addQrPlaceholder("myQR", "https://example.com", 300);
site.start();
```

## Dependencies

- `com.google.zxing:core:3.5.3`
- `org.jfree:jfreesvg:3.4`

## Build

```bash
mvn clean install
```
