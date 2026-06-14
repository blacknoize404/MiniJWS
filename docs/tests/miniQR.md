# miniQR — Unit Tests

## Test Classes (1 file, 12 tests)

| Test Class | Source Under Test | Tests | What it evaluates |
|------------|-------------------|-------|-------------------|
| `QRCodeGeneratorTest` | `QRCodeGenerator` | 12 | QR image generation (non-null, correct size, RGB type); various text inputs (URL, plain text, digits, empty, special chars); minimal size; SVG conversion (valid XML, dimensions); `generateSVG` integration; negative/zero size throws |

## Running the Tests

```bash
cd miniQR
mvn test
```
