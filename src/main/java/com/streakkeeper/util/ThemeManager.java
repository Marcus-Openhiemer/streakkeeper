// FILE: src/main/java/com/streakkeeper/util/ThemeManager.java
// PACKAGE: com.streakkeeper.util
package com.streakkeeper.util;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Window;

/**
 * Applies FlatLaf light/dark themes. Centralized here (rather than in the View)
 * because theme switching needs to touch {@link UIManager} and repaint every
 * window — a cross-cutting concern.
 */
public final class ThemeManager {

    private ThemeManager() {}

    /** Installs FlatLaf Light before any component exists. */
    public static void init() {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            // Fall back to whatever the platform default is; app still works.
            e.printStackTrace();
        }
    }

    /**
     * Switches L&amp;F at runtime. Iterating {@link Window#getWindows()} and
     * calling {@code updateComponentTreeUI} is the documented way to re-theme
     * live components without rebuilding them.
     */
    public static void applyTheme(boolean dark) {
        try {
            UIManager.setLookAndFeel(dark ? new FlatDarkLaf() : new FlatLightLaf());
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        for (Window w : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(w);
            w.invalidate();
            w.validate();
            w.repaint();
        }
        // Explicitly repaint frames so any custom-painted components (HoldButton,
        // HeatmapPanel, etc.) refresh their colors immediately.
        for (Window w : Window.getWindows()) {
            if (w instanceof JFrame f) f.getContentPane().repaint();
        }
    }
}