// FILE: src/main/java/com/streakkeeper/util/AnimationUtils.java
// PACKAGE: com.streakkeeper.util
package com.streakkeeper.util;

/**
 * Small collection of easing functions. Using non-linear easing (instead of
 * linear tweening) makes the animations read as smooth instead of robotic.
 *
 * <p>All easing inputs are normalized to [0,1]. Timers are expected to compute
 * {@code t = elapsed / duration}, ease it, then interpolate the animated value.</p>
 */
public final class AnimationUtils {

    private AnimationUtils() {}

    /** Fast start, slow end. Good for "settle" animations. */
    public static double easeOutCubic(double t) {
        double c = clamp01(t);
        return 1 - Math.pow(1 - c, 3);
    }

    /** Slow start, fast middle, slow end. Good for slides. */
    public static double easeInOutCubic(double t) {
        double c = clamp01(t);
        return c < 0.5
                ? 4 * c * c * c
                : 1 - Math.pow(-2 * c + 2, 3) / 2;
    }

    /** Linear fallback. */
    public static double linear(double t) { return clamp01(t); }

    private static double clamp01(double v) {
        if (v < 0) return 0;
        if (v > 1) return 1;
        return v;
    }
}