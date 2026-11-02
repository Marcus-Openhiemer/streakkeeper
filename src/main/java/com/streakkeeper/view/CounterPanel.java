// FILE: src/main/java/com/streakkeeper/view/CounterPanel.java
// PACKAGE: com.streakkeeper.view
package com.streakkeeper.view;

import com.streakkeeper.model.Counter;
import com.streakkeeper.model.StreakModel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * Main content area: counter name + value + stats, the hold button, the 30-day
 * heatmap, and the reset button.
 *
 * <p>Contains a self-managing 1 Hz timer so the "X days ago" text and the
 * "come back tomorrow" countdown stay fresh across a midnight rollover without
 * needing the model to fire on the hour.</p>
 */
public class CounterPanel extends JPanel {

    private final ScaledLabel nameLabel  = new ScaledLabel();
    private final ScaledLabel valueLabel = new ScaledLabel();
    private final JLabel statsLabel      = new JLabel(" ", SwingConstants.CENTER);
    private final HoldButton holdButton  = new HoldButton();
    private final JButton resetBtn       = new JButton("Reset");
    private final HeatmapPanel heatmap   = new HeatmapPanel();

    private Consumer<String> onRename;
    private Runnable onReset;

    /** Last-updated counter, kept so the 1 Hz timer can re-check state. */
    private Counter currentCounter;

    public CounterPanel() {
        setLayout(new BorderLayout(16, 16));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // -- Header: name + value + stats
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 26f));
        nameLabel.setText("No counter selected");
        nameLabel.setToolTipText("Double-click to rename");
        nameLabel.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && currentCounter != null) {
                    String newName = JOptionPane.showInputDialog(
                            CounterPanel.this, "New name:", currentCounter.getName());
                    if (newName != null && !newName.isBlank() && onRename != null) {
                        onRename.accept(newName.trim());
                    }
                }
            }
        });

        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 96f));
        valueLabel.setText("—");

        statsLabel.setFont(statsLabel.getFont().deriveFont(Font.PLAIN, 14f));

        JPanel valueWrap = new JPanel(new BorderLayout());
        valueWrap.setOpaque(false);
        valueWrap.setPreferredSize(new Dimension(0, 140));
        valueWrap.add(valueLabel, BorderLayout.CENTER);
        valueWrap.add(statsLabel, BorderLayout.SOUTH);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(nameLabel, BorderLayout.NORTH);
        header.add(valueWrap, BorderLayout.CENTER);

        // -- Center: hold button
        JPanel centerWrap = new JPanel(new GridBagLayout());
        centerWrap.setOpaque(false);
        centerWrap.add(holdButton);

        // -- South: heatmap + reset
        resetBtn.setToolTipText("Reset to start value  (Ctrl+R)");
        resetBtn.addActionListener(e -> { if (onReset != null) onReset.run(); });

        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(heatmap, BorderLayout.CENTER);
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnRow.setOpaque(false);
        btnRow.add(resetBtn);
        south.add(btnRow, BorderLayout.SOUTH);

        add(header, BorderLayout.NORTH);
        add(centerWrap, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        // 1 Hz refresh for "days ago" and the come-back countdown.
        new Timer(1000, e -> refreshLiveText()).start();
    }

    // ---------------------------------------------------------- wiring

    public HoldButton getHoldButton() { return holdButton; }
    public void setOnRename(Consumer<String> c) { this.onRename = c; }
    public void setOnReset(Runnable r)          { this.onReset = r; }

    // ------------------------------------------------------- model sync

    public void updateFromModel(StreakModel model) {
        currentCounter = model.getSelectedCounter();
        if (currentCounter == null) {
            nameLabel.setText("No counter selected");
            valueLabel.setText("—");
            statsLabel.setText("Create a counter to begin");
            holdButton.setEnabled(false);
            resetBtn.setEnabled(false);
            heatmap.setHistory(null);
            holdButton.setComeBackMode(false);
            return;
        }
        holdButton.setEnabled(true);
        resetBtn.setEnabled(true);
        nameLabel.setText(currentCounter.getName());
        valueLabel.setText(String.valueOf(currentCounter.getCurrentValue()));
        updateStatsText();
        heatmap.setHistory(currentCounter.getIncrementHistory());
        holdButton.setComeBackMode(!currentCounter.canIncrementToday());
    }

    private void updateStatsText() {
        if (currentCounter == null) return;
        statsLabel.setText("Last incremented: " + currentCounter.getDaysAgo()
                + "   ·   Streak: " + currentCounter.getCurrentStreak() + " days");
    }

    private void refreshLiveText() {
        if (currentCounter == null) return;
        updateStatsText();
        // In case we rolled past midnight while the app was open, flip the mode.
        holdButton.setComeBackMode(!currentCounter.canIncrementToday());
    }

    // ---------------------------------------------------- value bounce

    /** Plays a brief scale bounce on the big number. Called by the controller. */
    public void bounceValueLabel() {
        final long startNanos = System.nanoTime();
        final int DURATION_MS = 320;
        Timer t = new Timer(16, null);
        t.addActionListener(e -> {
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
            double p = Math.min(1.0, elapsedMs / (double) DURATION_MS);
            double scale = (p < 0.5)
                    ? 1.0 + 0.25 * (p * 2)
                    : 1.25 - 0.25 * ((p - 0.5) * 2);
            valueLabel.setScale(scale);
            if (p >= 1.0) {
                valueLabel.setScale(1.0);
                t.stop();
            }
        });
        t.start();
    }

    /** Simple toast celebrating a milestone. */
    public void playMilestone(int days) {
        if (!isShowing()) return;
        javax.swing.JWindow toast = new javax.swing.JWindow(
                SwingUtilities.getWindowAncestor(this));
        JLabel lbl = new JLabel("🎉  " + days + " day streak!  🎉", SwingConstants.CENTER);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 22f));
        lbl.setOpaque(true);
        lbl.setBackground(new Color(255, 235, 59));
        lbl.setForeground(Color.BLACK);
        lbl.setBorder(BorderFactory.createEmptyBorder(16, 32, 16, 32));
        toast.setContentPane(lbl);
        toast.pack();

        Point p = getLocationOnScreen();
        toast.setLocation(p.x + getWidth() / 2 - toast.getWidth() / 2,
                p.y + 60);
        toast.setVisible(true);

        Timer dismiss = new Timer(2200, e -> toast.dispose());
        dismiss.setRepeats(false);
        dismiss.start();
    }

    // ------------------------------------------------------ scaled label

    /**
     * JLabel whose text is painted at a manually controlled scale. That avoids
     * re-layout jitter during the number-bounce animation (which would happen
     * if we just animated the font size on a normal JLabel).
     */
    private static final class ScaledLabel extends JLabel {
        private double scale = 1.0;

        ScaledLabel() { super("—"); }

        void setScale(double s) { this.scale = s; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            String text = getText();
            Font base = getFont();
            Font scaledFont = base.deriveFont((float) (base.getSize2D() * scale));
            g2.setFont(scaledFont);
            g2.setColor(getForeground());

            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(text);
            int w = getWidth(), h = getHeight();
            int x = (w - tw) / 2;
            int y = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(text, x, y);
            g2.dispose();
        }
    }
}