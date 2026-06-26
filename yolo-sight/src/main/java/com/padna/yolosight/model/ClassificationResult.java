package com.padna.yolosight.model;

import java.util.Collections;
import java.util.List;

/**
 * Result of classifying a single image by largest-detected-object.
 *
 * @param fileName        original file name (e.g. "photo001.jpg")
 * @param primaryClass    the class of the largest detected object, or "未分类"
 * @param confidence      confidence of the primary class (0.0 ~ 1.0), or 0
 * @param sourcePath      absolute path of the source file
 * @param targetPath      absolute path where the clean file was saved
 * @param totalDetections how many objects were detected in this image
 * @param success         true if the image was classified and saved
 * @param detections      all detection results for thumbnail annotation rendering
 */
public record ClassificationResult(
        String fileName,
        String primaryClass,
        float confidence,
        String sourcePath,
        String targetPath,
        int totalDetections,
        boolean success,
        List<DetectionResult> detections
) {
    /** e.g. "93.6%" */
    public String confidenceDisplay() {
        return success ? String.format("%.1f%%", confidence * 100) : "—";
    }

    /** e.g. "5 个物体" */
    public String detectionsDisplay() {
        return totalDetections + " 个";
    }

    /** Non-null detections list for safe iteration */
    public List<DetectionResult> detections() {
        return detections != null ? detections : Collections.emptyList();
    }
}
