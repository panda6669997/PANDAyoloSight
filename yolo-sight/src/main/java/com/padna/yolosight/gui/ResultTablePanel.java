package com.padna.yolosight.gui;

import com.padna.yolosight.model.DetectionResult;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.function.Consumer;

/**
 * Results table using JavaFX TableView.
 */
public class ResultTablePanel {

    private final TableView<DetectionRow> tableView;
    private final ObservableList<DetectionRow> rows = FXCollections.observableArrayList();
    private Consumer<DetectionResult> onRowSelected;

    public ResultTablePanel() {
        tableView = new TableView<>();
        tableView.setItems(rows);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<DetectionRow, Number> numCol = new TableColumn<>("#");
        numCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().rowNum));
        numCol.setPrefWidth(40);
        numCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<DetectionRow, String> classCol = new TableColumn<>("类别");
        classCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().className));
        classCol.setPrefWidth(110);

        // Confidence column — color-coded
        TableColumn<DetectionRow, String> confCol = new TableColumn<>("置信度");
        confCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().confDisplay));
        confCol.setPrefWidth(70);
        confCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item);
                if (!empty && getTableRow() != null && getTableRow().getItem() != null) {
                    float conf = getTableRow().getItem().confidence;
                    if (conf >= 0.8f) setTextFill(Color.web("#4CAF50"));
                    else if (conf >= 0.5f) setTextFill(Color.web("#FF9800"));
                    else setTextFill(Color.web("#F44336"));
                }
                setStyle("-fx-alignment: CENTER-RIGHT;");
            }
        });

        TableColumn<DetectionRow, Number> xCol = numCol("X", d -> d.x);
        TableColumn<DetectionRow, Number> yCol = numCol("Y", d -> d.y);
        TableColumn<DetectionRow, Number> wCol = numCol("宽", d -> d.width);
        TableColumn<DetectionRow, Number> hCol = numCol("高", d -> d.height);

        tableView.getColumns().addAll(numCol, classCol, confCol, xCol, yCol, wCol, hCol);

        tableView.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null && onRowSelected != null) {
                onRowSelected.accept(sel.result);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private TableColumn<DetectionRow, Number> numCol(String title,
                                                      java.util.function.Function<DetectionRow, Integer> getter) {
        TableColumn<DetectionRow, Number> col = new TableColumn<>(title);
        col.setCellValueFactory(d -> new SimpleIntegerProperty(getter.apply(d.getValue())));
        col.setPrefWidth(55);
        col.setStyle("-fx-alignment: CENTER-RIGHT;");
        return col;
    }

    public Pane getView() {
        return new Pane(tableView) {{
            // Make table fill the container
            tableView.prefWidthProperty().bind(widthProperty());
            tableView.prefHeightProperty().bind(heightProperty());
            setStyle("-fx-background-color: #2a2a2e; -fx-background-radius: 6;");
        }};
    }

    public void setResults(List<DetectionResult> results) {
        rows.clear();
        int i = 1;
        for (DetectionResult r : results) {
            rows.add(new DetectionRow(i++, r));
        }
        if (!rows.isEmpty()) {
            tableView.getSelectionModel().selectFirst();
        }
    }

    public void setOnRowSelected(Consumer<DetectionResult> callback) {
        this.onRowSelected = callback;
    }

    // ── Row data class ──────────────────────────────────────────────

    public static class DetectionRow {
        final int rowNum;
        final String className;
        final float confidence;
        final String confDisplay;
        final int x, y, width, height;
        final DetectionResult result;

        DetectionRow(int rowNum, DetectionResult r) {
            this.rowNum = rowNum;
            this.className = r.className();
            this.confidence = r.confidence();
            this.confDisplay = String.format("%.1f%%", r.confidence() * 100);
            java.awt.Rectangle b = r.boundingBox();
            this.x = b.x;
            this.y = b.y;
            this.width = b.width;
            this.height = b.height;
            this.result = r;
        }
    }
}
