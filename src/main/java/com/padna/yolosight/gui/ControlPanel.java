package com.padna.yolosight.gui;

import com.padna.yolosight.config.AppConfig;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Side panel with detection controls: confidence threshold slider,
 * class filter combo, Detect button, and a summary label.
 */
public class ControlPanel extends JPanel {

    private final JSlider confidenceSlider;
    private final JLabel confidenceValueLabel;
    private final JComboBox<String> classFilterCombo;
    private final JButton detectButton;
    private final JLabel summaryLabel;

    // Callback interfaces (set by MainFrame)
    private Runnable onDetectAction;
    private java.util.function.Consumer<Float> onConfidenceChanged;
    private java.util.function.Consumer<String> onClassFilterChanged;

    public ControlPanel() {
        setPreferredSize(new Dimension(AppConfig.CONTROL_PANEL_WIDTH, 0));
        setMinimumSize(new Dimension(200, 0));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(8, 8, 8, 8));

        // ── Detect button ────────────────────────────────────────────
        detectButton = new JButton("🔍 检测物体");
        detectButton.setFont(detectButton.getFont().deriveFont(Font.BOLD, 14f));
        detectButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        detectButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        detectButton.addActionListener(e -> {
            if (onDetectAction != null) onDetectAction.run();
        });
        add(detectButton);
        add(Box.createVerticalStrut(16));

        // ── Confidence threshold slider ──────────────────────────────
        JPanel confPanel = createSectionPanel("置信度阈值");
        confPanel.setLayout(new BoxLayout(confPanel, BoxLayout.Y_AXIS));

        confidenceValueLabel = new JLabel("50%");
        confidenceValueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        confidenceValueLabel.setFont(confidenceValueLabel.getFont().deriveFont(Font.BOLD, 16f));

        confidenceSlider = new JSlider(JSlider.HORIZONTAL, 10, 100, 50);
        confidenceSlider.setMajorTickSpacing(20);
        confidenceSlider.setMinorTickSpacing(5);
        confidenceSlider.setPaintTicks(true);
        confidenceSlider.setSnapToTicks(true);
        confidenceSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        confidenceSlider.addChangeListener(e -> {
            int val = confidenceSlider.getValue();
            confidenceValueLabel.setText(val + "%");
            if (onConfidenceChanged != null) {
                onConfidenceChanged.accept(val / 100f);
            }
        });

        confPanel.add(confidenceValueLabel);
        confPanel.add(Box.createVerticalStrut(4));
        confPanel.add(confidenceSlider);
        add(confPanel);
        add(Box.createVerticalStrut(12));

        // ── Class filter ─────────────────────────────────────────────
        JPanel filterPanel = createSectionPanel("类别筛选");
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.Y_AXIS));

        classFilterCombo = new JComboBox<>(new String[]{"全部类别"});
        classFilterCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        classFilterCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        classFilterCombo.addActionListener(e -> {
            if (onClassFilterChanged != null) {
                onClassFilterChanged.accept((String) classFilterCombo.getSelectedItem());
            }
        });
        filterPanel.add(classFilterCombo);
        add(filterPanel);
        add(Box.createVerticalStrut(12));

        // ── Summary ──────────────────────────────────────────────────
        JPanel summaryPanel = createSectionPanel("检测结果");
        summaryPanel.setLayout(new BorderLayout());
        summaryLabel = new JLabel("<html>暂无检测结果。</html>");
        summaryPanel.add(summaryLabel, BorderLayout.CENTER);
        add(summaryPanel);

        // Push everything to the top
        add(Box.createVerticalGlue());
    }

    // ── Section helper ──────────────────────────────────────────────────

    private JPanel createSectionPanel(String title) {
        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(title));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        return panel;
    }

    // ── Getters & setters ───────────────────────────────────────────────

    public float getConfidenceThreshold() {
        return confidenceSlider.getValue() / 100f;
    }

    public void setConfidenceThreshold(float threshold) {
        confidenceSlider.setValue(Math.round(threshold * 100));
    }

    /** Populate the class-filter dropdown with unique class names from results. */
    public void updateClassFilter(java.util.List<String> classNames) {
        String current = (String) classFilterCombo.getSelectedItem();
        classFilterCombo.removeAllItems();
        classFilterCombo.addItem("全部类别");
        for (String name : classNames) {
            classFilterCombo.addItem(name);
        }
        // Restore selection if possible
        if (current != null) {
            for (int i = 0; i < classFilterCombo.getItemCount(); i++) {
                if (current.equals(classFilterCombo.getItemAt(i))) {
                    classFilterCombo.setSelectedIndex(i);
                    return;
                }
            }
        }
    }

    public void setSummary(int totalDetected, long elapsedMs) {
        if (totalDetected == 0) {
            summaryLabel.setText("<html>未检测到物体<br/>"
                    + "<small>尝试降低置信度阈值</small></html>");
        } else {
            summaryLabel.setText(String.format(
                    "<html><b>%d</b> 个物体<br/>"
                            + "<small>耗时 %,d 毫秒</small></html>",
                    totalDetected,
                    elapsedMs));
        }
    }

    public void clearSummary() {
        summaryLabel.setText("<html>暂无检测结果。</html>");
    }

    // ── Callbacks ───────────────────────────────────────────────────────

    public void setOnDetectAction(Runnable action) {
        this.onDetectAction = action;
    }

    public void setOnConfidenceChanged(java.util.function.Consumer<Float> callback) {
        this.onConfidenceChanged = callback;
    }

    public void setOnClassFilterChanged(java.util.function.Consumer<String> callback) {
        this.onClassFilterChanged = callback;
    }

    // Expose detect button enable/disable for MainFrame
    public void setDetectEnabled(boolean enabled) {
        detectButton.setEnabled(enabled);
    }
}
