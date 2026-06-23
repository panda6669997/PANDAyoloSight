package com.padna.yolosight.util;

import com.padna.yolosight.model.DetectionResult;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DrawingUtilsTest {

    @Test
    void drawDetections_emptyList_doesNotThrow() {
        BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
        DrawingUtils.drawDetections(g2d, new ArrayList<>());
        g2d.dispose();
        // Should not throw
    }

    @Test
    void drawDetections_nullList_doesNotThrow() {
        BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
        DrawingUtils.drawDetections(g2d, null);
        g2d.dispose();
    }

    @Test
    void drawDetections_singleBox_drawsWithoutError() {
        BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();

        List<DetectionResult> dets = List.of(
                new DetectionResult("person", 0.95f,
                        new Rectangle(50, 50, 100, 150), 0)
        );

        // Should not throw
        DrawingUtils.drawDetections(g2d, dets);
        g2d.dispose();
    }

    @Test
    void drawDetections_multipleClasses_differentColors() {
        // Verify that different classes get different colors
        Color c1 = DrawingUtils.getClassColor("person");
        Color c2 = DrawingUtils.getClassColor("car");
        Color c3 = DrawingUtils.getClassColor("person");

        assertNotEquals(c1, c2, "Different classes should have different colors");
        assertEquals(c1, c3, "Same class should have same color");
    }

    @Test
    void drawHighlighted_doesNotThrow() {
        BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();

        DetectionResult det = new DetectionResult("car", 0.88f,
                new Rectangle(30, 40, 80, 60), 2);

        DrawingUtils.drawHighlighted(g2d, det);
        g2d.dispose();
    }

    @Test
    void getClassColor_knownClass_isConsistent() {
        Color first = DrawingUtils.getClassColor("bicycle");
        Color second = DrawingUtils.getClassColor("bicycle");
        assertEquals(first, second);
    }

    @Test
    void drawDetections_labelClampedToTop_drawsInsideBox() {
        // If label would go above y=0 it should be drawn inside the box
        BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();

        // Box at very top — label would be at negative Y
        List<DetectionResult> dets = List.of(
                new DetectionResult("dog", 0.7f, new Rectangle(10, 0, 50, 50), 16)
        );

        DrawingUtils.drawDetections(g2d, dets);
        g2d.dispose();
        // No exception = pass
    }
}
