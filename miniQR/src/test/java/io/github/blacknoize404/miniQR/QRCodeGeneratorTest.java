package io.github.blacknoize404.miniQR;

import com.google.zxing.WriterException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class QRCodeGeneratorTest {

    @Test
    void generateQRCodeImage_returnsNonNullImage() throws WriterException {
        var image = QRCodeGenerator.generateQRCodeImage("https://example.com", 200);
        assertNotNull(image);
    }

    @Test
    void generateQRCodeImage_correctSize() throws WriterException {
        var image = QRCodeGenerator.generateQRCodeImage("test", 300);
        assertEquals(300, image.getWidth());
        assertEquals(300, image.getHeight());
    }

    @Test
    void generateQRCodeImage_typeIsRgb() throws WriterException {
        var image = QRCodeGenerator.generateQRCodeImage("data", 100);
        assertEquals(BufferedImage.TYPE_INT_RGB, image.getType());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "https://example.com",
        "Hello World",
        "1234567890",
        ""
    })
    void generateQRCodeImage_acceptsVariousTexts(String text) throws WriterException {
        var image = QRCodeGenerator.generateQRCodeImage(text, 100);
        assertNotNull(image);
    }

    @Test
    void generateQRCodeImage_withSpecialCharacters() throws WriterException {
        var image = QRCodeGenerator.generateQRCodeImage("héllo wörld! @#$%", 150);
        assertNotNull(image);
    }

    @Test
    void generateQRCodeImage_withMinimalSize() throws WriterException {
        var image = QRCodeGenerator.generateQRCodeImage("x", 10);
        assertNotNull(image);
        assertTrue(image.getWidth() >= 10);
    }

    @Test
    void convertToSVG_returnsValidSvgString() throws WriterException {
        var image = QRCodeGenerator.generateQRCodeImage("https://example.com", 100);
        var svg = QRCodeGenerator.convertToSVG(image, 100, 100);
        assertNotNull(svg);
        assertTrue(svg.contains("<svg"));
        assertTrue(svg.contains("</svg>"));
    }

    @Test
    void convertToSVG_containsDimensions() throws WriterException {
        var image = QRCodeGenerator.generateQRCodeImage("test", 200);
        var svg = QRCodeGenerator.convertToSVG(image, 200, 200);
        assertTrue(svg.contains("width=\"200\"") || svg.contains("width='200'"));
        assertTrue(svg.contains("height=\"200\"") || svg.contains("height='200'"));
    }

    @Test
    void generateSVG_returnsValidSvg() throws WriterException {
        var svg = QRCodeGenerator.generateSVG("https://example.com", 150);
        assertNotNull(svg);
        assertTrue(svg.contains("<svg"));
        assertTrue(svg.contains("</svg>"));
    }

    @Test
    void generateSVG_smallSize() throws WriterException {
        var svg = QRCodeGenerator.generateSVG("data", 50);
        assertNotNull(svg);
        assertTrue(svg.contains("<svg"));
    }

    @Test
    void generateQRCodeImage_throwsOnNegativeSize() {
        assertThrows(IllegalArgumentException.class, () ->
            QRCodeGenerator.generateQRCodeImage("test", -1));
    }

    @Test
    void generateQRCodeImage_throwsOnZeroSize() {
        assertThrows(IllegalArgumentException.class, () ->
            QRCodeGenerator.generateQRCodeImage("test", 0));
    }
}
