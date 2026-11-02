// FILE: src/main/java/com/streakkeeper/view/HoldButton.java
// PACKAGE: com.streakkeeper.view
package com.streakkeeper.view;

import com.streakkeeper.util.AnimationUtils;
import com.streakkeeper.util.ColorGenerator;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.util.ArrayList;
import java.util.List;

/**
 * A custom, reusable {@link JComponent} that implements press-and-hold
 * interaction with a circular progress arc.
 *
 * <p><b>Responsibilities (view-only):</b></p>
 * <ul>
 *   <li>Track mouse/key press state and drive a 5 s progress animation.</li>
 *   <li>Fire {@link HoldListener#onHoldComplete()} when 5 s elapses.</li>
 *   <li>Fade the arc out smoothly if released early (eased reset).</li>
 *   <li>Play a "pulse" glow and shift to a new random vivid color on every
 *       completion. Crucially, this happens <i>even when the controller refuses
 *       the increment</i> (the "satisfying toy" state). The number in the
 *       counter panel is unaffected — the controller decides that.</li>
 * </ul>
 *
 * <p>All timing uses {@link javax.swing.Timer} (fires on the EDT) — no
 * {@code Thread.sleep} anywhere.</p>
 */
public class HoldButton extends JComponent {

    private static final int HOLD_DURATION_MS  = 5000;
    private static final int RESET_DURATION_MS = 400;
    private static final int PULSE_DURATION_MS = 700;
    private static final int COLOR_DURATION_MS = 500;
    private static final int TICK_MS           = 16;   // ~60 fps

    private final List<HoldListener> listeners = new ArrayList<>();

    // Hold state
    private boolean holding = false;
    private double  progress = 0.0;   // 0..1
    private long    holdStartNanos;

    // Pulse / color animation state
    private float   pulse = 0f;       // 0..1
    private long    pulseStartNanos;
    private long    resetStartNanos;
    private double  resetStartProgress;
    private long    colorStartNanos;

    // Colors
    private Color currentColor;
    private Color prevColor;
    private Color targetColor;

    // "Come back tomorrow" mode
    private boolean comeBackMode = false;
    private Timer   comeBackRefreshTimer;

    // Timers
    private final Timer holdTimer;
    private final Timer resetTimer;
    private final Timer pulseTimer;
    private final Timer colorTimer;

    public HoldButton() {
        setFocusable(true);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(300, 300));

        currentColor = ColorGenerator.nextVividColor();
        targetColor  = currentColor;
        prevColor    = currentColor;

        holdTimer  = new Timer(TICK_MS, e -> updateHold());
        resetTimer = new Timer(TICK_MS, e -> updateReset());
        pulseTimer = new Timer(TICK_MS, e -> updatePulse());
        colorTimer = new Timer(TICK_MS, e -> updateColor());

