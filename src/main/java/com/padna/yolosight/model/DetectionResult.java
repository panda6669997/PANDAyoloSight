package com.padna.yolosight.model;

import java.awt.*;
import java.util.Comparator;

/**
 * Immutable record holding a single object detection result.
 *
 * @param className    human-readable class name, e.g. "person", "car"
 * @param confidence   detection confidence between 0.0 and 1.0
 * @param boundingBox  bounding box in pixel coordinates of the source image
 * @param classId      COCO class index, or -1 if unknown
 */
public record DetectionResult(
        String className,
        float confidence,
        Rectangle boundingBox,
        int classId
) {
    /** Format as "person (87.3%)" for display in labels. */
    public String toLabel() {
        return String.format("%s (%.1f%%)", className, confidence * 100);
    }

    /** Sort by confidence descending (most confident first). */
    public static final Comparator<DetectionResult> BY_CONFIDENCE =
            Comparator.comparingDouble(DetectionResult::confidence).reversed();
}
