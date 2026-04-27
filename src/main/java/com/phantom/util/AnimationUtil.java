package com.phantom.util;

/**
 * Utility for smooth UI animations using easing functions.
 */
public class AnimationUtil {

    /**
     * Linearly interpolates between current and target value.
     * @param current Current value
     * @param target Target value
     * @param speed Speed of interpolation (0.0 to 1.0)
     * @return Interpolated value
     */
    public static float lerp(float current, float target, float speed) {
        return current + (target - current) * speed;
    }

    /**
     * Smoothly transitions a value using a simplified exponential approach.
     */
    public static float calculateDelta(float current, float target, double deltaTime, float speed) {
        float diff = target - current;
        if (Math.abs(diff) < 0.001) return target;
        // Exponential decay for that "liquid" feel
        return current + diff * (float) (1.0 - Math.exp(-speed * deltaTime));
    }
}
