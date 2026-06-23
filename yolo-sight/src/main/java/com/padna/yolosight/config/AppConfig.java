package com.padna.yolosight.config;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Central configuration constants for the application.
 */
public final class AppConfig {

    private AppConfig() {
        // utility class — prevent instantiation
    }

    // ── Application metadata ───────────────────────────────────────────
    public static final String APP_NAME = "PANDAyoloSight";
    public static final String APP_VERSION = "1.0.0";

    // ── Model settings ──────────────────────────────────────────────────
    /** YOLOv8 nano variant (~6 MB), best trade-off of speed vs accuracy. */
    public static final String MODEL_NAME = "yolov8n";

    /** Local directory where DJL caches downloaded model files. */
    public static final Path MODEL_CACHE_DIR = Paths.get(
            System.getProperty("user.home"), ".yolosight", "models");

    // ── Detection defaults ──────────────────────────────────────────────
    /** Minimum confidence score to keep a detection (0.0 – 1.0). */
    public static final float DEFAULT_CONFIDENCE_THRESHOLD = 0.5f;

    /** IoU threshold for Non-Maximum Suppression. */
    public static final float DEFAULT_NMS_THRESHOLD = 0.45f;

    /** YOLOv8 default input resolution in pixels. */
    public static final int INPUT_SIZE = 640;

    // ── GUI dimensions ──────────────────────────────────────────────────
    public static final int WINDOW_WIDTH = 1280;
    public static final int WINDOW_HEIGHT = 800;
    public static final int CANVAS_MIN_WIDTH = 600;
    public static final int CANVAS_MIN_HEIGHT = 400;
    public static final int CONTROL_PANEL_WIDTH = 260;

    // ── Zoom constraints ────────────────────────────────────────────────
    public static final double MIN_ZOOM = 0.1;
    public static final double MAX_ZOOM = 10.0;
    public static final double ZOOM_STEP = 1.1; // multiplicative factor per scroll step

    // ── Supported image formats ─────────────────────────────────────────
    public static final String[] SUPPORTED_EXTENSIONS = {
            "jpg", "jpeg", "png", "bmp", "webp"
    };
    public static final String[] SUPPORTED_DESCRIPTION = {
            "Image files", "jpg", "jpeg", "png", "bmp", "webp"
    };
}
