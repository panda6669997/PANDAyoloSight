package com.padna.yolosight.model;

import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.output.BoundingBox;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.DetectedObjects.DetectedObject;
import ai.djl.modality.cv.output.Rectangle;
import com.padna.yolosight.config.AppConfig;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs YOLO object detection inference on a single image.
 */
public class YoloDetector {

    private final ModelManager modelManager;
    private final float confidenceThreshold;

    public YoloDetector(ModelManager modelManager, float confidenceThreshold) {
        this.modelManager = modelManager;
        this.confidenceThreshold = confidenceThreshold;
    }

    public YoloDetector(ModelManager modelManager) {
        this(modelManager, AppConfig.DEFAULT_CONFIDENCE_THRESHOLD);
    }

    public List<DetectionResult> detect(BufferedImage bufferedImage)
            throws Exception {

        // Step 1: BufferedImage → DJL Image
        Image djlImage = ai.djl.modality.cv.ImageFactory.getInstance()
                .fromImage(bufferedImage);

        // Step 2: Run inference
        DetectedObjects detectedObjects;
        try (Predictor<Image, DetectedObjects> predictor =
                     modelManager.newPredictor()) {
            detectedObjects = predictor.predict(djlImage);
        }

        // Step 3: Convert DetectedObjects → List<DetectionResult>
        // YOLO output is in NORMALIZED coordinates [0.0–1.0] —
        // multiply by image dimensions to get pixel coordinates.
        int imgW = bufferedImage.getWidth();
        int imgH = bufferedImage.getHeight();

        List<DetectionResult> results = new ArrayList<>();
        if (detectedObjects != null) {
            for (Classifications.Classification item : detectedObjects.items()) {
                if (item.getProbability() >= confidenceThreshold
                        && item instanceof DetectedObject) {
                    DetectedObject detObj = (DetectedObject) item;
                    BoundingBox bbox = detObj.getBoundingBox();
                    Rectangle rect = bbox.getBounds();

                    // Denormalize: [0,1] → pixel coordinates
                    int bx = (int) (rect.getX() * imgW);
                    int by = (int) (rect.getY() * imgH);
                    int bw = (int) (rect.getWidth() * imgW);
                    int bh = (int) (rect.getHeight() * imgH);

                    results.add(new DetectionResult(
                            item.getClassName(),
                            (float) item.getProbability(),
                            new java.awt.Rectangle(bx, by, Math.max(bw, 1), Math.max(bh, 1)),
                            -1
                    ));
                }
            }
        }

        results.sort(DetectionResult.BY_CONFIDENCE);
        return results;
    }
}
