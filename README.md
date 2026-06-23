# PANDAyoloSight

Desktop application for object detection in images, powered by **YOLOv8**.

Built with:
- **Java 17+** + **Swing** + **FlatLaf** (modern desktop GUI)
- **DJL** (Deep Java Library) — Java-native deep learning
- **ONNX Runtime** — lightweight inference engine
- **YOLOv8n** — pretrained on COCO (80 classes)

## Features

- Open any image (JPG, PNG, BMP, WebP)
- Drag & drop images directly onto the window
- Run YOLOv8 object detection with one click
- Adjust confidence threshold via slider (live re-detection)
- Filter results by object class
- Zoom (mouse wheel) and pan (click-drag) the image
- View detailed results in a sortable table
- Click a table row to highlight the bounding box
- Save annotated images with bounding boxes and labels

## Prerequisites

- **JDK 17** or later (e.g., [Eclipse Temurin](https://adoptium.net/))
- **Maven 3.8+** (or use the Maven Wrapper: `mvnw`)
- Internet connection on first run (to download YOLOv8n model, ~6 MB)

## Quick Start

```bash
# 1. Compile
mvn clean compile

# 2. Run tests (optional, requires network on first run)
mvn test

# 3. Package into a single fat JAR
mvn clean package

# 4. Launch
java -jar target/yolo-sight-1.0.0.jar
```

On first launch, clicking "Detect" will download the YOLOv8n model (~6 MB) into
`~/.djl.ai/cache/`. Subsequent launches use the cached model — no network needed.

## Project Structure

```
src/main/java/com/padna/yolosight/
├── App.java                  # Entry point
├── config/
│   └── AppConfig.java        # Constants & defaults
├── model/
│   ├── DetectionResult.java  # Detection data record
│   ├── ModelManager.java     # Model lifecycle (download/load/cache)
│   └── YoloDetector.java     # Inference pipeline
├── gui/
│   ├── MainFrame.java        # Main window (layout, wiring)
│   ├── ImageCanvas.java      # Image display + overlays + zoom/pan
│   ├── ControlPanel.java     # Sidebar: confidence slider, filter, detect
│   ├── ResultTablePanel.java # Results table with highlight-on-click
│   └── StatusBar.java        # Bottom status bar
└── util/
    ├── DrawingUtils.java     # Bounding box + label rendering
    ├── ImageUtils.java       # Image load/convert/save
    └── FileFilterUtils.java  # File extension filter
```

## Supported Classes (COCO dataset)

The YOLOv8n model detects 80 object classes including:
person, bicycle, car, motorcycle, airplane, bus, train, truck, boat,
traffic light, fire hydrant, stop sign, parking meter, bench, bird, cat,
dog, horse, sheep, cow, elephant, bear, zebra, giraffe, backpack, umbrella,
handbag, tie, suitcase, frisbee, skis, snowboard, sports ball, kite,
baseball bat, baseball glove, skateboard, surfboard, tennis racket, bottle,
wine glass, cup, fork, knife, spoon, bowl, banana, apple, sandwich, orange,
broccoli, carrot, hot dog, pizza, donut, cake, chair, couch, potted plant,
bed, dining table, toilet, TV, laptop, mouse, remote, keyboard, cell phone,
microwave, oven, toaster, sink, refrigerator, book, clock, vase, scissors,
teddy bear, hair drier, toothbrush.

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `Ctrl+O` | Open image |
| `Ctrl+D` | Run detection |
| `Ctrl+S` | Save annotated result |
| `Ctrl+0` | Fit image to window |
| `Ctrl+=` | Zoom in |
| `Ctrl+-` | Zoom out |
| `Ctrl+1` | Reset zoom (100%) |
| `Ctrl+Q` | Exit |

## Troubleshooting

**"Model not found" on first launch:**
Ensure you have internet access. The app downloads the YOLOv8n ONNX model (~6 MB)
from the DJL model repository on first run.

**ONNX Runtime fails to load on Windows:**
The fat JAR bundles ONNX Runtime native libraries. If you see native library errors,
try installing the [Visual C++ Redistributable](https://aka.ms/vs/17/release/vc_redist.x64.exe).

**Out of memory with large images:**
The app converts images for display but YOLOv8 always resizes input to 640×640,
so inference memory is bounded. If loading fails, try a smaller image.
