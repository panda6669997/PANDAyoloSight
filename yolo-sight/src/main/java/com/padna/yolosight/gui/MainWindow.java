package com.padna.yolosight.gui;

import com.padna.yolosight.config.AppConfig;
import com.padna.yolosight.model.DetectionResult;
import com.padna.yolosight.model.ModelManager;
import com.padna.yolosight.model.YoloDetector;
import com.padna.yolosight.util.DrawingUtils;
import com.padna.yolosight.util.ImageUtils;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main application window — JavaFX stage setup, layout, and wiring.
 *
 * <pre>
 * ┌─────────────────────────────────────────────┐
 * │  MenuBar: 文件 | 视图 | 帮助                  │
 * │  ToolBar: [打开] [检测] [保存] [适应]          │
 * ├──────────┬──────────────────────────────────┤
 * │ Control  │  ImageCanvas (ScrollPane)        │
 * │ Panel    │                                  │
 * │          │                                  │
 * ├──────────┴──────────────────────────────────┤
 * │  ResultTablePanel                          │
 * ├─────────────────────────────────────────────┤
 * │  StatusBar                                  │
 * └─────────────────────────────────────────────┘
 * </pre>
 */
public class MainWindow {

    private final Stage stage;
    private final BorderPane root;

    // Sub-components
    private final ImageCanvasView imageCanvas;
    private final ControlPanel controlPanel;
    private final ResultTablePanel resultTablePanel;
    private final StatusBar statusBar;
    private final Label statusLabel;

    // State
    private BufferedImage currentImage;
    private File currentImageFile;
    private Button detectBtn;
    private Button saveBtn;
    private ModelManager modelManager;
    private YoloDetector detector;
    private List<DetectionResult> lastDetections;

    public MainWindow(Stage stage) {
        this.stage = stage;
        this.root = new BorderPane();

        imageCanvas = new ImageCanvasView();
        controlPanel = new ControlPanel();
        resultTablePanel = new ResultTablePanel();
        statusBar = new StatusBar();
        statusLabel = new Label("就绪 — 请打开一张图片");

        buildMenuBar();
        buildToolBar();
        buildContentPane();
        setupDragAndDrop();
        wireCallbacks();

        setDetectEnabled(false);
        statusBar.setLeft(statusLabel);
    }

    public BorderPane getRoot() {
        return root;
    }

    // ── Menu bar ───────────────────────────────────────────────────────

