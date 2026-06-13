# miniQR

![Java 25+](https://img.shields.io/badge/Java-25+-orange?logo=openjdk&logoColor=white)
![Maven 3.8+](https://img.shields.io/badge/Maven-3.8+-C71A36?logo=apachemaven&logoColor=white)
![ZXing 3.5.3](https://img.shields.io/badge/ZXing-3.5.3-4B8BBE)
![JFreeSVG 3.4](https://img.shields.io/badge/JFreeSVG-3.4-4B8BBE)

Módulo de generación de códigos QR usando ZXing y JFreeSVG.

## API

```java
// QR como BufferedImage
BufferedImage qr = QRCodeGenerator.generateQRCodeImage("https://example.com", 300);

// QR como cadena SVG
String svg = QRCodeGenerator.generateSVG("https://example.com", 300);

// Usar con miniStaticServer para inyección en plantillas
QrStaticSite site = new QrStaticSite(8080, Path.of("data"));
site.addQrPlaceholder("myQR", "https://example.com", 300);
site.start();
```

## Dependencias

- `com.google.zxing:core:3.5.3`
- `org.jfree:jfreesvg:3.4`

## Compilación

```bash
mvn clean install
```
