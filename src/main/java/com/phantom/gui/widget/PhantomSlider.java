/*
 * PhantomSlider.java — Reusable slider widget for module settings.
 *
 * Wraps Minecraft's AbstractSliderButton to map a 0.0–1.0 internal position to
 * any real-world double range (e.g. 3.0–8.0 for reach, 0.0–1.0 for knockback %).
 * Fires a ValueConsumer callback on every change so the owning module can update
 * its value and save config immediately. Displays whole numbers without decimals.
 */
package com.phantom.gui.widget;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

/**
 * A reusable slider widget that maps a 0.0–1.0 internal value to a custom
 * double range ({@code min}–{@code max}), calling a consumer on every change.
 *
 * <p>Vanilla's {@link AbstractSliderButton} stores its position as a 0.0–1.0 fraction
 * internally. This wrapper handles the min/max remapping transparently so callers
 * always work in real-world units (e.g. 3.0–8.0 blocks for reach).</p>
 *
 * <p>Display: shows the label and real value separated by ": ". Integer values are
 * shown without decimal places (e.g. "Zoom Level (x): 4") while fractional values
 * show two decimal places (e.g. "Knockback %: 0.75").</p>
 */
public class PhantomSlider extends AbstractSliderButton {
    private final String label;
    private final double min;
    private final double max;
    private final ValueConsumer onValueChanged;
    /** The actual mapped value in [min, max] range, updated on every slider move. */
    private double realValue;

    /**
     * @param x              Left edge, in screen pixels.
     * @param y              Top edge, in screen pixels.
     * @param width          Widget width in pixels.
     * @param height         Widget height in pixels (typically 20).
     * @param label          Display label shown before the colon.
     * @param min            The real-world minimum value (maps to slider position 0.0).
     * @param max            The real-world maximum value (maps to slider position 1.0).
     * @param initialValue   Starting value in [min, max]; clamped automatically.
     * @param onValueChanged Called with the new real value whenever the slider moves.
     */
    public PhantomSlider(int x, int y, int width, int height, String label,
                         double min, double max, double initialValue,
                         ValueConsumer onValueChanged) {
        super(x, y, width, height, Component.empty(), 0.0);
        this.label = label;
        this.min = min;
        this.max = max;
        this.onValueChanged = onValueChanged;

        // Clamp the initial value and convert it to the 0–1 internal representation.
        double clamped = Math.max(min, Math.min(max, initialValue));
        this.value = (clamped - min) / (max - min);
        this.realValue = getRealValue();
        this.updateMessage(); // Set the display text immediately.
    }

    /**
     * Rebuilds the visible label. Called by the parent class whenever the slider position changes.
     * Shows integers without a decimal point for cleaner readability.
     */
    @Override
    protected void updateMessage() {
        if (realValue == Math.floor(realValue)) {
            // Whole number — omit ".00" suffix (e.g. "Zoom Level (x): 4" not "4.00").
            this.setMessage(Component.literal(label + ": " + (int) realValue));
        } else {
            // Fractional value — two decimal places is enough precision for sliders.
            this.setMessage(Component.literal(label + ": " + String.format("%.2f", realValue)));
        }
    }

    /**
     * Called by the parent whenever the internal {@code value} (0–1) changes.
     * Maps back to the real range and fires the consumer.
     */
    @Override
    protected void applyValue() {
        this.realValue = getRealValue();
        this.onValueChanged.accept(this.realValue);
    }

    /** Converts the 0–1 slider position to the real [min, max] value. */
    private double getRealValue() {
        return min + this.value * (max - min);
    }

    /** Consumer interface — avoids needing a full lambda type declaration at call sites. */
    public interface ValueConsumer {
        void accept(double value);
    }
}