    private void buildMenuBar() {
        MenuBar menuBar = new MenuBar();

        // 文件
        Menu fileMenu = new Menu("文件");
        MenuItem openItem = new MenuItem("打开图片...");
        openItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(
                javafx.scene.input.KeyCode.O, javafx.scene.input.KeyCombination.CONTROL_DOWN));
        openItem.setOnAction(e -> onOpenImage());
        MenuItem saveItem = new MenuItem("保存结果...");
        saveItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(
                javafx.scene.input.KeyCode.S, javafx.scene.input.KeyCombination.CONTROL_DOWN));
        saveItem.setOnAction(e -> onSaveResult());
        MenuItem exitItem = new MenuItem("退出");
        exitItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(
                javafx.scene.input.KeyCode.Q, javafx.scene.input.KeyCombination.CONTROL_DOWN));
        exitItem.setOnAction(e -> stage.close());
        fileMenu.getItems().addAll(openItem, saveItem, new SeparatorMenuItem(), exitItem);

        // 视图
        Menu viewMenu = new Menu("视图");
        MenuItem fitItem = new MenuItem("适应窗口");
        fitItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(
                javafx.scene.input.KeyCode.DIGIT0, javafx.scene.input.KeyCombination.CONTROL_DOWN));
        fitItem.setOnAction(e -> imageCanvas.fitToWindow());
        MenuItem zoomInItem = new MenuItem("放大");
        zoomInItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(
                javafx.scene.input.KeyCode.EQUALS, javafx.scene.input.KeyCombination.CONTROL_DOWN));
        zoomInItem.setOnAction(e -> imageCanvas.zoomIn());
        MenuItem zoomOutItem = new MenuItem("缩小");
        zoomOutItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(
                javafx.scene.input.KeyCode.MINUS, javafx.scene.input.KeyCombination.CONTROL_DOWN));
        zoomOutItem.setOnAction(e -> imageCanvas.zoomOut());
        MenuItem resetItem = new MenuItem("原始大小 (100%)");
        resetItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(
                javafx.scene.input.KeyCode.DIGIT1, javafx.scene.input.KeyCombination.CONTROL_DOWN));
        resetItem.setOnAction(e -> imageCanvas.resetZoom());
        viewMenu.getItems().addAll(fitItem, zoomInItem, zoomOutItem, resetItem);

        // 帮助
        Menu helpMenu = new Menu("帮助");
        MenuItem aboutItem = new MenuItem("关于");
        aboutItem.setOnAction(e -> showAboutDialog());
        helpMenu.getItems().add(aboutItem);

        menuBar.getMenus().addAll(fileMenu, viewMenu, helpMenu);
        root.setTop(menuBar);
    }

    // ── Toolbar ────────────────────────────────────────────────────────

    private void buildToolBar() {
        ToolBar toolBar = new ToolBar();
        toolBar.getStyleClass().add("tool-bar");

        Button openBtn = new Button("打开图片");
        openBtn.getStyleClass().add("toolbar-button");
        openBtn.setOnAction(e -> onOpenImage());
        toolBar.getItems().add(openBtn);

        detectBtn = new Button("开始检测");
        detectBtn.getStyleClass().add("toolbar-button");
        detectBtn.setOnAction(e -> onDetect());
        toolBar.getItems().add(detectBtn);

        saveBtn = new Button("保存结果");
        saveBtn.getStyleClass().add("toolbar-button");
        saveBtn.setDisable(true);
        saveBtn.setOnAction(e -> onSaveResult());
        toolBar.getItems().add(saveBtn);

        toolBar.getItems().add(new Separator());

        Button fitBtn = new Button("适应窗口");
        fitBtn.getStyleClass().add("toolbar-button");
        fitBtn.setOnAction(e -> imageCanvas.fitToWindow());
        toolBar.getItems().add(fitBtn);

        // Stack menu + toolbar in a VBox
        // First remove the menu bar we just added
        MenuBar menuBar = (MenuBar) root.getTop();
        root.setTop(new VBox(menuBar, toolBar));
    }

    // ── Content pane ───────────────────────────────────────────────────

    private void buildContentPane() {
        // Canvas in scroll pane
        ScrollPane canvasScroll = new ScrollPane(imageCanvas.getView());
        canvasScroll.getStyleClass().add("canvas-viewport");
        canvasScroll.setFitToWidth(true);
        canvasScroll.setFitToHeight(true);
        canvasScroll.setPannable(true);

        // Right side: canvas on top, result table at bottom
        SplitPane rightSplit = new SplitPane();
        rightSplit.setOrientation(javafx.geometry.Orientation.VERTICAL);
        rightSplit.setDividerPositions(0.72);
        rightSplit.getItems().addAll(canvasScroll, resultTablePanel.getView());

        // Main split: ControlPanel (left) | Canvas+Table (right)
        SplitPane mainSplit = new SplitPane();
        mainSplit.setDividerPositions(0.22);
        mainSplit.getItems().addAll(controlPanel.getView(), rightSplit);

        root.setCenter(mainSplit);
        root.setBottom(statusBar.getView());
    }

    // ── Drag & drop ────────────────────────────────────────────────────

    private void setupDragAndDrop() {
        imageCanvas.getView().setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) {
                e.acceptTransferModes(TransferMode.COPY);
            }
            e.consume();
        });
        imageCanvas.getView().setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasFiles() && !db.getFiles().isEmpty()) {
                loadImageFile(db.getFiles().get(0));
            }
            e.setDropCompleted(true);
            e.consume();
        });
    }

    // ── Callbacks ──────────────────────────────────────────────────────

    private void wireCallbacks() {
        controlPanel.setOnDetectAction(this::onDetect);
        controlPanel.setOnConfidenceChanged(conf -> {
            if (detector != null) onDetect();
        });
        resultTablePanel.setOnRowSelected(det -> {
            imageCanvas.setHighlighted(det);
            imageCanvas.requestRedraw();
        });
    }

    // ── Actions ────────────────────────────────────────────────────────

    private void onOpenImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("打开图片");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("图片文件",
                        "*.jpg", "*.jpeg", "*.png", "*.bmp", "*.webp"));
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            loadImageFile(file);
        }
    }

    private void loadImageFile(File file) {
        statusLabel.setText("加载中: " + file.getName() + "...");

        Task<BufferedImage> loadTask = new Task<>() {
            @Override
            protected BufferedImage call() throws Exception {
                return ImageUtils.loadImage(file);
            }
        };
        loadTask.setOnSucceeded(e -> {
            try {
                currentImage = loadTask.getValue();
                currentImageFile = file;
                Image fxImage = SwingFXUtils.toFXImage(currentImage, null);
                imageCanvas.setImage(fxImage);

                long fileSize = file.length();
                int w = currentImage.getWidth();
                int h = currentImage.getHeight();
                statusBar.setRight(w + "×" + h + " | " + ImageUtils.formatFileSize(fileSize));
                statusLabel.setText("已加载: " + file.getName());
                setDetectEnabled(true);
                saveBtn.setDisable(true);
            } catch (Exception ex) {
                statusLabel.setText("加载失败: " + ex.getMessage());
            }
        });
        loadTask.setOnFailed(e -> {
            statusLabel.setText("加载失败: " + loadTask.getException().getMessage());
        });
        new Thread(loadTask).start();
    }

    private void onDetect() {
        if (currentImage == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("无图片");
            alert.setHeaderText("请先打开一张图片。");
            alert.show();
            return;
        }

        detectBtn.setDisable(true);
        controlPanel.setDetectEnabled(false);
        statusLabel.setText("正在检测...");
        float currentThreshold = controlPanel.getConfidenceThreshold();

        long[] elapsedHolder = new long[1];
        Task<List<DetectionResult>> detectTask = new Task<>() {
            @Override
            protected List<DetectionResult> call() throws Exception {
                long start = System.currentTimeMillis();
                if (modelManager == null) {
                    updateMessage("正在加载 YOLOv8 模型（首次启动需下载约 6MB）...");
                    modelManager = new ModelManager();
                    try {
                        modelManager.loadModel();
                    } catch (Exception e) {
                        throw new Exception("模型加载失败: " + e, e);
                    }
                    updateMessage("模型已加载，正在推理...");
                }
                detector = new YoloDetector(modelManager, currentThreshold);
                List<DetectionResult> results = detector.detect(currentImage);
                elapsedHolder[0] = System.currentTimeMillis() - start;
                return results;
            }
        };

        detectTask.messageProperty().addListener((obs, old, msg) ->
                Platform.runLater(() -> statusLabel.setText(msg)));

        detectTask.setOnSucceeded(e -> {
            lastDetections = detectTask.getValue();
            imageCanvas.setDetections(lastDetections);
            imageCanvas.setHighlighted(null);
            imageCanvas.requestRedraw();
            resultTablePanel.setResults(lastDetections);

            List<String> classNames = lastDetections.stream()
                    .map(DetectionResult::className)
                    .distinct().sorted().collect(Collectors.toList());
            controlPanel.updateClassFilter(classNames);
            controlPanel.setSummary(lastDetections.size(), elapsedHolder[0]);

            statusLabel.setText(String.format("检测完成，耗时 %,d 毫秒 — 检测到 %d 个物体",
                    elapsedHolder[0], lastDetections.size()));
            saveBtn.setDisable(false);
            detectBtn.setDisable(false);
            controlPanel.setDetectEnabled(true);
        });

        detectTask.setOnFailed(e -> {
            Throwable cause = detectTask.getException();
            if (cause.getCause() != null) cause = cause.getCause();
            statusLabel.setText("检测失败: " + cause.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("检测错误");
            alert.setHeaderText("检测失败");
            alert.setContentText(cause.toString());
            alert.show();
            detectBtn.setDisable(false);
            controlPanel.setDetectEnabled(true);
        });

        new Thread(detectTask).start();
    }

    private void onSaveResult() {
        if (currentImage == null || lastDetections == null || lastDetections.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("无可保存内容");
            alert.setHeaderText("没有可保存的检测结果。\n请先运行检测。");
            alert.show();
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("保存结果图片");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG 图片", "*.png"),
                new FileChooser.ExtensionFilter("JPEG 图片", "*.jpg", "*.jpeg"));
        if (currentImageFile != null) {
            String name = currentImageFile.getName();
            int dot = name.lastIndexOf('.');
            String base = dot > 0 ? name.substring(0, dot) : name;
            chooser.setInitialFileName(base + "_detected.png");
        }
        File outFile = chooser.showSaveDialog(stage);
        if (outFile == null) return;

        String format = outFile.getName().toLowerCase().endsWith(".png") ? "png" : "jpg";
        Task<Void> saveTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                BufferedImage rendered = new BufferedImage(
                        currentImage.getWidth(), currentImage.getHeight(),
                        BufferedImage.TYPE_INT_RGB);
                java.awt.Graphics2D g2d = rendered.createGraphics();
                g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                        java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.drawImage(currentImage, 0, 0, null);
                DrawingUtils.drawDetections(g2d, lastDetections);
                g2d.dispose();
                ImageIO.write(rendered, format, outFile);
                return null;
            }
        };
        saveTask.setOnSucceeded(e -> statusLabel.setText("已保存: " + outFile.getName()));
        saveTask.setOnFailed(e -> {
            statusLabel.setText("保存失败: " + saveTask.getException().getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("保存错误");
            alert.setHeaderText("保存图片失败");
            alert.setContentText(saveTask.getException().toString());
            alert.show();
        });
        new Thread(saveTask).start();
    }

    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("关于 " + AppConfig.APP_NAME);
        alert.setHeaderText(AppConfig.APP_NAME + " v" + AppConfig.APP_VERSION);
        alert.setContentText("基于 YOLOv8 的物体检测\n"
                + "技术栈: DJL + ONNX Runtime\n"
                + "界面: JavaFX");
        alert.show();
    }

    private void setDetectEnabled(boolean enabled) {
        detectBtn.setDisable(!enabled);
        controlPanel.setDetectEnabled(enabled);
    }

}
