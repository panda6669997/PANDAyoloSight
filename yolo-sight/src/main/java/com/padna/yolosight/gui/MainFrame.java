package com.padna.yolosight.gui;

import com.padna.yolosight.config.AppConfig;
import com.padna.yolosight.model.DetectionResult;
import com.padna.yolosight.model.ModelManager;
import com.padna.yolosight.model.YoloDetector;
import com.padna.yolosight.util.DrawingUtils;
import com.padna.yolosight.util.ImageUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main application window.
 *
 * <pre>
 * ┌─────────────────────────────────────────────────┐
 * │  Menu Bar: File | View | Help                   │
 * │  Toolbar: [Open] [Detect] [Save] [Zoom]        │
 * ├───────────┬─────────────────────────────────────┤
 * │ Control   │  ImageCanvas (JScrollPane)          │
 * │ Panel     │                                     │
 * │           │                                     │
 * ├───────────┴─────────────────────────────────────┤
 * │  StatusBar                                      │
 * └─────────────────────────────────────────────────┘
 * </pre>
 */
public class MainFrame extends JFrame {

    // ── Sub-components ──────────────────────────────────────────────────
    private final ImageCanvas imageCanvas;
    private final JScrollPane scrollPane;
    private final ControlPanel controlPanel;
    private final ResultTablePanel resultTablePanel;
    private final StatusBar statusBar;

    // ── State ──────────────────────────────────────────────────────────
    private BufferedImage currentImage;
    private File currentImageFile;
    private JButton detectBtn;
    private JButton saveBtn;

    // Model / inference (lazy-initialized on first Detect click)
    private ModelManager modelManager;
    private YoloDetector detector;
    private List<DetectionResult> lastDetections;

    public MainFrame() {
        super(AppConfig.APP_NAME);
        configureWindow();

        // Create sub-panels
        imageCanvas = new ImageCanvas();
        scrollPane = new JScrollPane(imageCanvas);
        controlPanel = new ControlPanel();
        resultTablePanel = new ResultTablePanel();
        statusBar = new StatusBar();

        // Wire ControlPanel callbacks
        controlPanel.setOnDetectAction(this::onDetect);
        controlPanel.setOnConfidenceChanged(conf -> {
            if (detector != null) {
                // Re-run detection with new threshold (uses existing model)
                onDetect();
            }
        });
        controlPanel.setDetectEnabled(false);

        // Wire ResultTablePanel: click row → highlight on canvas
        resultTablePanel.setOnRowSelected(det -> {
            imageCanvas.setHighlighted(det);
            imageCanvas.repaint();
        });

        // Build layout
        buildMenuBar();
        buildToolBar();
        buildContentPane();

        // Drag & drop support
        setupDragAndDrop();

        // Initial state
        setDetectEnabled(false);
        statusBar.setStatus("就绪 — 请打开一张图片");
    }

    // ── Window configuration ───────────────────────────────────────────

