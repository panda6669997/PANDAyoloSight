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
 * Manages the YOLO model lifecycle:
 * <ul>
 *   <li>First launch: downloads YOLOv8n (~6 MB) from DJL repository.</li>
 *   <li>Subsequent launches: loads from local cache (no network needed).</li>
 *   <li>Provides {@link #newPredictor()} for running inference.</li>
 * </ul>
 *
 * <p><b>Thread safety:</b> The model itself is thread-safe. Individual
 * {@link Predictor}s returned by {@link #newPredictor()} are NOT — each
 * inference call should use its own predictor and close it afterwards.</p>
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
                .optOption("optThreshold", String.valueOf(AppConfig.DEFAULT_CONFIDENCE_THRESHOLD))
                .optOption("optNmsThreshold", String.valueOf(AppConfig.DEFAULT_NMS_THRESHOLD))
                .build();
    }

    /**
     * Load the model into memory. On first call this downloads the model
     * file (~6 MB) from the DJL repository and caches it locally.
     * <p>
     * <b>Blocks</b> until download + load complete. Call from a background
     * thread to avoid freezing the GUI.
     * </p>
     *
     * @throws IOException              on network or filesystem errors
     * @throws ModelNotFoundException   if the model artifact is not found
     * @throws MalformedModelException  if the downloaded model is corrupted
     */
    public void loadModel() throws IOException, ModelNotFoundException,
            MalformedModelException {
        if (model == null) {
            this.model = criteria.loadModel();
        }
    }

    /**
     * Create a new Predictor for one inference call.
     * <b>Caller must close the returned predictor.</b>
     */
    public Predictor<Image, DetectedObjects> newPredictor() {
        if (model == null) {
            throw new IllegalStateException(
                    "Model not loaded. Call loadModel() first.");
        }
        return model.newPredictor();
    }

    /** @return true if the model has been loaded successfully. */
    public boolean isLoaded() {
        return model != null;
    }

    /** Release model resources. */
    @Override
    public void close() {
        if (model != null) {
            model.close();
        }
    }
}
