# PANDAyoloSight

批量图片物体识别与自动分类桌面应用，基于 **YOLOv8** 深度学习模型。

## 功能

- 选择一个图片文件夹，自动对全部图片运行目标检测
- 按**最高置信度**策略分类：每张图检测到的最可信物体决定其归属
- 分类后图片按类别存入对应子文件夹，输出干净原图
- **缩略图网格**预览，标注边界框和类别名直接在缩略图上渲染
- 点击缩略图弹出**大图窗口**，支持鼠标滚轮缩放和拖拽平移
- 暗色主题界面，全中文

## 技术栈

| 组件 | 用途 |
|------|------|
| Java 17 | 运行时 |
| JavaFX 21 | GUI（Canvas 渲染 + CSS 暗色主题） |
| DJL 0.36.0 | 深度学习框架 |
| ONNX Runtime 1.20.0 | 推理引擎（独立 DLL 部署） |
| YOLOv8n | 预训练目标检测模型，COCO 80 类 |

## 快速开始

**前置条件**：JDK 17+、Maven 3.8+、Windows 10/11 64 位

```bash
cd yolo-sight
mvn clean compile      # 编译
mvn javafx:run         # 启动
```

或双击 `run.bat` 一键启动。桌面快捷方式见 `PANDAyoloSight.bat`，启动更快（约 2 秒）。

首次使用点击「批量分类」会自动下载 YOLOv8n（约 6MB），之后无需网络。

## 使用流程

1. 点击 **「批量分类」**
2. 选择**源文件夹**（含待分类图片）
3. 选择**输出目录**
4. 确认 → 自动检测并分类
5. 右侧缩略图预览，**点击任意缩略图放大查看**

## 输出结构

```
输出目录/
├── person/
│   ├── photo001.jpg      ← 干净原图，无标注
│   └── photo005.jpg
├── car/
│   └── photo003.jpg
└── 未分类/
    └── photo004.jpg       ← 未检测到物体的
```

文件夹中为原始图片。应用内缩略图和大图自动叠加边界框和类别标签。

## 项目结构

```
yolo-sight/src/main/
├── java/com/padna/yolosight/
│   ├── App.java                      # JavaFX 入口
│   ├── config/AppConfig.java         # 配置常量
│   ├── model/
│   │   ├── DetectionResult.java      # 检测结果 record
│   │   ├── ClassificationResult.java # 分类结果 record
│   │   ├── ModelManager.java         # 模型生命周期
│   │   ├── YoloDetector.java         # 推理管道
│   │   └── BatchClassifier.java      # 批量分类引擎
│   ├── gui/
│   │   ├── MainWindow.java           # 主窗口
│   │   ├── ThumbnailGridPanel.java   # 缩略图网格 + 标注渲染
│   │   ├── ImageCanvasView.java      # 大图查看（缩放平移）
│   │   ├── BatchResultPanel.java     # 结果表格 + 统计
│   │   └── StatusBar.java            # 状态栏
│   └── util/
│       ├── DrawingUtils.java         # 边界框绘制（保存用）
│       ├── ImageUtils.java           # 图片加载/格式转换
│       └── FileFilterUtils.java      # 扩展名过滤
└── resources/css/dark-theme.css      # 暗色主题
```

## 可识别类别（COCO 80 类）

person, bicycle, car, motorcycle, airplane, bus, train, truck, boat,
traffic light, stop sign, bird, cat, dog, horse, sheep, cow, elephant,
bear, zebra, giraffe, backpack, umbrella, handbag, suitcase, sports ball,
skateboard, tennis racket, bottle, wine glass, cup, fork, knife, spoon,
bowl, banana, apple, sandwich, orange, broccoli, carrot, hot dog, pizza,
donut, cake, chair, couch, potted plant, bed, dining table, toilet, TV,
laptop, mouse, remote, keyboard, cell phone, microwave, oven, sink,
refrigerator, book, clock, vase, scissors, teddy bear, toothbrush 等。

## 文档

| 文档 | 说明 |
|------|------|
| [需求文档](需求文档.md) | 功能需求 + 非功能需求 |
| [设计文档](设计文档.md) | 架构设计 + 核心实现 |
| [CLAUDE.md](yolo-sight/CLAUDE.md) | 开发者指南 |

## 故障排除

| 问题 | 解决 |
|------|------|
| ONNX DLL 初始化失败 | 已内置于启动脚本，自动指向 `~/.yolosight/native/` |
| 模型下载失败 | 检查网络，模型约 6MB，从 DJL 仓库下载 |
| 启动闪退 | 确认 JDK 17 路径正确，运行 `java -version` 验证 |
