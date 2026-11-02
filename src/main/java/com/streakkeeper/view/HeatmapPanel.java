// FILE: src/main/java/com/streakkeeper/view/HeatmapPanel.java
// PACKAGE: com.streakkeeper.view
package com.streakkeeper.view;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Small 30-day heatmap showing which days were incremented.
 * Purely a view: consumes {@link LocalDate}s from the model.
 */
public class HeatmapPanel extends JPanel {

    private List<LocalDate> history = new ArrayList<>();

    public HeatmapPanel() {
        setOpaque(false);
        setPreferredSize(new Dimension(0, 56));
    }

    public void setHistory(List<LocalDate> history) {
        this.history = history == null ? new ArrayList<>() : new ArrayList<>(history);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        final int cells = 30;
        final int cell = 14, gap = 4;
        final int totalW = cells * (cell + gap) - gap;
        final int startX = (getWidth() - totalW) / 2;
        final int startY = (getHeight() - cell) / 2;

        Set<LocalDate> set = new HashSet<>(history);
        LocalDate today = LocalDate.now();

        Color activeColor   = new Color(76, 175, 80);
        Color inactiveColor = new Color(160, 160, 160, 90);

        for (int i = 0; i < cells; i++) {
            LocalDate day = today.minusDays(cells - 1 - i);
            g2.setColor(set.contains(day) ? activeColor : inactiveColor);
            int x = startX + i * (cell + gap);
            g2.fillRoundRect(x, startY, cell, cell, 4, 4);
        }
        g2.dispose();
    }
}