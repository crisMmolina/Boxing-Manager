package com.sigebox.central.ui.setup;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.awt.image.BufferedImage;
import java.util.Map;

/**
 * El QR de unión: {@code sigebox://join?h=…&p=…&c=<código>} (ADR 0004), tinta oscura sobre blanco y
 * margen de un módulo. Sin JavaFX, para poder leerlo de vuelta en las pruebas.
 */
public final class QrCode {

    /** Lado en píxeles, el mismo de la tarjeta del lugar. */
    public static final int SIZE = 168;

    static final int INK = 0xFF0F172A;
    static final int PAPER = 0xFFFFFFFF;

    private QrCode() {
    }

    public static BitMatrix matrix(String text, int size) {
        try {
            return new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size,
                    Map.of(EncodeHintType.MARGIN, 1));
        } catch (WriterException e) {
            throw new IllegalStateException("no se pudo generar el QR", e);
        }
    }

    public static BufferedImage image(String text, int size) {
        var matrix = matrix(text, size);
        var img = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < matrix.getHeight(); y++) {
            for (int x = 0; x < matrix.getWidth(); x++) {
                img.setRGB(x, y, matrix.get(x, y) ? INK : PAPER);
            }
        }
        return img;
    }
}
