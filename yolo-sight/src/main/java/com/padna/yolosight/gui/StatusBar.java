package com.padna.yolosight.gui;

import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.geometry.Orientation;

/**
 * Bottom status bar — left status message, right image/model info.
 */
public class StatusBar {

    private final HBox bar;
    private final Label leftLabel;
    private final Label rightLabel;

    public StatusBar() {
        bar = new HBox();
        bar.getStyleClass().add("status-bar");
        bar.setPrefHeight(32);

        leftLabel = new Label("就绪");
        rightLabel = new Label("YOLOv8n  ·  ONNX Runtime");

        // Spacer between left and right
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Separator sep = new Separator(Orientation.VERTICAL);
        sep.setStyle("-fx-padding: 0 8;");

        bar.getChildren().addAll(leftLabel, spacer, rightLabel);
    }

    public void setLeft(Label label) {
        bar.getChildren().set(0, label);
    }

    public void setRight(String text) {
        rightLabel.setText(text);
    }

    public Pane getView() {
        return bar;
    }
}
