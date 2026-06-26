package com.padna.yolosight.gui;

import com.padna.yolosight.config.AppConfig;
import com.padna.yolosight.model.*;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Main window — pure batch classification mode.
 * Right panel shows thumbnails of classified images after batch completes.
 */
public class MainWindow {

    private final Stage stage;
    private final BorderPane root;

    private final ThumbnailGridPanel thumbnailGrid;
    private final BatchResultPanel batchResultPanel;
    private final StatusBar statusBar;
    private final Label statusLabel;

    private Button batchBtn;
    private Button bigBatchBtn;
    private Button stopBtn;
    private Button resumeBtn;
    private Label sidebarInfo;
    private ProgressBar sidebarProgress;
    private ModelManager modelManager;
    private File lastOutputDir;
    private File lastSourceDir;
    private BatchClassifier runningClassifier;

    public MainWindow(Stage stage) {
        this.stage = stage;
        this.root = new BorderPane();

        thumbnailGrid = new ThumbnailGridPanel();
        batchResultPanel = new BatchResultPanel();
        statusBar = new StatusBar();
        statusLabel = new Label("就绪 — 点击「批量分类」开始");

        buildMenuBar();
        buildToolBar();
        buildContentPane();

        batchBtn.setDisable(false);
        statusBar.setLeft(statusLabel);
    }

    public BorderPane getRoot() { return root; }

    // ── Menu ───────────────────────────────────────────────────────────

