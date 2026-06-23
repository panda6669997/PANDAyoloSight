package com.padna.yolosight;

import com.formdev.flatlaf.FlatLightLaf;
import com.padna.yolosight.gui.MainFrame;

import javax.swing.*;

/**
 * Application entry point.
 * Sets the FlatLaf look-and-feel and hands off to the EDT.
 */
public final class App {

    public static void main(String[] args) {
        // Install modern FlatLaf theme before any Swing component is created
        FlatLightLaf.setup();

        // All Swing work must happen on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
