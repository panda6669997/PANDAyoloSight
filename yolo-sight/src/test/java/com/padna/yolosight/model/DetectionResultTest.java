package com.padna.yolosight.model;

import org.junit.jupiter.api.Test;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DetectionResultTest {

    @Test
    void toLabel_formatsCorrectly() {
        DetectionResult r = new DetectionResult("person", 0.873f,
                new Rectangle(10, 20, 100, 200), 0);
        assertEquals("person (87.3%)", r.toLabel());
    }

    @Test
    void toLabel_zeroConfidence() {
        DetectionResult r = new DetectionResult("car", 0.0f,
                new Rectangle(0, 0, 50, 50), 2);
        assertEquals("car (0.0%)", r.toLabel());
    }

    @Test
    void toLabel_fullConfidence() {
        DetectionResult r = new DetectionResult("dog", 1.0f,
                new Rectangle(0, 0, 50, 50), 16);
        assertEquals("dog (100.0%)", r.toLabel());
    }

    @Test
    void byConfidence_sortsDescending() {
        DetectionResult low = new DetectionResult("a", 0.3f,
                new Rectangle(0, 0, 10, 10), 0);
        DetectionResult mid = new DetectionResult("b", 0.5f,
                new Rectangle(0, 0, 10, 10), 1);
        DetectionResult high = new DetectionResult("c", 0.9f,
                new Rectangle(0, 0, 10, 10), 2);

        List<DetectionResult> list = new ArrayList<>(List.of(low, high, mid));
        list.sort(DetectionResult.BY_CONFIDENCE);

        assertEquals(0.9f, list.get(0).confidence());
        assertEquals(0.5f, list.get(1).confidence());
        assertEquals(0.3f, list.get(2).confidence());
    }

    @Test
    void recordAccessors_work() {
        Rectangle r = new Rectangle(100, 200, 300, 400);
        DetectionResult det = new DetectionResult("bicycle", 0.75f, r, 1);

        assertEquals("bicycle", det.className());
        assertEquals(0.75f, det.confidence());
        assertEquals(r, det.boundingBox());
        assertEquals(1, det.classId());
    }
}
