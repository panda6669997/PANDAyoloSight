package com.padna.yolosight.gui;

import com.padna.yolosight.model.DetectionResult;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Scrollable table listing all detection results.
 * Clicking a row highlights the corresponding bounding box on the canvas.
 */
public class ResultTablePanel extends JPanel {

    private final JTable table;
    private final DetectionTableModel tableModel;

    // Callback when user clicks a row
    private java.util.function.Consumer<DetectionResult> onRowSelected;

    public ResultTablePanel() {
        setLayout(new BorderLayout());
        setBorder(new TitledBorder("检测结果列表"));

        tableModel = new DetectionTableModel();
        table = new JTable(tableModel);
        configureTable();

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        scrollPane.setPreferredSize(new Dimension(0, 150));
        add(scrollPane, BorderLayout.CENTER);
    }

    private void configureTable() {
        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(30);   // #
        table.getColumnModel().getColumn(0).setMaxWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);  // Class
        table.getColumnModel().getColumn(2).setPreferredWidth(60);   // Conf
        table.getColumnModel().getColumn(2).setMaxWidth(70);
        table.getColumnModel().getColumn(3).setPreferredWidth(50);   // X
        table.getColumnModel().getColumn(4).setPreferredWidth(50);   // Y
        table.getColumnModel().getColumn(5).setPreferredWidth(50);   // W
        table.getColumnModel().getColumn(6).setPreferredWidth(50);   // H

        // Row height
        table.setRowHeight(24);

        // Header
        table.getTableHeader().setReorderingAllowed(false);

        // Selection — single row
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Right-align numeric columns
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int col : new int[]{0, 2, 3, 4, 5, 6}) {
            table.getColumnModel().getColumn(col).setCellRenderer(rightRenderer);
        }

        // Color-code confidence column
        table.getColumnModel().getColumn(2).setCellRenderer(new ConfidenceRenderer());

        // Row selection → callback
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && onRowSelected != null) {
                int row = table.getSelectedRow();
                if (row >= 0 && row < tableModel.getRowCount()) {
                    onRowSelected.accept(tableModel.getDetectionAt(row));
                }
            }
        });
    }

    /** Replace all rows with new results. */
    public void setResults(List<DetectionResult> results) {
        tableModel.setResults(results);
        if (!results.isEmpty()) {
            table.setRowSelectionInterval(0, 0);
        }
    }

    /** Clear all results. */
    public void clear() {
        tableModel.setResults(new ArrayList<>());
    }

    public void setOnRowSelected(java.util.function.Consumer<DetectionResult> callback) {
        this.onRowSelected = callback;
    }

    // ── Table model ────────────────────────────────────────────────────

    private static class DetectionTableModel extends AbstractTableModel {

        private static final String[] COLUMNS = {
                "序号", "类别", "置信度", "X", "Y", "宽度", "高度"
        };

        private List<DetectionResult> results = new ArrayList<>();

        public void setResults(List<DetectionResult> results) {
            this.results = new ArrayList<>(results);
            fireTableDataChanged();
        }

        public DetectionResult getDetectionAt(int row) {
            return results.get(row);
        }

        @Override
        public int getRowCount() {
            return results.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int col) {
            return COLUMNS[col];
        }

        @Override
        public Object getValueAt(int row, int col) {
            DetectionResult r = results.get(row);
            Rectangle bbox = r.boundingBox();
            return switch (col) {
                case 0 -> row + 1;
                case 1 -> r.className();
                case 2 -> String.format("%.1f%%", r.confidence() * 100);
                case 3 -> bbox.x;
                case 4 -> bbox.y;
                case 5 -> bbox.width;
                case 6 -> bbox.height;
                default -> "";
            };
        }
    }

    // ── Confidence color renderer ─────────────────────────────────────

    private static class ConfidenceRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int col) {

            Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, col);
            setHorizontalAlignment(SwingConstants.RIGHT);

            if (!isSelected) {
                DetectionTableModel model = (DetectionTableModel) table.getModel();
                float conf = model.getDetectionAt(row).confidence();
                if (conf >= 0.8f) {
                    c.setForeground(new Color(0x1B, 0x8C, 0x34)); // green
                } else if (conf >= 0.5f) {
                    c.setForeground(new Color(0xE6, 0x8A, 0x00)); // orange
                } else {
                    c.setForeground(new Color(0xCC, 0x33, 0x33)); // red
                }
            }
            return c;
        }
    }
}
