package com.padna.yolosight.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Image loading, conversion, and saving utilities.
 */
public final class ImageUtils {

    private ImageUtils() {
        // utility class
    }

    /**
     * Load an image file into a BufferedImage suitable for DJL processing.
     * Converts to TYPE_3BYTE_BGR which DJL handles efficiently.
     *
     * @param file the image file
     * @return a BufferedImage in TYPE_3BYTE_BGR format
     * @throws IOException if the file cannot be read or format is unsupported
     */
    public static BufferedImage loadImage(File file) throws IOException {
        BufferedImage original = ImageIO.read(file);
        if (original == null) {
            throw new IOException("Unsupported or corrupted image: " + file.getName());
        }
        return convertToBGR(original);
    }

    /**
     * Convert any BufferedImage to TYPE_3BYTE_BGR for DJL compatibility.
     */
    public static BufferedImage convertToBGR(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_3BYTE_BGR) {
            return source;
        }
        BufferedImage converted = new BufferedImage(
                source.getWidth(), source.getHeight(),
                BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = converted.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return converted;
    }

    /**
     * Save a BufferedImage to a file.
     *
     * @param image  the image to save
     * @param format e.g. "png", "jpg"
     * @param file   destination file
     * @throws IOException on write failure
     */
    public static void saveImage(BufferedImage image, String format, File file)
            throws IOException {
        boolean ok = ImageIO.write(image, format, file);
        if (!ok) {
            throw new IOException("No writer found for format: " + format);
        }
    }

    /**
     * Create a formatted file-size string (e.g. "2.3 MB").
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
