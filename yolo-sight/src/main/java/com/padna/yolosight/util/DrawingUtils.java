package com.padna.yolosight.util;

import com.padna.yolosight.model.DetectionResult;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * Static methods for drawing detection overlays (bounding boxes, labels)
 * onto a {@link Graphics2D} context.
 * All coordinates are expected in image-pixel space.
 * The {@code zoomFactor} parameter compensates for canvas scaling so
 * strokes and fonts remain visible at any zoom level.
 */
public final class DrawingUtils {

    // Distinct colors for up to 20 classes, cycled via hashCode
    private static final Color[] CLASS_COLORS = {
            new Color(0xFF, 0x3B, 0x30), // red
            new Color(0x34, 0xC7, 0x59), // green
            new Color(0x00, 0x7A, 0xFF), // blue
            new Color(0xFF, 0x95, 0x00), // orange
            new Color(0xAF, 0x52, 0xDE), // purple
            new Color(0xFF, 0xCC, 0x00), // yellow
            new Color(0x5A, 0xC8, 0xFA), // light blue
            new Color(0xFF, 0x2D, 0x55), // pink
            new Color(0x8E, 0x8E, 0x93), // gray
            new Color(0x4C, 0xDA, 0x64), // lime
            new Color(0x58, 0x56, 0xD6), // indigo
            new Color(0xFF, 0x64, 0x8E), // hot pink
            new Color(0x00, 0xC8, 0x53), // emerald
            new Color(0xEF, 0x53, 0x50), // deep orange
            new Color(0x9C, 0x27, 0xB0), // deep purple
            new Color(0x21, 0x96, 0xF3), // material blue
            new Color(0x00, 0x96, 0x88), // teal
            new Color(0xFF, 0x98, 0x00), // amber
            new Color(0x60, 0x7D, 0x8B), // blue gray
            new Color(0x79, 0x55, 0x48), // brown
    };

    // Base visual constants (will be scaled by 1/zoomFactor)
    private static final float BASE_STROKE = 5.0f;
    private static final float BASE_HIGHLIGHT_STROKE = 8.0f;
    private static final int BASE_FONT_SIZE = 20;
    private static final Color LABEL_BG_COLOR = new Color(0, 0, 0, 180);
    private static final Color LABEL_TEXT_COLOR = Color.WHITE;
    private static final int LABEL_PAD_X = 6;
    private static final int LABEL_PAD_Y = 3;
    private static final int LABEL_ARC = 4;

    private DrawingUtils() {
        // utility class
    }

    /**
     * Draw all detection overlays with zoom-compensated stroke and font sizes.
     *
     * @param g2d        the graphics context (already translated/scaled for image coords)
     * @param results    detections to draw
     * @param zoomFactor current canvas zoom (1.0 = 100%). Pass 1.0 for
     *                   non-zoomed contexts like saving to file.
     */
    public static void drawDetections(Graphics2D g2d, List<DetectionResult> results,
                                       double zoomFactor) {
        if (results == null || results.isEmpty()) return;

        Stroke savedStroke = g2d.getStroke();
        Font savedFont = g2d.getFont();
        Color savedColor = g2d.getColor();

        // Compensate stroke & font so they appear the same size on screen
        // regardless of zoom level. Clamp to prevent degenerate values.
        double invZoom = 1.0 / Math.max(zoomFactor, 0.05);

        Stroke boxStroke = new BasicStroke(
                (float) (BASE_STROKE * invZoom),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        Font labelFont = new Font("SansSerif", Font.BOLD,
                (int) Math.round(BASE_FONT_SIZE * invZoom));
        int padX = (int) Math.round(LABEL_PAD_X * invZoom);
        int padY = (int) Math.round(LABEL_PAD_Y * invZoom);
        int arc = (int) Math.round(LABEL_ARC * invZoom);

        for (DetectionResult r : results) {
            Color classColor = getClassColor(r.className());
            Rectangle bbox = r.boundingBox();
            String label = r.toLabel();

            // ── Bounding box ──────────────────────────────────────
            g2d.setStroke(boxStroke);
            g2d.setColor(classColor);
            g2d.draw(bbox);

            // ── Label ─────────────────────────────────────────────
            g2d.setFont(labelFont);
            FontMetrics fm = g2d.getFontMetrics();
            int labelW = fm.stringWidth(label) + padX * 2;
            int labelH = fm.getHeight() + padY * 2;
            int labelX = bbox.x - 1;
            int labelY = bbox.y - labelH;
            if (labelY < 0) {
                labelY = bbox.y; // clamp to top
            }

            g2d.setColor(LABEL_BG_COLOR);
            g2d.fill(new RoundRectangle2D.Float(
                    labelX, labelY, labelW, labelH, arc, arc));

            g2d.setColor(LABEL_TEXT_COLOR);
            g2d.drawString(label,
                    labelX + padX,
                    labelY + fm.getAscent() + padY);
        }

        g2d.setStroke(savedStroke);
        g2d.setFont(savedFont);
        g2d.setColor(savedColor);
    }

    /**
     * Overload for non-zoomed contexts (e.g. saving to file).
     */
    public static void drawDetections(Graphics2D g2d, List<DetectionResult> results) {
        drawDetections(g2d, results, 1.0);
    }

    /**
     * Draw one detection with a highlighted (thicker, brighter) stroke.
     */
    public static void drawHighlighted(Graphics2D g2d, DetectionResult r,
                                        double zoomFactor) {
        if (r == null) return;

        double invZoom = 1.0 / Math.max(zoomFactor, 0.05);
        Stroke savedStroke = g2d.getStroke();
        g2d.setStroke(new BasicStroke(
                (float) (BASE_HIGHLIGHT_STROKE * invZoom),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.setColor(getClassColor(r.className()).brighter());
        g2d.draw(r.boundingBox());
        g2d.setStroke(savedStroke);
    }

    public static void drawHighlighted(Graphics2D g2d, DetectionResult r) {
        drawHighlighted(g2d, r, 1.0);
    }

    /**
     * Pick a distinct color for a class name (stable across runs).
     */
    public static Color getClassColor(String className) {
        int hash = Math.abs(className.hashCode());
        return CLASS_COLORS[hash % CLASS_COLORS.length];
    }
}