    private void buildMenuBar() {
        MenuBar menuBar = new MenuBar();

        Menu fileMenu = new Menu("文件");
        MenuItem exitItem = new MenuItem("退出");
        exitItem.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN));
        exitItem.setOnAction(e -> stage.close());
        fileMenu.getItems().add(exitItem);

        Menu helpMenu = new Menu("帮助");
        MenuItem aboutItem = new MenuItem("关于");
        aboutItem.setOnAction(e -> showAboutDialog());
        helpMenu.getItems().add(aboutItem);

        menuBar.getMenus().addAll(fileMenu, helpMenu);
        root.setTop(menuBar);
    }

    // ── Toolbar ────────────────────────────────────────────────────────

    private void buildToolBar() {
        ToolBar toolBar = new ToolBar();

        batchBtn = new Button("批量分类");
        batchBtn.getStyleClass().add("toolbar-button");
        batchBtn.setOnAction(e -> onBatchClassify());
        toolBar.getItems().add(batchBtn);

        MenuBar menuBar = (MenuBar) root.getTop();
        root.setTop(new VBox(menuBar, toolBar));
    }

    // ── Content ────────────────────────────────────────────────────────

    private void buildContentPane() {
        // Left sidebar
        VBox sidebar = new VBox(12);
        sidebar.setPadding(new Insets(14, 12, 14, 12));
        sidebar.setStyle("-fx-background-color: #1e1e22;");
        sidebar.setPrefWidth(240);

        bigBatchBtn = new Button("批量分类");
        bigBatchBtn.getStyleClass().add("accent-button");
        bigBatchBtn.setMaxWidth(Double.MAX_VALUE);
        bigBatchBtn.setPrefHeight(44);
        bigBatchBtn.setOnAction(e -> onBatchClassify());
        batchBtn.disableProperty().addListener((obs, o, n) -> bigBatchBtn.setDisable(n));

        sidebarProgress = new ProgressBar(0);
        sidebarProgress.setMaxWidth(Double.MAX_VALUE);
        sidebarProgress.setPrefHeight(8);

        stopBtn = new Button("停止");
        stopBtn.setMaxWidth(Double.MAX_VALUE);
        stopBtn.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-background-radius: 4;");
        stopBtn.setVisible(false);
        stopBtn.setOnAction(e -> {
            if (runningClassifier != null) runningClassifier.cancel();
            stopBtn.setDisable(true);
            stopBtn.setText("停止中...");
        });

        resumeBtn = new Button("继续上次任务");
        resumeBtn.setMaxWidth(Double.MAX_VALUE);
        resumeBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-background-radius: 4;");
        resumeBtn.setVisible(false);
        resumeBtn.setOnAction(e -> resumeBatch());

        sidebarInfo = new Label("选择源文件夹和输出目录\n即可批量分类所有图片");
        sidebarInfo.setWrapText(true);
        sidebarInfo.setStyle("-fx-text-fill: #808085; -fx-font-size: 12px;");

        sidebar.getChildren().addAll(bigBatchBtn, sidebarProgress, stopBtn, resumeBtn, sidebarInfo);
        ScrollPane sidebarScroll = new ScrollPane(sidebar);
        sidebarScroll.setFitToWidth(true);
        sidebarScroll.setStyle("-fx-background-color: #1e1e22;");

        // Right: thumbnail grid on top, batch results at bottom
        SplitPane rightSplit = new SplitPane();
        rightSplit.setOrientation(javafx.geometry.Orientation.VERTICAL);
        rightSplit.setDividerPositions(0.65);
        rightSplit.getItems().addAll(thumbnailGrid.getView(), batchResultPanel.getView());

        SplitPane mainSplit = new SplitPane();
        mainSplit.setDividerPositions(0.19);
        mainSplit.getItems().addAll(sidebarScroll, rightSplit);

        root.setCenter(mainSplit);
        root.setBottom(statusBar.getView());
    }

    // ── Batch classify (select folders + confirm) ────────────────────────

    private void onBatchClassify() {
        DirectoryChooser srcChooser = new DirectoryChooser();
        srcChooser.setTitle("选择包含图片的源文件夹");
        File sourceDir = srcChooser.showDialog(stage);
        if (sourceDir == null) return;

        DirectoryChooser outChooser = new DirectoryChooser();
        outChooser.setTitle("选择分类输出目标文件夹");
        File outputDir = outChooser.showDialog(stage);
        if (outputDir == null) return;

        // Resume or restart?
        boolean resumeMode = false;
        if (BatchClassifier.canResume(outputDir)) {
            int done = BatchClassifier.resumeCount(outputDir);
            Alert choice = new Alert(Alert.AlertType.CONFIRMATION);
            choice.setTitle("检测到未完成任务");
            choice.setHeaderText("该输出目录有未完成的任务（已完成 " + done + " 张）");
            choice.setContentText("点击「确定」从中断处继续\n点击「取消」重新开始（会覆盖已有文件）");
            var result = choice.showAndWait();
            if (result.isEmpty()) return;
            resumeMode = (result.get() == ButtonType.OK);
            if (!resumeMode) {
                // Delete state file to start fresh
                new File(outputDir, ".batch_state.txt").delete();
            }
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("确认批量分类");
        confirm.setHeaderText(resumeMode ? "从中断处继续分类" : "全新批量分类");
        confirm.setContentText("源: " + sourceDir.getAbsolutePath()
                + "\n输出: " + outputDir.getAbsolutePath()
                + (resumeMode ? "\n\n跳过已完成的 " + BatchClassifier.resumeCount(outputDir) + " 张" : "")
                + "\n\n图片存入类别子目录。继续？");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        lastSourceDir = sourceDir;
        lastOutputDir = outputDir;
        resumeBtn.setVisible(false);  // new batch, hide old resume button
        doBatchProcess(sourceDir, outputDir);
    }

    // ── Resume (one-click continue) ────────────────────────────────────

    private void resumeBatch() {
        if (lastSourceDir == null || lastOutputDir == null
                || !BatchClassifier.canResume(lastOutputDir)) {
            resumeBtn.setVisible(false);
            return;
        }
        resumeBtn.setVisible(false);
        doBatchProcess(lastSourceDir, lastOutputDir);
    }

    // ── Core batch processing ──────────────────────────────────────────

    private void doBatchProcess(File sourceDir, File outputDir) {
        batchResultPanel.showProgress(true);
        batchResultPanel.updateProgress(0, 1);
        sidebarProgress.setProgress(0);
        stopBtn.setVisible(true);
        stopBtn.setDisable(false);
        stopBtn.setText("停止");
        statusLabel.setText("正在批量分类...");
        batchBtn.setDisable(true);
        bigBatchBtn.setDisable(true);

        Task<List<ClassificationResult>> batchTask = new Task<>() {
            @Override
            protected List<ClassificationResult> call() throws Exception {
                if (modelManager == null) {
                    updateMessage("正在加载模型...");
                    modelManager = new ModelManager();
                    modelManager.loadModel();
                }
                runningClassifier = new BatchClassifier(modelManager,
                        AppConfig.DEFAULT_CONFIDENCE_THRESHOLD, false);
                return runningClassifier.classifyFolder(sourceDir, outputDir,
                        (cur, total) -> { updateProgress(cur, total); updateMessage(cur + "/" + total); });
            }
        };

        batchTask.progressProperty().addListener((obs, old, p) ->
                Platform.runLater(() -> sidebarProgress.setProgress(p.doubleValue())));
        batchTask.messageProperty().addListener((obs, old, msg) ->
                Platform.runLater(() -> batchResultPanel.updateProgress(
                        (int)(batchTask.getProgress() * 100), 100)));

        batchTask.setOnSucceeded(e -> {
            List<ClassificationResult> results = batchTask.getValue();
            boolean wasCancelled = runningClassifier != null && runningClassifier.isCancelled();
            runningClassifier = null;
            stopBtn.setVisible(false);
            resumeBtn.setVisible(wasCancelled && BatchClassifier.canResume(outputDir));

            batchResultPanel.setResults(results, outputDir);
            batchResultPanel.showProgress(false);
            thumbnailGrid.populateFrom(results, outputDir);

            long success = results.stream().filter(ClassificationResult::success).count();
            Map<String, Long> dist = results.stream()
                    .filter(ClassificationResult::success)
                    .collect(Collectors.groupingBy(ClassificationResult::primaryClass, Collectors.counting()));
            String top = dist.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(4).map(en -> en.getKey() + "(" + en.getValue() + ")")
                    .collect(Collectors.joining(" "));
            sidebarInfo.setText(String.format("共 %d 张, 成功 %d 张%s",
                    results.size(), success,
                    wasCancelled ? "\n(已暂停，可继续)" : "\n" + (top.isEmpty() ? "无" : top)));
            statusLabel.setText(String.format(wasCancelled ? "已暂停: %d 张 → %s" : "完成: %d 张 → %s",
                    results.size(), outputDir.getAbsolutePath()));
            batchBtn.setDisable(false);
            bigBatchBtn.setDisable(false);
        });

        batchTask.setOnFailed(ev -> {
            runningClassifier = null;
            stopBtn.setVisible(false);
            batchResultPanel.showProgress(false);
            statusLabel.setText("失败: " + batchTask.getException().getMessage());
            batchBtn.setDisable(false);
            bigBatchBtn.setDisable(false);
            new Alert(Alert.AlertType.ERROR, "批量分类出错\n" + batchTask.getException().toString()).show();
        });

        new Thread(batchTask).start();
    }

    // ── About ──────────────────────────────────────────────────────────

    private void showAboutDialog() {
        new Alert(Alert.AlertType.INFORMATION,
                AppConfig.APP_NAME + " v" + AppConfig.APP_VERSION + "\n"
                        + "批量图片物体识别与分类\n"
                        + "技术栈: DJL + ONNX Runtime + JavaFX").show();
    }
}
