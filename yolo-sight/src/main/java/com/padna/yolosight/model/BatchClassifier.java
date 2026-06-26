package com.padna.yolosight.model;

import com.padna.yolosight.config.AppConfig;
import com.padna.yolosight.util.ImageUtils;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Scans a folder of images, runs YOLO detection on each, and sorts them
 * into category-named subdirectories based on the highest-confidence object.
 *
 * <p>Supports pause/cancel and resume: a {@code .batch_state.txt} file in
 * the output directory tracks completed files so processing can resume
 * after interruption.</p>
 */
public class BatchClassifier {

    public static final String UNCLASSIFIED = "未分类";
    private static final String STATE_FILE = ".batch_state.txt";

    private final ModelManager modelManager;
    private final float confidenceThreshold;
    private final boolean moveInsteadOfCopy;
    private volatile boolean cancelled;
    private File stateFile;

    public BatchClassifier(ModelManager modelManager, float confidenceThreshold,
                           boolean moveInsteadOfCopy) {
        this.modelManager = modelManager;
        this.confidenceThreshold = confidenceThreshold;
        this.moveInsteadOfCopy = moveInsteadOfCopy;
    }

    /** @return true if processing was cancelled by user. */
    public boolean isCancelled() { return cancelled; }

    /** Check if a previous batch was interrupted and can be resumed. */
    public static boolean canResume(File outputDir) {
        return new File(outputDir, STATE_FILE).exists();
    }

    /** Get the count of already-completed files from the state file. */
    public static int resumeCount(File outputDir) {
        try {
            return Files.readAllLines(new File(outputDir, STATE_FILE).toPath()).size();
        } catch (Exception e) { return 0; }
    }

    /**
     * Process all supported images. If a previous state file exists in the
     * output directory, already-processed files are skipped.
     */
    public List<ClassificationResult> classifyFolder(
            File sourceDir, File outputDir,
            ProgressCallback onProgress) throws Exception {

        cancelled = false;
        stateFile = new File(outputDir, STATE_FILE);
        List<ClassificationResult> results = new ArrayList<>();

        // Load already-processed files from previous interrupted run
        Set<String> done = new HashSet<>();
        if (stateFile.exists()) {
            done.addAll(Files.readAllLines(stateFile.toPath(), StandardCharsets.UTF_8));
        }

        // Gather supported image files (exclude already done)
        File[] files = sourceDir.listFiles(f -> {
            if (!f.isFile()) return false;
            if (done.contains(f.getName())) return false;
            String name = f.getName().toLowerCase();
            for (String ext : AppConfig.SUPPORTED_EXTENSIONS) {
                if (name.endsWith("." + ext)) return true;
            }
            return false;
        });

        if (files == null || files.length == 0) {
            return results;
        }

        YoloDetector detector = new YoloDetector(modelManager, confidenceThreshold);
        int total = done.size() + files.length; // include already-done in total
        int processed = done.size();

        // Send initial progress
        if (onProgress != null) {
            onProgress.update(processed, total);
        }

        for (File file : files) {
            if (cancelled) break;

            processed++;
            if (onProgress != null) {
                onProgress.update(processed, total);
            }

            try {
                BufferedImage img = ImageUtils.loadImage(file);
                List<DetectionResult> detections = detector.detect(img);

                String category;
                float conf;
                if (detections.isEmpty()) {
                    category = UNCLASSIFIED;
                    conf = 0f;
                } else {
                    DetectionResult best = detections.get(0);
                    category = sanitizeFolderName(best.className());
                    conf = best.confidence();
                }

                File categoryDir = new File(outputDir, category);
                if (!categoryDir.exists()) categoryDir.mkdirs();
                File dest = resolveConflict(new File(categoryDir, file.getName()));
                Files.copy(file.toPath(), dest.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                results.add(new ClassificationResult(
                        file.getName(), category, conf,
                        file.getAbsolutePath(), dest.getAbsolutePath(),
                        detections.size(), true, detections));

                // Persist progress after each successful file
                appendState(file.getName());

            } catch (Exception e) {
                results.add(new ClassificationResult(
                        file.getName(), UNCLASSIFIED, 0f,
                        file.getAbsolutePath(), "", 0, false, List.of()));
                System.err.println("[BatchClassifier] Failed: " + file.getName() + " - " + e.getMessage());
            }
        }

        // Clean up state file on successful full completion
        if (!cancelled && stateFile.exists()) {
            stateFile.delete();
        }

        return results;
    }

    /** Append one completed filename to the state file. */
    private void appendState(String fileName) {
        try (BufferedWriter w = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(stateFile, true), StandardCharsets.UTF_8))) {
            w.write(fileName);
            w.newLine();
        } catch (IOException ignored) {}
    }

    /** Signal cancellation — stops after current image finishes. */
    public void cancel() {
        cancelled = true;
    }

    private String sanitizeFolderName(String className) {
        return className.replaceAll("[<>:\"/\\\\|?*]", "_").trim();
    }

    private File resolveConflict(File dest) {
        if (!dest.exists()) return dest;
        String base = dest.getName();
        int dot = base.lastIndexOf('.');
        String name = dot > 0 ? base.substring(0, dot) : base;
        String ext = dot > 0 ? base.substring(dot) : "";
        int counter = 2;
        while (true) {
            File alt = new File(dest.getParent(), name + " (" + counter + ")" + ext);
            if (!alt.exists()) return alt;
            counter++;
        }
    }

    @FunctionalInterface
    public interface ProgressCallback {
        void update(int current, int total);
    }
}
