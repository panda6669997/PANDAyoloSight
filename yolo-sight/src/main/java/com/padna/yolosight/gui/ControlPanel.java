package com.padna.yolosight.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

/**
 * Left sidebar with card-style controls.
 */
public class ControlPanel {

    private final ScrollPane scrollView;
    private final VBox content;
    private final Button detectButton;
    private final Slider confidenceSlider;
    private final Label confidenceValueLabel;
    private final ComboBox<String> classFilterCombo;
    private final Label summaryCountLabel;
    private final Label summarySubLabel;

    private Runnable onDetectAction;
    private java.util.function.Consumer<Float> onConfidenceChanged;
    private java.util.function.Consumer<String> onClassFilterChanged;

    public ControlPanel() {
        content = new VBox(12);
        content.setPadding(new Insets(12, 10, 12, 10));
        content.setStyle("-fx-background-color: #1e1e22;");

        // ── Detect button ────────────────────────────────────────────
        detectButton = new Button("检测物体");
        detectButton.getStyleClass().add("accent-button");
        detectButton.setMaxWidth(Double.MAX_VALUE);
        detectButton.setPrefHeight(44);
        detectButton.setOnAction(e -> {
            if (onDetectAction != null) onDetectAction.run();
        });
        content.getChildren().add(detectButton);

        // ── Confidence card ──────────────────────────────────────────
        VBox confCard = createCard("置信度阈值");
        confidenceValueLabel = new Label("50%");
        confidenceValueLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 26));
        confidenceValueLabel.setAlignment(Pos.CENTER);
        confidenceValueLabel.setMaxWidth(Double.MAX_VALUE);

        confidenceSlider = new Slider(10, 100, 50);
        confidenceSlider.setMajorTickUnit(10);
        confidenceSlider.setMinorTickCount(4);
        confidenceSlider.setShowTickMarks(true);
        confidenceSlider.setSnapToTicks(true);
        confidenceSlider.valueProperty().addListener((obs, old, val) -> {
            int v = val.intValue();
            confidenceValueLabel.setText(v + "%");
            if (!confidenceSlider.isValueChanging() && onConfidenceChanged != null) {
                onConfidenceChanged.accept(v / 100f);
            }
        });
        // Fire on release
        confidenceSlider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
            if (!isChanging && onConfidenceChanged != null) {
                onConfidenceChanged.accept((float) confidenceSlider.getValue() / 100f);
            }
        });

        confCard.getChildren().addAll(confidenceValueLabel, confidenceSlider);
        content.getChildren().add(confCard);

        // ── Filter card ──────────────────────────────────────────────
        VBox filterCard = createCard("类别筛选");
        classFilterCombo = new ComboBox<>();
        classFilterCombo.getItems().add("全部类别");
        classFilterCombo.getSelectionModel().selectFirst();
        classFilterCombo.setMaxWidth(Double.MAX_VALUE);
        classFilterCombo.setOnAction(e -> {
            if (onClassFilterChanged != null) {
                onClassFilterChanged.accept(classFilterCombo.getValue());
            }
        });
        filterCard.getChildren().add(classFilterCombo);
        content.getChildren().add(filterCard);

        // ── Results card ─────────────────────────────────────────────
        VBox resultsCard = createCard("检测结果");
        summaryCountLabel = new Label();
        summaryCountLabel.getStyleClass().add("summary-count");
        summarySubLabel = new Label("暂无检测结果");
        summarySubLabel.getStyleClass().add("summary-sub");
        resultsCard.getChildren().addAll(summaryCountLabel, summarySubLabel);
        content.getChildren().add(resultsCard);

        // Scroll wrapper
        scrollView = new ScrollPane(content);
        scrollView.setFitToWidth(true);
        scrollView.setMinWidth(220);
        scrollView.setPrefWidth(260);
        scrollView.setStyle("-fx-background-color: #1e1e22;");
        scrollView.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    }

    private VBox createCard(String title) {
        VBox card = new VBox(10);
        card.getStyleClass().add("control-card");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-title");
        card.getChildren().add(0, titleLabel);
        return card;
    }

    public ScrollPane getView() {
        return scrollView;
    }

    // ── Getters & setters ───────────────────────────────────────────────

    public float getConfidenceThreshold() {
        return (float) confidenceSlider.getValue() / 100f;
    }

    public void updateClassFilter(List<String> classNames) {
        String current = classFilterCombo.getValue();
        classFilterCombo.getItems().clear();
        classFilterCombo.getItems().add("全部类别");
        classFilterCombo.getItems().addAll(classNames);
        if (current != null && classFilterCombo.getItems().contains(current)) {
            classFilterCombo.setValue(current);
        } else {
            classFilterCombo.getSelectionModel().selectFirst();
        }
    }

    public void setSummary(int totalDetected, long elapsedMs) {
        if (totalDetected == 0) {
            summaryCountLabel.setText("—");
            summarySubLabel.setText("未检测到物体\n尝试降低置信度阈值");
        } else {
            summaryCountLabel.setText(String.valueOf(totalDetected));
            summarySubLabel.setText(String.format("个物体  ·  %,d 毫秒", elapsedMs));
        }
    }

    public void setOnDetectAction(Runnable action) { this.onDetectAction = action; }
    public void setOnConfidenceChanged(java.util.function.Consumer<Float> cb) { this.onConfidenceChanged = cb; }
    public void setOnClassFilterChanged(java.util.function.Consumer<String> cb) { this.onClassFilterChanged = cb; }
    public void setDetectEnabled(boolean enabled) { detectButton.setDisable(!enabled); }
}
