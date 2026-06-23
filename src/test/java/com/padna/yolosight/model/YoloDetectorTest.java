package com.padna.yolosight.model;

import org.junit.jupiter.api.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for YoloDetector.
 * Shares the ModelManager from ModelManagerTest setup.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class YoloDetectorTest {

    private ModelManager modelManager;
    private YoloDetector detector;

    @BeforeAll
    void setUp() throws Exception {
        modelManager = new ModelManager();
        modelManager.loadModel();
        detector = new YoloDetector(modelManager, 0.3f); // low threshold for test
    }

    @AfterAll
    void tearDown() {
        if (modelManager != null) {
            modelManager.close();
        }
    }

    @Test
    @Order(1)
    void detect_simpleImage_returnsList() throws Exception {
        // Create a simple test image (solid gray 640x640 — no objects expected,
        // but the inference pipeline should complete without errors)
        BufferedImage testImg = new BufferedImage(640, 480, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = testImg.createGraphics();
        g.setColor(new Color(128, 128, 128));
        g.fillRect(0, 0, 640, 480);
        g.dispose();

        List<DetectionResult> results = detector.detect(testImg);
        assertNotNull(results);
        // A blank gray image may or may not have detections — either is valid
        // The key is that it doesn't throw
    }

    @Test
    @Order(2)
    void detect_colorfulImage_runsWithoutError() throws Exception {
        // Create an image with colored rectangles that might trigger weak detections
        BufferedImage testImg = new BufferedImage(640, 480, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = testImg.createGraphics();
        // Sky-like background
        g.setColor(new Color(135, 206, 235));
        g.fillRect(0, 0, 640, 480);
        // Green "grass"
        g.setColor(new Color(34, 139, 34));
        g.fillRect(0, 350, 640, 130);
        // Brown rectangle (might look like something)
        g.setColor(new Color(139, 69, 19));
        g.fillRect(200, 150, 100, 200);
        g.dispose();

        List<DetectionResult> results = detector.detect(testImg);
        assertNotNull(results);

        // Verify results are sorted by confidence descending
        for (int i = 0; i < results.size() - 1; i++) {
            assertTrue(results.get(i).confidence() >= results.get(i + 1).confidence(),
                    "Results should be sorted by confidence descending");
        }
    }

    @Test
    @Order(3)
    void detect_withHighThreshold_returnsFewerResults() throws Exception {
        YoloDetector strictDetector = new YoloDetector(modelManager, 0.9f);

        BufferedImage testImg = new BufferedImage(640, 480, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = testImg.createGraphics();
        g.setColor(new Color(135, 206, 235));
        g.fillRect(0, 0, 640, 480);
        g.setColor(new Color(34, 139, 34));
        g.fillRect(0, 350, 640, 130);
        g.setColor(new Color(139, 69, 19));
        g.fillRect(200, 150, 100, 200);
        g.dispose();

        List<DetectionResult> lowThreshold = detector.detect(testImg);
        List<DetectionResult> highThreshold = strictDetector.detect(testImg);

        assertNotNull(highThreshold);
        // With a higher threshold, we should never have MORE results
        assertTrue(highThreshold.size() <= lowThreshold.size());
    }
}
