package com.padna.yolosight.gui;

import com.padna.yolosight.model.ClassificationResult;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Panel showing batch classification results with statistics and
 * a button to open the classified output folder.
 */
public class BatchResultPanel {

    private final VBox root;
    private final TableView<ClassificationRow> tableView;
    private final ObservableList<ClassificationRow> rows = FXCollections.observableArrayList();
    private final Label statsLabel;
    private final Label distLabel;
    private final Button openFolderBtn;
    private final ProgressBar progressBar;
    private final Label progressLabel;

    private File outputDir;

    public BatchResultPanel() {
        root = new VBox(10);
        root.setPadding(new Insets(12));
        root.setStyle("-fx-background-color: #1e1e22;");

        // ── Title bar ──────────────────────────────────────────────
        HBox titleBar = new HBox(10);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("批量分类结果");
        title.setFont(Font.font("SansSerif", FontWeight.BOLD, 14));
        title.setTextFill(Color.web("#e0e0e0"));

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        openFolderBtn = new Button("打开输出目录");
        openFolderBtn.setDisable(true);
        openFolderBtn.setOnAction(e -> {
            if (outputDir != null && outputDir.exists()) {
                try { Desktop.getDesktop().open(outputDir); } catch (IOException ignored) {}
            }
        });

        titleBar.getChildren().addAll(title, spacer, openFolderBtn);
        root.getChildren().add(titleBar);

        // ── Table ──────────────────────────────────────────────────
        tableView = new TableView<>();
        tableView.setItems(rows);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableView.setPrefHeight(200);

        TableColumn<ClassificationRow, Number> numCol = col("#", 35,
                d -> new SimpleIntegerProperty(d.rowNum));
        TableColumn<ClassificationRow, String> fileCol = col("文件名", 140,
                d -> new SimpleStringProperty(d.fileName));
        TableColumn<ClassificationRow, String> catCol = col("分类", 90,
                d -> new SimpleStringProperty(d.primaryClass));
        catCol.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item);
                if (!empty && "未分类".equals(item)) {
                    setTextFill(Color.web("#F44336"));
                } else {
                    setTextFill(Color.web("#e0e0e0"));
                }
            }
        });
        TableColumn<ClassificationRow, String> confCol = col("置信度", 65,
                d -> new SimpleStringProperty(d.confDisplay));
        TableColumn<ClassificationRow, String> detCol = col("检测数", 55,
                d -> new SimpleStringProperty(d.detDisplay));
        TableColumn<ClassificationRow, String> statusCol = col("状态", 45,
                d -> new SimpleStringProperty(d.statusIcon));
        statusCol.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item);
                setAlignment(Pos.CENTER);
            }
        });

        tableView.getColumns().addAll(numCol, fileCol, catCol, confCol, detCol, statusCol);
        root.getChildren().add(tableView);

        // ── Progress ───────────────────────────────────────────────
        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(6);
        progressBar.setVisible(false);
        progressLabel = new Label();
        progressLabel.setStyle("-fx-text-fill: #808085; -fx-font-size: 11px;");
        root.getChildren().addAll(progressBar, progressLabel);

        // ── Statistics ─────────────────────────────────────────────
        statsLabel = new Label();
        statsLabel.setStyle("-fx-text-fill: #c0c0c5; -fx-font-size: 12px;");
        distLabel = new Label();
        distLabel.setStyle("-fx-text-fill: #808085; -fx-font-size: 11px;");
        distLabel.setWrapText(true);
        root.getChildren().addAll(statsLabel, distLabel);
    }

    // ── Construction helpers ──────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private <T> TableColumn<ClassificationRow, T> col(
            String title, int width,
            java.util.function.Function<ClassificationRow,
                    javafx.beans.value.ObservableValue<T>> factory) {
        TableColumn<ClassificationRow, T> c = new TableColumn<>(title);
        c.setPrefWidth(width);
        c.setCellValueFactory(d -> factory.apply(d.getValue()));
        return c;
    }

    // ── Public API ────────────────────────────────────────────────────

    public Pane getView() { return root; }

    /** Show progress during batch processing. */
    public void showProgress(boolean visible) {
        progressBar.setVisible(visible);
        progressLabel.setVisible(visible);
    }

    public void updateProgress(int current, int total) {
        progressBar.setProgress((double) current / total);
        progressLabel.setText(current + " / " + total);
    }

    /** Populate with results and update stats. */
    public void setResults(List<ClassificationResult> results, File outputDir) {
        this.outputDir = outputDir;
        rows.clear();
        int i = 1;
        for (ClassificationResult r : results) {
            rows.add(new ClassificationRow(i++, r));
        }
        openFolderBtn.setDisable(false);
        updateStats(results);
    }

    public void clear() {
        rows.clear();
        statsLabel.setText("");
        distLabel.setText("");
        openFolderBtn.setDisable(true);
    }

    // ── Stats ────────────────────────────────────────────────────────

    private void updateStats(List<ClassificationResult> results) {
        long success = results.stream().filter(ClassificationResult::success).count();
        long fail = results.size() - success;

        statsLabel.setText(String.format("共 %d 张图片  ·  成功 %d 张  ·  失败 %d 张",
                results.size(), success, fail));

        // Category distribution
        Map<String, Long> dist = results.stream()
                .filter(ClassificationResult::success)
                .collect(Collectors.groupingBy(
                        ClassificationResult::primaryClass, Collectors.counting()));
        String distStr = dist.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> e.getKey() + "(" + e.getValue() + ")")
                .collect(Collectors.joining("  "));
        distLabel.setText("类别分布: " + (distStr.isEmpty() ? "无" : distStr));
    }

    // ── Row data ─────────────────────────────────────────────────────

    public static class ClassificationRow {
        final int rowNum;
        final String fileName, primaryClass, confDisplay, detDisplay, statusIcon;
        ClassificationRow(int i, ClassificationResult r) {
            this.rowNum = i;
            this.fileName = r.fileName();
            this.primaryClass = r.primaryClass();
            this.confDisplay = r.confidenceDisplay();
            this.detDisplay = r.detectionsDisplay();
            this.statusIcon = r.success() ? "✅" : "❌";
        }
    }
}
