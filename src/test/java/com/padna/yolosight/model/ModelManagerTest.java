package com.padna.yolosight.model;

import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.output.DetectedObjects;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for ModelManager.
 * Requires network on first run to download the model (~6 MB).
 * Uses @TestInstance(Lifecycle.PER_CLASS) to share the loaded model across tests.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ModelManagerTest {

    private ModelManager modelManager;

    @BeforeAll
    void setUp() throws Exception {
        modelManager = new ModelManager();
        assertFalse(modelManager.isLoaded());
        modelManager.loadModel();
    }

    @AfterAll
    void tearDown() {
        if (modelManager != null) {
            modelManager.close();
        }
    }

    @Test
    @Order(1)
    void modelIsLoaded_afterLoadModel() {
        assertTrue(modelManager.isLoaded());
    }

    @Test
    @Order(2)
    void newPredictor_returnsNonNull() {
        Predictor<Image, DetectedObjects> predictor = modelManager.newPredictor();
        assertNotNull(predictor);
        predictor.close();
    }

    @Test
    @Order(3)
    void loadModel_twice_isIdempotent() throws Exception {
        // Calling loadModel again should be a no-op
        modelManager.loadModel();
        assertTrue(modelManager.isLoaded());
    }
}