    private void configureWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(AppConfig.WINDOW_WIDTH, AppConfig.WINDOW_HEIGHT);
        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(null);
    }

    // ── Menu bar ───────────────────────────────────────────────────────

    private void buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // ── 文件菜单 ──────────────────────────────────────────────
        JMenu fileMenu = new JMenu("文件");
        fileMenu.setMnemonic('F');

        JMenuItem openItem = new JMenuItem("打开图片...");
        openItem.setMnemonic('O');
        openItem.setAccelerator(KeyStroke.getKeyStroke("control O"));
        openItem.addActionListener(e -> onOpenImage());
        fileMenu.add(openItem);

        JMenuItem saveItem = new JMenuItem("保存结果...");
        saveItem.setMnemonic('S');
        saveItem.setAccelerator(KeyStroke.getKeyStroke("control S"));
        saveItem.addActionListener(e -> onSaveResult());
        fileMenu.add(saveItem);

        fileMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem("退出");
        exitItem.setMnemonic('x');
        exitItem.setAccelerator(KeyStroke.getKeyStroke("control Q"));
        exitItem.addActionListener(e -> dispose());
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);

        // ── 视图菜单 ──────────────────────────────────────────────
        JMenu viewMenu = new JMenu("视图");
        viewMenu.setMnemonic('V');

        JMenuItem fitItem = new JMenuItem("适应窗口");
        fitItem.setAccelerator(KeyStroke.getKeyStroke("control 0"));
        fitItem.addActionListener(e -> imageCanvas.fitToWindow());
        viewMenu.add(fitItem);

        JMenuItem zoomInItem = new JMenuItem("放大");
        zoomInItem.setAccelerator(KeyStroke.getKeyStroke("control EQUALS"));
        zoomInItem.addActionListener(e -> imageCanvas.zoomIn());
        viewMenu.add(zoomInItem);

        JMenuItem zoomOutItem = new JMenuItem("缩小");
        zoomOutItem.setAccelerator(KeyStroke.getKeyStroke("control MINUS"));
        zoomOutItem.addActionListener(e -> imageCanvas.zoomOut());
        viewMenu.add(zoomOutItem);

        JMenuItem resetZoomItem = new JMenuItem("原始大小 (100%)");
        resetZoomItem.setAccelerator(KeyStroke.getKeyStroke("control 1"));
        resetZoomItem.addActionListener(e -> imageCanvas.resetZoom());
        viewMenu.add(resetZoomItem);

        menuBar.add(viewMenu);

        // ── 帮助菜单 ──────────────────────────────────────────────
        JMenu helpMenu = new JMenu("帮助");
        helpMenu.setMnemonic('H');

        JMenuItem aboutItem = new JMenuItem("关于");
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    // ── Toolbar ────────────────────────────────────────────────────────

    private void buildToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton openBtn = new JButton("打开");
        openBtn.setToolTipText("打开图片文件 (Ctrl+O)");
        openBtn.addActionListener(e -> onOpenImage());
        toolBar.add(openBtn);

        toolBar.addSeparator();

        detectBtn = new JButton("检测");
        detectBtn.setToolTipText("运行物体检测 (Ctrl+D)");
        detectBtn.addActionListener(e -> onDetect());
        toolBar.add(detectBtn);

        saveBtn = new JButton("保存结果");
        saveBtn.setToolTipText("保存标注后的图片 (Ctrl+S)");
        saveBtn.setEnabled(false);
        saveBtn.addActionListener(e -> onSaveResult());
        toolBar.add(saveBtn);

        toolBar.addSeparator();

        JButton fitBtn = new JButton("适应");
        fitBtn.setToolTipText("适应窗口大小 (Ctrl+0)");
        fitBtn.addActionListener(e -> imageCanvas.fitToWindow());
        toolBar.add(fitBtn);

        add(toolBar, BorderLayout.NORTH);
    }

    // ── Content pane ───────────────────────────────────────────────────

    private void buildContentPane() {
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Color.DARK_GRAY);

        // Right side: canvas on top, result table at bottom
        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                scrollPane, resultTablePanel);
        rightSplit.setResizeWeight(1.0); // canvas takes extra space
        rightSplit.setDividerLocation(500);
        rightSplit.setDividerSize(4);

        // Main split: ControlPanel (left) | Canvas+Table (right)
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                controlPanel, rightSplit);
        mainSplit.setResizeWeight(0.0); // keep left panel fixed-size
        mainSplit.setDividerLocation(AppConfig.CONTROL_PANEL_WIDTH);
        mainSplit.setDividerSize(4);

        add(mainSplit, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
    }

    // ── Drag & drop ────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void setupDragAndDrop() {
        imageCanvas.setDropTarget(new DropTarget() {
            @Override
            public synchronized void drop(DropTargetDropEvent evt) {
                evt.acceptDrop(DnDConstants.ACTION_COPY);
                try {
                    List<File> droppedFiles = (List<File>) evt.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                    if (!droppedFiles.isEmpty()) {
                        File file = droppedFiles.get(0);
                        loadImageFile(file);
                    }
                } catch (Exception ex) {
                    statusBar.setStatus("拖放失败: " + ex.getMessage());
                }
            }
        });
    }

    // ── Actions ────────────────────────────────────────────────────────

    private void onOpenImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("打开图片");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "图片文件", AppConfig.SUPPORTED_EXTENSIONS));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            loadImageFile(chooser.getSelectedFile());
        }
    }

    /**
     * Load an image file in a background thread, then display it.
     */
    private void loadImageFile(File file) {
        statusBar.setStatus("加载中: " + file.getName() + "...");

        new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                return ImageUtils.loadImage(file);
            }

            @Override
            protected void done() {
                try {
                    currentImage = get();
                    currentImageFile = file;
                    imageCanvas.setImage(currentImage);

                    long fileSize = file.length();
                    int w = currentImage.getWidth();
                    int h = currentImage.getHeight();
                    statusBar.setImageInfo(w + "×" + h + " | "
                            + ImageUtils.formatFileSize(fileSize));
                    statusBar.setStatus("已加载: " + file.getName());

                    setDetectEnabled(true);
                    saveBtn.setEnabled(false); // no detections yet
                } catch (Exception e) {
                    statusBar.setStatus("加载图片失败: " + e.getMessage());
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "无法加载图片:\n" + e.getMessage(),
                            "加载错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void onDetect() {
        if (currentImage == null) {
            JOptionPane.showMessageDialog(this,
                    "请先打开一张图片。",
                    "无图片", JOptionPane.WARNING_MESSAGE);
            return;
        }

        detectBtn.setEnabled(false);
        controlPanel.setDetectEnabled(false);
        statusBar.setStatus("正在检测...");

        // Get current confidence threshold from the slider
        float currentThreshold = controlPanel.getConfidenceThreshold();

        new SwingWorker<List<DetectionResult>, Void>() {
            private long elapsedMs;

            @Override
            protected List<DetectionResult> doInBackground() throws Exception {
                long start = System.currentTimeMillis();

                // Lazy-init model on first call (may download ~6 MB)
                if (modelManager == null) {
                    statusBar.setStatus("正在加载 YOLOv8 模型（首次启动需下载约 6MB）...");
                    modelManager = new ModelManager();
                    try {
                        modelManager.loadModel();
                    } catch (Exception e) {
                        // Wrap with clear message so the user sees the real cause
                        throw new Exception("模型加载失败: " + e.toString(), e);
                    }
                    statusBar.setStatus("模型已加载，正在推理...");
                }

                // Create detector with current threshold each time
                detector = new YoloDetector(modelManager, currentThreshold);

                List<DetectionResult> results = detector.detect(currentImage);
                elapsedMs = System.currentTimeMillis() - start;
                return results;
            }

            @Override
            protected void done() {
                try {
                    lastDetections = get();
                    imageCanvas.setDetections(lastDetections);
                    imageCanvas.setHighlighted(null);
                    imageCanvas.repaint();

                    // Update result table
                    resultTablePanel.setResults(lastDetections);

                    // Update class filter dropdown with unique class names
                    List<String> classNames = lastDetections.stream()
                            .map(DetectionResult::className)
                            .distinct()
                            .sorted()
                            .collect(Collectors.toList());
                    controlPanel.updateClassFilter(classNames);
                    controlPanel.setSummary(lastDetections.size(), elapsedMs);

                    statusBar.setStatus(String.format(
                            "检测完成，耗时 %,d 毫秒 — 检测到 %d 个物体",
                            elapsedMs,
                            lastDetections.size()));
                    saveBtn.setEnabled(true);
                } catch (Exception e) {
                    // Unwrap to get the real cause
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    String msg = cause.toString();
                    // If there's a nested cause, include it too
                    if (cause.getCause() != null) {
                        msg += "\n原因: " + cause.getCause().toString();
                    }
                    statusBar.setStatus("检测失败: " + cause.getMessage());
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "检测失败:\n" + msg,
                            "检测错误", JOptionPane.ERROR_MESSAGE);
                    // Also print full stack trace to console for debugging
                    e.printStackTrace();
                } finally {
                    detectBtn.setEnabled(true);
                    controlPanel.setDetectEnabled(true);
                }
            }
        }.execute();
    }

    private void onSaveResult() {
        if (currentImage == null || lastDetections == null || lastDetections.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "没有可保存的检测结果。\n请先运行检测。",
                    "无可保存内容", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("保存结果图片");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "PNG 图片", "png"));
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "JPEG 图片", "jpg", "jpeg"));

        // Suggest filename based on original
        if (currentImageFile != null) {
            String name = currentImageFile.getName();
            int dot = name.lastIndexOf('.');
            String base = dot > 0 ? name.substring(0, dot) : name;
            chooser.setSelectedFile(new File(
                    currentImageFile.getParent(), base + "_detected.png"));
        }

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File outFile = chooser.getSelectedFile();
            String format = outFile.getName().toLowerCase().endsWith(".png") ? "png" : "jpg";

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    // Render image + detections to a new BufferedImage
                    BufferedImage rendered = new BufferedImage(
                            currentImage.getWidth(),
                            currentImage.getHeight(),
                            BufferedImage.TYPE_INT_RGB);
                    Graphics2D g2d = rendered.createGraphics();
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2d.drawImage(currentImage, 0, 0, null);
                    DrawingUtils.drawDetections(g2d, lastDetections);
                    g2d.dispose();

                    ImageIO.write(rendered, format, outFile);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        statusBar.setStatus("已保存: " + outFile.getName());
                    } catch (Exception e) {
                        statusBar.setStatus("保存失败: " + e.getMessage());
                        JOptionPane.showMessageDialog(MainFrame.this,
                                "保存图片失败:\n" + e.getMessage(),
                                "保存错误", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }

    private void showAboutDialog() {
        JOptionPane.showMessageDialog(this,
                AppConfig.APP_NAME + " v" + AppConfig.APP_VERSION + "\n\n"
                        + "基于 YOLOv8 的物体检测\n"
                        + "技术栈: DJL (Deep Java Library) + ONNX Runtime\n"
                        + "界面: Swing + FlatLaf",
                "关于 " + AppConfig.APP_NAME,
                JOptionPane.INFORMATION_MESSAGE);
    }

    // ── State helpers ──────────────────────────────────────────────────

    private void setDetectEnabled(boolean enabled) {
        detectBtn.setEnabled(enabled);
        controlPanel.setDetectEnabled(enabled);
    }

    public BufferedImage getCurrentImage() {
        return currentImage;
    }

    public File getCurrentImageFile() {
        return currentImageFile;
    }

    public StatusBar getStatusBar() {
        return statusBar;
    }

    public ImageCanvas getImageCanvas() {
        return imageCanvas;
    }
}
