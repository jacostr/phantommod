package com.phantom.gui.framework;

import com.phantom.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * A modern glassy slider component.
 */
public class ModernSlider extends BaseComponent {
    private final String label;
    private final double min, max;
    private double value;
    private final Consumer<Double> onChange;
    private boolean dragging;

    public ModernSlider(int x, int y, int width, int height, String label, double min, double max, double initialValue, Consumer<Double> onChange) {
        super(x, y, width, height);
        this.label = label;
        this.min = min;
        this.max = max;
        this.value = (initialValue - min) / (max - min);
        this.onChange = onChange;
    }

    @Override
    protected void draw(GuiGraphics graphics, int mouseX, int mouseY, float dt) {
        if (dragging) {
            this.value = Math.max(0, Math.min(1, (mouseX - x) / (double) width));
            onChange.accept(getRealValue());
        }

        // Draw track
        RenderUtil.drawGlassPanel(graphics, x, y + height / 2 - 2, width, 4, 0x40000000, 0x30FFFFFF);

        // Draw progress
        int progressWidth = (int) (value * width);
        RenderUtil.drawHorizontalGradient(graphics, x, y + height / 2 - 2, progressWidth, 4, 0x80A8E6A3, 0xFFA8E6A3);

        // Draw thumb
        int thumbX = x + progressWidth - 4;
        RenderUtil.drawGlassPanel(graphics, thumbX, y + 2, 8, height - 4, 0xFFFFFFFF, 0xFFA8E6A3);
        
        if (hovered || dragging) {
            RenderUtil.drawGlow(graphics, thumbX, y + 2, 8, height - 4, 0x40A8E6A3);
        }

        // Text
        String displayValue = getRealValue() == Math.floor(getRealValue()) ? String.valueOf((int)getRealValue()) : String.format("%.2f", getRealValue());
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y - 9);
        graphics.pose().scale(0.85f, 0.85f);
        graphics.drawString(font, styledText(label + ": " + displayValue), 0, 0, 0xFFD0D0D0, false);
        graphics.pose().popMatrix();
    }

    private double getRealValue() {
        return min + value * (max - min);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            this.dragging = true;
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        this.dragging = false;
    }
}