        // Mouse bindings
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                startHold();
            }
            @Override public void mouseReleased(MouseEvent e) {
                stopHold();
            }
        };
        addMouseListener(ma);

        // Space key press/release bindings — only while this component has focus.
        getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false), "startHold");
        getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, true), "stopHold");
        getActionMap().put("startHold", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (!holding) startHold();
            }
        });
        getActionMap().put("stopHold", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (holding) stopHold();
            }
        });
    }

    // --------------------------------------------------------- public API

    public void addListener(HoldListener l) {
        if (l != null) listeners.add(l);
    }

    /**
     * Enables the "come back tomorrow" countdown text. The button is still
     * fully interactive — the arc will animate on hold — only the idle label
     * changes.
     */
    public void setComeBackMode(boolean enabled) {
        if (this.comeBackMode == enabled) return;
        this.comeBackMode = enabled;
        if (comeBackRefreshTimer == null) {
            // 1Hz repaint so the countdown ticks visually.
            comeBackRefreshTimer = new Timer(1000, e -> repaint());
        }
        if (enabled) comeBackRefreshTimer.start();
        else         comeBackRefreshTimer.stop();
        repaint();
    }

    // --------------------------------------------------------- hold cycle

    private void startHold() {
        if (holding) return;
        // Cancel any in-flight reset so the arc doesn't fight us.
        resetTimer.stop();

        holding = true;
        progress = 0.0;
        holdStartNanos = System.nanoTime();
        holdTimer.start();

        for (HoldListener l : new ArrayList<>(listeners)) l.onHoldStarted();
        repaint();
    }

    private void stopHold() {
        if (!holding) return;
        holding = false;
        holdTimer.stop();

        // Cancel early → animate progress back to zero.
        resetStartProgress = progress;
        resetStartNanos = System.nanoTime();
        resetTimer.start();

        for (HoldListener l : new ArrayList<>(listeners)) l.onHoldCancelled(progress);
    }

    private void updateHold() {
        long elapsedMs = (System.nanoTime() - holdStartNanos) / 1_000_000;
        progress = Math.min(1.0, elapsedMs / (double) HOLD_DURATION_MS);
        repaint();
        if (progress >= 1.0) {
            holdTimer.stop();
            completeHold();
        }
    }

    /**
     * Called when the user completes a 5 s hold. View-local animations fire
     * regardless of whether the controller accepts the increment — this is the
     * "toy state" behavior.
     */
    private void completeHold() {
        holding = false;

        // Pulse glow
        pulse = 1f;
        pulseStartNanos = System.nanoTime();
        pulseTimer.start();

        // Fresh random color
        prevColor   = currentColor;
        targetColor = ColorGenerator.nextVividColor();
        colorStartNanos = System.nanoTime();
        colorTimer.start();

        // Fade the arc back to 0
        resetStartProgress = 1.0;
        resetStartNanos = System.nanoTime();
        resetTimer.start();

        // Notify controller
        for (HoldListener l : new ArrayList<>(listeners)) l.onHoldComplete();
    }

    // ---------------------------------------------------------- animations

    private void updateReset() {
        long elapsedMs = (System.nanoTime() - resetStartNanos) / 1_000_000;
        double t = Math.min(1.0, elapsedMs / (double) RESET_DURATION_MS);
        double eased = AnimationUtils.easeOutCubic(t);
        progress = resetStartProgress * (1 - eased);
        if (t >= 1.0) {
            progress = 0.0;
            resetTimer.stop();
        }
        repaint();
    }

    private void updatePulse() {
        long elapsedMs = (System.nanoTime() - pulseStartNanos) / 1_000_000;
        double t = Math.min(1.0, elapsedMs / (double) PULSE_DURATION_MS);
        pulse = (float) (1.0 - AnimationUtils.easeOutCubic(t));
        if (t >= 1.0) {
            pulse = 0f;
            pulseTimer.stop();
        }
        repaint();
    }

    private void updateColor() {
        long elapsedMs = (System.nanoTime() - colorStartNanos) / 1_000_000;
        double t = Math.min(1.0, elapsedMs / (double) COLOR_DURATION_MS);
        double eased = AnimationUtils.easeInOutCubic(t);
        currentColor = ColorGenerator.lerp(prevColor, targetColor, eased);
        if (t >= 1.0) {
            currentColor = targetColor;
            colorTimer.stop();
        }
        repaint();
    }

    // -------------------------------------------------------------- paint

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int w = getWidth(), h = getHeight();
        int diameter = (int) (Math.min(w, h) * 0.82);
        int cx = w / 2, cy = h / 2;
        int x = cx - diameter / 2;
        int y = cy - diameter / 2;

        // --- Pulse glow (radial gradient fading outward)
        if (pulse > 0.01f) {
            int maxGlow = Math.min(w, h);
            int glowSize = (int) (diameter + (maxGlow - diameter) * pulse);
            Color center = new Color(currentColor.getRed(), currentColor.getGreen(),
                    currentColor.getBlue(), (int) (170 * pulse));
            Color edge = new Color(currentColor.getRed(), currentColor.getGreen(),
                    currentColor.getBlue(), 0);
            g2.setPaint(new RadialGradientPaint(
                    new Point(cx, cy), glowSize / 2f,
                    new float[]{0f, 1f}, new Color[]{center, edge},
                    MultipleGradientPaint.CycleMethod.NO_CYCLE));
            g2.fillOval(cx - glowSize / 2, cy - glowSize / 2, glowSize, glowSize);
        }

        // --- Main circle
        g2.setColor(currentColor);
        g2.fillOval(x, y, diameter, diameter);

        // --- Subtle inner border
        g2.setColor(currentColor.darker());
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawOval(x, y, diameter, diameter);

        // --- Progress arc
        if (progress > 0.001) {
            int arcSize = diameter - 20;
            int arcAngle = (int) Math.round(-360 * progress);
            g2.setStroke(new BasicStroke(11f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(255, 255, 255, 220));
            g2.drawArc(x + 10, y + 10, arcSize, arcSize, 90, arcAngle);
        }

        // --- Focus ring
        if (isFocusOwner()) {
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1f, new float[]{4f, 4f}, 0f));
            g2.setColor(new Color(255, 255, 255, 160));
            g2.drawOval(x - 4, y - 4, diameter + 8, diameter + 8);
        }

        // --- Text
        g2.setColor(Color.WHITE);
        Font baseFont = getFont() != null ? getFont() : new Font("SansSerif", Font.BOLD, 16);
        Font textFont = baseFont.deriveFont(Font.BOLD, Math.max(15f, diameter / 12f));
        g2.setFont(textFont);

        String text = computeText();
        String[] lines = text.split("\n");
        FontMetrics fm = g2.getFontMetrics();
        int lineHeight = fm.getHeight();
        int totalHeight = lines.length * lineHeight;
        int startY = cy - totalHeight / 2 + fm.getAscent();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int lineWidth = fm.stringWidth(line);
            g2.drawString(line, cx - lineWidth / 2, startY + i * lineHeight);
        }

        g2.dispose();
    }

    private String computeText() {
        if (holding || progress > 0.01) {
            return "HOLDING " + (int) Math.round(progress * 100) + "%";
        }
        if (comeBackMode) {
            return "Come back tomorrow\n" + computeCountdownToMidnight();
        }
        return "HOLD 5s";
    }

    private String computeCountdownToMidnight() {
        long nowSeconds = System.currentTimeMillis() / 1000L;
        // Seconds remaining until next midnight in the current default zone.
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
        long secs = java.time.Duration.between(now, midnight).getSeconds();
        long hh = secs / 3600;
        long mm = (secs % 3600) / 60;
        long ss = secs % 60;
        return String.format("%02d:%02d:%02d", hh, mm, ss);
    }
}