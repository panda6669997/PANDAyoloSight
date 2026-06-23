# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在此仓库中工作时提供指导。

## 构建与运行

```bash
mvn clean compile               # 仅编译
mvn test                        # 运行全部测试（首次需联网下载模型）
mvn package                     # 生成 fat JAR 到 target/yolo-sight-1.0.0.jar
java -jar target/yolo-sight-1.0.0.jar

# 运行单个测试类
mvn test -Dtest=YoloDetectorTest
```

需要 **JDK 17+** 和 **Maven 3.8+**。fat JAR 由 `maven-shade-plugin` 配合 `ServicesResourceTransformer` 生成（确保 ONNX Runtime 原生 SPI 服务在合并后不丢失）。

## 架构

这是一个 **Java Swing 桌面应用**，使用 **DJL (Deep Java Library)** 通过 ONNX Runtime 对用户选择的图片运行 YOLOv8 目标检测。

### 线程模型

**所有 I/O 和推理必须在 `SwingWorker` 后台线程上运行** — 绝不能占用 EDT。该模式在 `MainFrame` 中用于三处：
- `loadImageFile()` — 后台从磁盘读取图片
- `onDetect()` — 后台下载模型（首次启动）+ 推理
- `onSaveResult()` — 后台渲染标注图片 + 写入磁盘

`SwingWorker.done()` 始终在 EDT 上运行，因此结果在这里交还给 GUI 组件（`imageCanvas.setDetections()`、`resultTablePanel.setResults()` 等）。

### 推理管道

```
BufferedImage (TYPE_3BYTE_BGR)
  → ImageFactory.getInstance().fromImage() → DJL Image
  → Predictor.predict()                    → DetectedObjects
  → 按置信度过滤，映射为 DetectionResult record
  → 按置信度降序排列
```

`Predictor<Image, DetectedObjects>` **不是线程安全的** — 每次 `YoloDetector.detect()` 调用都会创建新的 Predictor 并在 try-with-resources 中关闭。底层的 `ZooModel`（由 `ModelManager` 持有）是线程安全的、可共享的。

DJL 的 `Criteria`（`ModelManager.java:34-42`）通过 `optApplication(OBJECT_DETECTION)` 自动选择 YoloV8Translator。该 translator 负责 letterbox-resize 到 640×640、通道转换、归一化和 NMS 后处理。`optEngine("OnnxRuntime")` 选择 ONNX Runtime 作为推理后端。

### 模型生命周期

`ModelManager.loadModel()` 在首次点击"检测"时延迟调用。DJL 的 `Criteria.loadModel()` 先检查 `~/.djl.ai/cache/` — 如果模型未缓存，则从 DJL 模型仓库下载约 6 MB。后续启动直接命中缓存，无需网络。模型下载在 `SwingWorker` 线程上进行，`ProgressBar` 输出到控制台。

### 坐标系统

- **DetectionResult.boundingBox()** — 存储的是**原始图片**的像素坐标（非屏幕/缩放后坐标）
- **ImageCanvas** 使用 `AffineTransform`（平移 + 缩放）将图片坐标映射到屏幕坐标。`DrawingUtils.drawDetections()` 接收的是变换**之后**的 `Graphics2D`，因此直接在图片像素空间中绘制。
- **鼠标滚轮缩放** 会调整 `panX/panY`，使光标指向的位置在缩放前后保持不变。
- `getPreferredSize()` 返回缩放后的尺寸，使外层 `JScrollPane` 的滚动条能正确跟踪；每次缩放后必须调用 `revalidate()`。

### GUI 组件树

```
MainFrame (JFrame, BorderLayout)
├── NORTH: JToolBar [打开 | 检测 | 保存结果 | 适应窗口]
├── CENTER: JSplitPane (水平分割)
│   ├── LEFT: ControlPanel（固定约 260px）
│   └── RIGHT: JSplitPane (垂直分割)
│       ├── TOP: JScrollPane → ImageCanvas
│       └── BOTTOM: ResultTablePanel
└── SOUTH: StatusBar
```

`MainFrame` 拥有所有组件的连接关系 — `ControlPanel` 和 `ResultTablePanel` 通过 `MainFrame` 设置的回调（`Runnable`、`Consumer<T>`）进行通信，而不是直接相互引用。

### 关键常量

全部在 `AppConfig.java` 中：模型名称（`yolov8n`）、输入尺寸（640）、默认置信度（0.5）、NMS 阈值（0.45）、缩放范围（0.1–10.0）、窗口尺寸。

### 图片格式要求

图片必须为 `BufferedImage.TYPE_3BYTE_BGR` 才能与 DJL 兼容。`ImageUtils.loadImage()` 和 `convertToBGR()` 负责此转换。保存标注结果时改用 `TYPE_INT_RGB`（兼容 JPEG 格式）。
