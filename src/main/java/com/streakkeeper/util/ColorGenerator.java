// FILE: src/main/java/com/streakkeeper/util/ColorGenerator.java
// PACKAGE: com.streakkeeper.util
package com.streakkeeper.util;

import java.awt.Color;
import java.util.Random;

/**
 * Produces vivid, dopamine-y colors for the hold button.
 */
public final class ColorGenerator {

    private static final Random RNG = new Random();

    private ColorGenerator() {}

    public static Color nextVividColor() {
        float hue = RNG.nextFloat();
        // Push saturation and brightness much higher for a "neon" effect.
        float sat = 0.85f + RNG.nextFloat() * 0.15f;   // 0.85 – 1.00
        float bri = 0.85f + RNG.nextFloat() * 0.15f;   // 0.85 – 1.00
        return Color.getHSBColor(hue, sat, bri);
    }

    /**
     * Interpolates between two colors in HSB space.
     * This prevents the "muddy middle" effect of RGB lerping, keeping the
     * transitioning colors vivid and highly saturated at all times.
     */
    public static Color lerpHSB(Color a, Color b, double t) {
        float c = (float) Math.max(0, Math.min(1, t));
        float[] hsbA = Color.RGBtoHSB(a.getRed(), a.getGreen(), a.getBlue(), null);
        float[] hsbB = Color.RGBtoHSB(b.getRed(), b.getGreen(), b.getBlue(), null);

        // Hue interpolation (handle wrap-around)
        float hueDiff = hsbB[0] - hsbA[0];
        if (hueDiff > 0.5f) {
            hsbA[0] += 1.0f;
        } else if (hueDiff < -0.5f) {
            hsbB[0] += 1.0f;
        }

        float h = hsbA[0] + (hsbB[0] - hsbA[0]) * c;
        if (h >= 1.0f) h -= 1.0f;

        float s = hsbA[1] + (hsbB[1] - hsbA[1]) * c;
        float br = hsbA[2] + (hsbB[2] - hsbA[2]) * c;

        return Color.getHSBColor(h, s, br);
    }
}