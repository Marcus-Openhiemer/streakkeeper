// FILE: src/main/java/com/streakkeeper/view/CelebrationOverlay.java
// PACKAGE: com.streakkeeper.view
package com.streakkeeper.view;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A transparent overlay that draws a high-performance particle and ring burst.
 * Replaces heavy full-screen gradients to guarantee smooth 60 FPS animation.
 */
public class CelebrationOverlay extends JComponent {

    private final List<Particle> particles = new ArrayList<>();
    private final List<Ring> rings = new ArrayList<>();
    private final Random random = new Random();
    private Timer timer;
    private boolean animating = false;

    public CelebrationOverlay() {
        setOpaque(false);
        setVisible(false);
        timer = new Timer(16, e -> updateAnimation());
    }

    public void playCelebration(Point2D center, Color baseColor) {
        particles.clear();
        rings.clear();

        // 1. Create expanding energy rings
        for (int i = 0; i < 3; i++) {
            rings.add(new Ring(center, baseColor, i * 5)); // Stagger start radius
        }

        // 2. Create radiating particle burst (confetti/sparks)
        int particleCount = 90;
        for (int i = 0; i < particleCount; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 8 + random.nextDouble() * 18;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed;

            // Mix the button color with white and gold for extra pop
            Color pColor = baseColor;
            int colorType = random.nextInt(3);
            if (colorType == 0) pColor = Color.WHITE;
            else if (colorType == 1) pColor = new Color(255, 215, 0); // Gold

            particles.add(new Particle(center.getX(), center.getY(), vx, vy, pColor));
        }

        this.animating = true;
        this.setVisible(true);
        timer.start();
    }

    private void updateAnimation() {
        boolean hasActiveItems = false;

        // Update rings
        for (Ring ring : rings) {
            ring.radius += 35; // Expand speed
            ring.alpha -= 0.04f; // Fade out speed
            if (ring.alpha > 0) hasActiveItems = true;
        }
        rings.removeIf(r -> r.alpha <= 0);

        // Update particles
        for (Particle p : particles) {
            p.x += p.vx;
            p.y += p.vy;

            // Friction (slow down over time)
            p.vx *= 0.94;
            p.vy *= 0.94;

            // Slight gravity (pull down)
            p.vy += 0.6;

            p.alpha -= 0.02f;
            if (p.alpha > 0) hasActiveItems = true;
        }
        particles.removeIf(p -> p.alpha <= 0);

        if (!hasActiveItems) {
            timer.stop();
            setVisible(false);
            animating = false;
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (!animating) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw Rings
        for (Ring r : rings) {
            int alphaInt = (int) (Math.max(0, r.alpha) * 255);
            g2.setColor(new Color(r.color.getRed(), r.color.getGreen(), r.color.getBlue(), alphaInt));

            // FIXED: Explicitly cast the double result to a float
            float thickness = (float) (15 + (r.radius * 0.05));

            g2.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int rInt = (int) r.radius;
            g2.drawOval((int) r.center.getX() - rInt, (int) r.center.getY() - rInt, rInt * 2, rInt * 2);
        }

        // Draw Particles
        for (Particle p : particles) {
            int alphaInt = (int) (Math.max(0, p.alpha) * 255);
            g2.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), alphaInt));
            g2.fillOval((int) p.x, (int) p.y, p.size, p.size);
        }

        g2.dispose();
    }

    private static class Ring {
        Point2D center;
        Color color;
        double radius;
        float alpha = 1.0f;

        Ring(Point2D center, Color color, double initialRadius) {
            this.center = center;
            this.color = color;
            this.radius = initialRadius;
        }
    }

    private static class Particle {
        double x, y, vx, vy;
        Color color;
        float alpha = 1.0f;
        int size;

        Particle(double x, double y, double vx, double vy, Color color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
            this.size = 4 + (int)(Math.random() * 6); // Random size 4-10px
        }
    }
}