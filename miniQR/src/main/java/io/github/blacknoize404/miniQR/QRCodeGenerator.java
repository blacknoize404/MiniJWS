package io.github.blacknoize404.miniQR;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.jfree.graphics2d.svg.SVGGraphics2D;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Hashtable;

public class QRCodeGenerator {

    Path path;

    public QRCodeGenerator(Path path) {
        this.path = path;
    }

    public static BufferedImage generateQRCodeImage(String text, int size) throws WriterException {

        Hashtable<EncodeHintType, Object> hintMap = new Hashtable<>();
        hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix byteMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, size, size, hintMap);
        int matrixSize = byteMatrix.getHeight();
        BufferedImage image = new BufferedImage(matrixSize, matrixSize, BufferedImage.TYPE_INT_RGB);
        image.createGraphics();

        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, matrixSize, matrixSize);
        graphics.setColor(Color.BLACK);

        for (int i = 0; i < matrixSize; i++) {
            for (int j = 0; j < matrixSize; j++) {
                if (byteMatrix.get(i, j)) {
                    graphics.fillRect(i, j, 1, 1);
                }
            }
        }
        return image;
    }

    public static String convertToSVG(BufferedImage image, int width, int height) {

        SVGGraphics2D g2 = new SVGGraphics2D(width, height);
        g2.drawImage(image, 0, 0, width, height, null);
        return g2.getSVGElement();
    }

    public static String generateSVG(String text, int size) throws WriterException {
        BufferedImage qrCodeImage = generateQRCodeImage(text, size);
        return convertToSVG(qrCodeImage, size, size);
    }
}
