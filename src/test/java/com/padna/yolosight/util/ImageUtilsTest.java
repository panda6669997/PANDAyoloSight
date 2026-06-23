package com.padna.yolosight.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ImageUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    void loadImage_validPNG_returnsBGR() throws IOException {
        // Create a test PNG
        BufferedImage testImg = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = testImg.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, 100, 100);
        g.dispose();

        File file = tempDir.resolve("test.png").toFile();
        ImageIO.write(testImg, "png", file);

        BufferedImage loaded = ImageUtils.loadImage(file);
        assertNotNull(loaded);
        assertEquals(100, loaded.getWidth());
        assertEquals(100, loaded.getHeight());
        assertEquals(BufferedImage.TYPE_3BYTE_BGR, loaded.getType());
    }

    @Test
    void loadImage_unsupportedFile_throws() {
        File badFile = tempDir.resolve("notanimage.txt").toFile();
        assertThrows(IOException.class, () -> ImageUtils.loadImage(badFile));
    }

    @Test
    void convertToBGR_alreadyBGR_isNoOp() {
        BufferedImage bgr = new BufferedImage(64, 64, BufferedImage.TYPE_3BYTE_BGR);
        BufferedImage result = ImageUtils.convertToBGR(bgr);
        assertSame(bgr, result); // same instance returned
    }

    @Test
    void convertToBGR_convertsOtherType() {
        BufferedImage intRGB = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        BufferedImage result = ImageUtils.convertToBGR(intRGB);
        assertNotSame(intRGB, result);
        assertEquals(BufferedImage.TYPE_3BYTE_BGR, result.getType());
        assertEquals(64, result.getWidth());
        assertEquals(64, result.getHeight());
    }

    @Test
    void saveImage_roundTrip() throws IOException {
        BufferedImage original = new BufferedImage(50, 50, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = original.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, 50, 50);
        g.dispose();

        File outFile = tempDir.resolve("saved.png").toFile();
        ImageUtils.saveImage(original, "png", outFile);

        assertTrue(outFile.exists());
        BufferedImage reloaded = ImageIO.read(outFile);
        assertNotNull(reloaded);
        assertEquals(50, reloaded.getWidth());
        assertEquals(50, reloaded.getHeight());
    }

    @Test
    void formatFileSize_variousSizes() {
        assertEquals("500 B", ImageUtils.formatFileSize(500));
        assertEquals("1.0 KB", ImageUtils.formatFileSize(1024));
        assertEquals("1.5 KB", ImageUtils.formatFileSize(1536));
        assertEquals("1.0 MB", ImageUtils.formatFileSize(1024 * 1024));
        assertEquals("2.5 MB", ImageUtils.formatFileSize((long) (2.5 * 1024 * 1024)));
    }
}
