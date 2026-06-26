package com.padna.yolosight.model;

import ai.djl.Application;
import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import com.padna.yolosight.config.AppConfig;

import java.io.IOException;

/**
 * Manages the YOLO model lifecycle.
 */
public class ModelManager implements AutoCloseable {

    private ZooModel<Image, DetectedObjects> model;
    private final Criteria<Image, DetectedObjects> criteria;

    public ModelManager() {
        this.criteria = Criteria.builder()
                .setTypes(Image.class, DetectedObjects.class)
                .optApplication(Application.CV.OBJECT_DETECTION)
                .optArtifactId(AppConfig.MODEL_NAME)
                .optEngine("OnnxRuntime")
                .optProgress(new ProgressBar())
                .optOption("threshold", String.valueOf(AppConfig.DEFAULT_CONFIDENCE_THRESHOLD))
                .build();
    }

    public void loadModel() throws IOException, ModelNotFoundException,
            MalformedModelException {
        if (model == null) {
            this.model = criteria.loadModel();
        }
    }

    public Predictor<Image, DetectedObjects> newPredictor() {
        if (model == null) {
            throw new IllegalStateException("Model not loaded. Call loadModel() first.");
        }
        return model.newPredictor();
    }

    public boolean isLoaded() { return model != null; }

    @Override
    public void close() {
        if (model != null) { model.close(); }
    }
}
