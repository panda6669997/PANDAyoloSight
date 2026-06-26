package com.padna.yolosight.gui;

import com.padna.yolosight.model.ClassificationResult;
import com.padna.yolosight.model.DetectionResult;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * Thumbnail grid with annotated previews. Thumbnails show bounding boxes
 * rendered on-the-fly from detection data. Click opens full-size popup.
 */
public class ThumbnailGridPanel {

    private static final int THUMB = 180;
    private static final int GAP = 10;

    private final ScrollPane scrollPane;
    private final FlowPane grid;
    private final Label placeholder;

    private static final String[] COLORS = {
            "#FF3B30","#34C759","#007AFF","#FF9500","#AF52DE",
            "#FFCC00","#5AC8FA","#FF2D55","#8E8E93","#4CDA64",
            "#5856D6","#FF648E","#00C853","#EF5350","#9C27B0",
            "#2196F3","#009688","#FF9800","#607D8B","#795548",
    };

    public ThumbnailGridPanel() {
        grid = new FlowPane(GAP, GAP);
        grid.setPadding(new javafx.geometry.Insets(GAP));
        grid.setStyle("-fx-background-color: #151518;");

        scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #151518;");
        scrollPane.getStyleClass().add("canvas-viewport");

        placeholder = new Label("运行批量分类后，标注缩略图将显示在此处");
        placeholder.setStyle("-fx-text-fill: #606065; -fx-font-size: 15px;");
        grid.getChildren().add(placeholder);
    }

    public ScrollPane getView() { return scrollPane; }

    /** Build annotated thumbnails from classification results. */
    public void populateFrom(List<ClassificationResult> results, File outputDir) {
        grid.getChildren().clear();
        if (results == null || results.isEmpty()) {
            grid.getChildren().add(placeholder);
            return;
        }

        for (ClassificationResult r : results) {
            if (!r.success()) continue;
            File imgFile = new File(r.targetPath());
            if (!imgFile.exists()) continue;

            VBox card = buildAnnotatedThumbnail(imgFile, r);
            grid.getChildren().add(card);
        }
    }

    private VBox buildAnnotatedThumbnail(File file, ClassificationResult result) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: #2a2a2e; -fx-background-radius: 6; -fx-padding: 6;");
        card.setCursor(Cursor.HAND);

        // Canvas for annotated thumbnail
        Canvas canvas = new Canvas(THUMB, THUMB);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Load image and draw annotations async
        new Thread(() -> {
            try {
                BufferedImage img = ImageIO.read(file);
                if (img == null) return;

                // Scale to fit thumbnail
                double scale = Math.min(THUMB / (double) img.getWidth(),
                        THUMB / (double) img.getHeight());
                int dw = (int) (img.getWidth() * scale);
                int dh = (int) (img.getHeight() * scale);
                int dx = (THUMB - dw) / 2;
                int dy = (THUMB - dh) / 2;

                Image fxImg = SwingFXUtils.toFXImage(img, null);

                Platform.runLater(() -> {
                    // Fill bg
                    gc.setFill(Color.web("#1a1a1e"));
                    gc.fillRect(0, 0, THUMB, THUMB);

                    // Draw image scaled
                    gc.drawImage(fxImg, dx, dy, dw, dh);

                    // Draw detection boxes (scaled down)
                    List<DetectionResult> dets = result.detections();
                    if (dets != null && !dets.isEmpty()) {
                        for (DetectionResult det : dets) {
                            java.awt.Rectangle b = det.boundingBox();
                            double bx = dx + b.x * scale;
                            double by = dy + b.y * scale;
                            double bw = b.width * scale;
                            double bh = b.height * scale;

                            Color c = Color.web(COLORS[Math.abs(det.className().hashCode()) % COLORS.length]);
                            gc.setStroke(c);
                            gc.setLineWidth(1.5);
                            gc.strokeRect(bx, by, bw, bh);

                            // Tiny label
                            String label = det.className();
                            gc.setFill(new Color(0,0,0,0.65));
                            gc.fillText(label, bx + 2, by + 10);
                            gc.setFill(Color.WHITE);
                            gc.setFont(Font.font("SansSerif", 9));
                            gc.fillText(label, bx + 2, by + 10);
                        }
                    }
                });
            } catch (Exception ignored) {}
        }).start();

        // Labels
        Label catLabel = new Label(result.primaryClass());
        catLabel.setFont(Font.font("SansSerif", 11));
        catLabel.setTextFill(Color.web("#909095"));
        catLabel.setMaxWidth(THUMB);
        catLabel.setAlignment(Pos.CENTER);

        Label nameLabel = new Label(result.fileName());
        nameLabel.setFont(Font.font("SansSerif", 10));
        nameLabel.setTextFill(Color.web("#606065"));
        nameLabel.setMaxWidth(THUMB);
        nameLabel.setAlignment(Pos.CENTER);

        card.getChildren().addAll(canvas, catLabel, nameLabel);

        // Click → open full-size popup
        card.setOnMouseClicked(e -> openFullSize(file, result));

        return card;
    }

    private void openFullSize(File file, ClassificationResult result) {
        ImageCanvasView canvas = new ImageCanvasView();
        Image fullImg = new Image(file.toURI().toString());
        canvas.setImage(fullImg);

        // Show detection boxes on the full-size view
        if (!result.detections().isEmpty()) {
            canvas.setDetections(result.detections());
            canvas.requestRedraw();
        }

        Stage popup = new Stage();
        popup.setTitle(result.fileName() + " — " + result.primaryClass());
        popup.initOwner(grid.getScene().getWindow());

        BorderPane popupRoot = new BorderPane();
        popupRoot.setCenter(canvas.getView());

        Scene scene = new Scene(popupRoot, 1000, 750);
        scene.getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());
        popup.setScene(scene);
        popup.show();
        popup.setOnShown(ev -> canvas.fitToWindow());
    }
}
