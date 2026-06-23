package com.padna.yolosight.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Bottom status bar showing current status, image dimensions, and model info.
 */
public class StatusBar extends JPanel {

    private final JLabel statusLabel;
    private final JLabel imageInfoLabel;
    private final JLabel modelLabel;

    public StatusBar() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                new EmptyBorder(2, 8, 2, 8)));

        statusLabel = new JLabel("就绪");
        imageInfoLabel = new JLabel();
        modelLabel = new JLabel("模型: yolov8n");

        add(statusLabel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        rightPanel.add(imageInfoLabel);
        rightPanel.add(modelLabel);
        add(rightPanel, BorderLayout.EAST);
    }

    /** Set the main status text (left side). */
    public void setStatus(String text) {
        statusLabel.setText(text);
    }

    /** Set image info text (right side), e.g. "1920×1080 | 2.3 MB". */
    public void setImageInfo(String text) {
        imageInfoLabel.setText(text);
    }

    /** Clear image info. */
    public void clearImageInfo() {
        imageInfoLabel.setText("");
    }
}
