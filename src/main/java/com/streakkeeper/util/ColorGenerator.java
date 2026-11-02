// FILE: src/main/java/com/streakkeeper/util/ColorGenerator.java
// PACKAGE: com.streakkeeper.util
package com.streakkeeper.util;

import java.awt.Color;
import java.util.Random;

/**
 * Produces vivid, dopamine-y colors for the hold button.
 *
 * <p>Design: high saturation (0.75–1.0) and mid-high brightness (0.78–0.95)
 * in HSV — enough contrast that white text remains readable, but vivid enough
 * that each increment feels fresh.</p>
 */
public final class ColorGenerator {

    private static final Random RNG = new Random();

    private ColorGenerator() {}

    public static Color nextVividColor() {
        float hue = RNG.nextFloat();
        float sat = 0.75f + RNG.nextFloat() * 0.25f;   // 0.75 – 1.00
        float bri = 0.78f + RNG.nextFloat() * 0.17f;   // 0.78 – 0.95
        return Color.getHSBColor(hue, sat, bri);
    }

    /** Linear RGB interpolation between two colors. */
    public static Color lerp(Color a, Color b, double t) {
        double c = Math.max(0, Math.min(1, t));
        int r = (int) Math.round(a.getRed()   + (b.getRed()   - a.getRed())   * c);
        int g = (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * c);
        int bl = (int) Math.round(a.getBlue() + (b.getBlue()  - a.getBlue())  * c);
        return new Color(r, g, bl);
    }
}