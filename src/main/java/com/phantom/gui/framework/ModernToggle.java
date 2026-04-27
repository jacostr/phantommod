package com.phantom.gui.framework;

import com.phantom.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.Consumer;

/**
 * A modern switch-style toggle component.
 */
public class ModernToggle extends BaseComponent {
    private boolean enabled;
    private final Consumer<Boolean> onToggle;
    private final String label;
    private float animationPos = 0f;

    public ModernToggle(int x, int y, int width, int height, String label, boolean initialValue, Consumer<Boolean> onToggle) {
        super(x, y, width, height);
        this.label = label;
        this.enabled = initialValue;
        this.onToggle = onToggle;
        this.animationPos = enabled ? 1.0f : 0.0f;
    }

    @Override
    protected void draw(GuiGraphics graphics, int mouseX, int mouseY, float dt) {
        // Liquid animation for the switch slider
        float target = enabled ? 1.0f : 0.0f;
        animationPos = com.phantom.util.AnimationUtil.calculateDelta(animationPos, target, dt, 12.0f);

        int trackColor = RenderUtil.interpolateColor(0x40000000, 0x60A8E6A3, animationPos);
        int borderColor = RenderUtil.interpolateColor(0x30FFFFFF, 0xFFA8E6A3, animationPos);

        // Draw glassy track
        RenderUtil.drawGlassPanel(graphics, x + width - 40, y + 4, 40, height - 8, trackColor, borderColor);

        // Draw thumb (the sliding part)
        int thumbX = x + width - 40 + 2 + (int) (animationPos * (40 - 12));
        int thumbColor = 0xFFFFFFFF;
        graphics.fill(thumbX, y + 2, thumbX + 8, y + height - 2, thumbColor);
        
        if (animationPos > 0.1) {
            RenderUtil.drawGlow(graphics, thumbX, y + 2, 8, height - 4, (int)(animationPos * 0x60) << 24 | 0xA8E6A3);
        }

        // Draw label
        if (label != null && !label.isEmpty()) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(x, y + (height - 8) / 2 + 1);
            graphics.pose().scale(0.85f, 0.85f);
            graphics.drawString(font, styledText(label), 0, 0, 0xFFD0D0D0, false);
            graphics.pose().popMatrix();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            enabled = !enabled;
            onToggle.accept(enabled);
            return true;
        }
        return false;
    }
}
