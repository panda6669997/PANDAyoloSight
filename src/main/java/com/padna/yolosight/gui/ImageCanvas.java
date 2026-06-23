package com.padna.yolosight.gui;

import com.padna.yolosight.model.DetectionResult;
import com.padna.yolosight.util.DrawingUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

/**
 * Canvas that displays an image and overlays detection results.
 * Supports zoom (mouse wheel, centered on cursor) and pan (click-drag).
 * Lives inside a JScrollPane for scrollbar support.
 */
public class ImageCanvas extends JPanel {

    // Cached copy of the original image for painting
    private Image originalImage;

    // Current detection results (drawn as overlays in paintComponent)
    private List<DetectionResult> detections;

    // Single detection to highlight (user clicked a row in the table)
    private DetectionResult highlighted;

    // Zoom / pan state
    private double zoomFactor = 1.0;
    private double panX = 0.0;
    private double panY = 0.0;

    // Mouse-pan tracking
    private int dragOriginX;
    private int dragOriginY;

    public ImageCanvas() {
        setBackground(Color.DARK_GRAY);
        setFocusable(true);

        // Mouse wheel → zoom centered on cursor
        addMouseWheelListener(e -> {
            if (originalImage == null) return;

            double oldZoom = zoomFactor;
            if (e.getPreciseWheelRotation() < 0) {
                zoomFactor = Math.min(zoomFactor * 1.1, 10.0);
            } else {
                zoomFactor = Math.max(zoomFactor / 1.1, 0.1);
            }

            // Adjust pan so the point under cursor stays fixed
            double scaleChange = zoomFactor / oldZoom;
            panX = e.getX() - scaleChange * (e.getX() - panX);
            panY = e.getY() - scaleChange * (e.getY() - panY);

            revalidate();
            repaint();
        });

        // Mouse drag → pan
        MouseAdapter panAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragOriginX = e.getX();
                dragOriginY = e.getY();
                setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                setCursor(Cursor.getDefaultCursor());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - dragOriginX;
                int dy = e.getY() - dragOriginY;
                panX += dx;
                panY += dy;
                dragOriginX = e.getX();
                dragOriginY = e.getY();
                repaint();
            }
        };
        addMouseListener(panAdapter);
        addMouseMotionListener(panAdapter);
    }

    /** Load a new image and fit it to the current view. */
    public void setImage(Image image) {
        this.originalImage = image;
        fitToWindow();
    }

    /** @return currently displayed image, or null. */
    public Image getImage() {
        return originalImage;
    }

    /** Store detection results for overlay painting. Pass null to clear. */
    public void setDetections(List<DetectionResult> detections) {
        this.detections = detections;
    }

    /** Set a single detection to highlight (from table row click). Pass null to clear. */
    public void setHighlighted(DetectionResult det) {
        this.highlighted = det;
    }

    /** @return current detections, or null. */
    public List<DetectionResult> getDetections() {
        return detections;
    }

    /** Scale (zoom + pan) so the entire image fits within the visible area. */
    public void fitToWindow() {
        if (originalImage == null) return;

        Container parent = getParent();
        if (parent == null) return;

        int viewW = parent.getWidth();
        int viewH = parent.getHeight();
        int imgW = originalImage.getWidth(null);
        int imgH = originalImage.getHeight(null);

        if (imgW <= 0 || imgH <= 0) return;

        double scaleX = (double) viewW / imgW;
        double scaleY = (double) viewH / imgH;
        zoomFactor = Math.min(scaleX, scaleY) * 0.9;
        panX = 0;
        panY = 0;

        revalidate();
        repaint();
    }

    /** Zoom in by 25%. */
    public void zoomIn() {
        zoomFactor = Math.min(zoomFactor * 1.25, 10.0);
        revalidate();
        repaint();
    }

    /** Zoom out by 25%. */
    public void zoomOut() {
        zoomFactor = Math.max(zoomFactor / 1.25, 0.1);
        revalidate();
        repaint();
    }

    /** Reset to 100% (1:1 pixel mapping). */
    public void resetZoom() {
        zoomFactor = 1.0;
        panX = 0;
        panY = 0;
        revalidate();
        repaint();
    }

    /** Current zoom factor (1.0 = 100%). */
    public double getZoomFactor() {
        return zoomFactor;
    }

    // ── Layout ─────────────────────────────────────────────────────────

    @Override
    public Dimension getPreferredSize() {
        if (originalImage == null) {
            return new Dimension(600, 400);
        }
        int w = (int) (originalImage.getWidth(null) * zoomFactor);
        int h = (int) (originalImage.getHeight(null) * zoomFactor);
        return new Dimension(Math.max(w, 100), Math.max(h, 100));
    }

    // ── Painting ───────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Enable high-quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);

        if (originalImage != null) {
            AffineTransform savedTransform = g2d.getTransform();
            g2d.translate(panX, panY);
            g2d.scale(zoomFactor, zoomFactor);
            g2d.drawImage(originalImage, 0, 0, null);

            // Draw detection overlays with zoom compensation
            if (detections != null && !detections.isEmpty()) {
                DrawingUtils.drawDetections(g2d, detections, zoomFactor);
            }

            // Draw highlighted detection on top
            if (highlighted != null) {
                DrawingUtils.drawHighlighted(g2d, highlighted, zoomFactor);
            }

            g2d.setTransform(savedTransform);
        } else {
            // Placeholder hint when no image is loaded
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 16));
            String hint = "拖放图片到此处，或使用 文件 → 打开";
            FontMetrics fm = g2d.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(hint)) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2d.drawString(hint, x, y);
        }
    }
}
