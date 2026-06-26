package com.padna.yolosight.gui;

import com.padna.yolosight.model.DetectionResult;
import com.padna.yolosight.util.DrawingUtils;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.List;

/**
 * Canvas-based image viewer with zoom/pan and detection overlays.
 * The StackPane wrapper clips the canvas to the viewport for scroll pane support.
 */
public class ImageCanvasView {

    private final StackPane view;
    private final Canvas canvas;
    private final GraphicsContext gc;

    private Image originalImage;
    private List<DetectionResult> detections;
    private DetectionResult highlighted;

    // Zoom/pan state
    private double zoomFactor = 1.0;
    private double panX = 0.0;
    private double panY = 0.0;

    // Drag tracking
    private double dragStartX, dragStartY, dragPanX, dragPanY;

    public ImageCanvasView() {
        canvas = new Canvas(800, 600);
        gc = canvas.getGraphicsContext2D();
        view = new StackPane(canvas);
        view.setStyle("-fx-background-color: #151518;");
        view.setMinSize(400, 300);

        canvas.widthProperty().bind(view.widthProperty());
        canvas.heightProperty().bind(view.heightProperty());

        // Redraw on resize
        canvas.widthProperty().addListener((o, ov, nv) -> redraw());
        canvas.heightProperty().addListener((o, ov, nv) -> redraw());

        // Mouse wheel → zoom
        view.setOnScroll(e -> {
            if (originalImage == null) return;
            double oldZoom = zoomFactor;
            double delta = e.getDeltaY();
            if (delta > 0) {
                zoomFactor = Math.min(zoomFactor * 1.1, 10.0);
            } else {
                zoomFactor = Math.max(zoomFactor / 1.1, 0.1);
            }
            double scaleChange = zoomFactor / oldZoom;
            panX = e.getX() - scaleChange * (e.getX() - panX);
            panY = e.getY() - scaleChange * (e.getY() - panY);
            redraw();
        });

        // Mouse drag → pan
        view.setOnMousePressed(e -> {
            dragStartX = e.getX();
            dragStartY = e.getY();
            dragPanX = panX;
            dragPanY = panY;
        });
        view.setOnMouseDragged(e -> {
            panX = dragPanX + (e.getX() - dragStartX);
            panY = dragPanY + (e.getY() - dragStartY);
            redraw();
        });

        redraw();
    }

    public Pane getView() {
        return view;
    }

    // ── Image / data setters ───────────────────────────────────────────

    public void setImage(Image image) {
        this.originalImage = image;
        fitToWindow();
    }

    public void setDetections(List<DetectionResult> detections) {
        this.detections = detections;
    }

    public void setHighlighted(DetectionResult det) {
        this.highlighted = det;
    }

    public void requestRedraw() {
        redraw();
    }

    // ── Zoom controls ──────────────────────────────────────────────────

    public void fitToWindow() {
        if (originalImage == null) return;
        double viewW = view.getWidth();
        double viewH = view.getHeight();
        if (viewW <= 0 || viewH <= 0) return;

        double scaleX = viewW / originalImage.getWidth();
        double scaleY = viewH / originalImage.getHeight();
        zoomFactor = Math.min(scaleX, scaleY) * 0.9;
        panX = 0;
        panY = 0;
        redraw();
    }

    public void zoomIn() {
        zoomFactor = Math.min(zoomFactor * 1.25, 10.0);
        redraw();
    }

    public void zoomOut() {
        zoomFactor = Math.max(zoomFactor / 1.25, 0.1);
        redraw();
    }

    public void resetZoom() {
        zoomFactor = 1.0;
        panX = 0;
        panY = 0;
        redraw();
    }

    // ── Redraw ─────────────────────────────────────────────────────────

    private void redraw() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        gc.clearRect(0, 0, w, h);
        gc.setFill(Color.web("#151518"));
        gc.fillRect(0, 0, w, h);

        if (originalImage != null) {
            gc.save();
            gc.translate(panX, panY);
            gc.scale(zoomFactor, zoomFactor);
            gc.drawImage(originalImage, 0, 0);

            // Draw detection overlays in image-pixel space
            if (detections != null && !detections.isEmpty()) {
                drawDetectionsJavaFX(gc, detections, zoomFactor);
            }
            if (highlighted != null) {
                drawHighlightedJavaFX(gc, highlighted, zoomFactor);
            }

            gc.restore();
        } else {
            gc.setFill(Color.web("#808085"));
            gc.setFont(Font.font("SansSerif", 16));
            String hint = "拖放图片到此处，或使用 文件 → 打开";
            javafx.scene.text.Text t = new javafx.scene.text.Text(hint);
            t.setFont(gc.getFont());
            double tw = t.getLayoutBounds().getWidth();
            double th = t.getLayoutBounds().getHeight();
            gc.fillText(hint, (w - tw) / 2, (h - th) / 2);
        }
    }

    // ── JavaFX drawing (using GraphicsContext) ─────────────────────────

    private static final String[] CLASS_COLORS = {
            "#FF3B30", "#34C759", "#007AFF", "#FF9500", "#AF52DE",
            "#FFCC00", "#5AC8FA", "#FF2D55", "#8E8E93", "#4CDA64",
            "#5856D6", "#FF648E", "#00C853", "#EF5350", "#9C27B0",
            "#2196F3", "#009688", "#FF9800", "#607D8B", "#795548",
    };

    private void drawDetectionsJavaFX(GraphicsContext gc,
                                       List<DetectionResult> results, double zoom) {
        double invZoom = 1.0 / Math.max(zoom, 0.05);
        double strokeW = 5.0 * invZoom;
        double fontSize = 20 * invZoom;

        for (DetectionResult r : results) {
            Color color = getClassColor(r.className());
            java.awt.Rectangle bbox = r.boundingBox();
            String label = r.toLabel();

            // Bounding box
            gc.setStroke(color);
            gc.setLineWidth(strokeW);
            gc.strokeRect(bbox.x, bbox.y, bbox.width, bbox.height);

            // Label
            gc.setFont(Font.font("SansSerif", javafx.scene.text.FontWeight.BOLD, fontSize));
            var fm = gc.getFont();
            double labelW = label.length() * fontSize * 0.6 + 12 * invZoom;
            double labelH = fontSize + 6 * invZoom;
            double labelX = bbox.x - 1;
            double labelY = bbox.y - labelH;
            if (labelY < 0) labelY = bbox.y;

            gc.setFill(new Color(0, 0, 0, 0.7));
            gc.fillRoundRect(labelX, labelY, labelW, labelH, 4 * invZoom, 4 * invZoom);
            gc.setFill(Color.WHITE);
            gc.fillText(label, labelX + 6 * invZoom,
                    labelY + fontSize + 2 * invZoom);
        }
    }

    private void drawHighlightedJavaFX(GraphicsContext gc,
                                        DetectionResult r, double zoom) {
        double invZoom = 1.0 / Math.max(zoom, 0.05);
        java.awt.Rectangle bbox = r.boundingBox();
        Color color = getClassColor(r.className()).brighter();
        gc.setStroke(color);
        gc.setLineWidth(8.0 * invZoom);
        gc.strokeRect(bbox.x, bbox.y, bbox.width, bbox.height);
    }

    private Color getClassColor(String className) {
        int hash = Math.abs(className.hashCode());
        return Color.web(CLASS_COLORS[hash % CLASS_COLORS.length]);
    }
}
