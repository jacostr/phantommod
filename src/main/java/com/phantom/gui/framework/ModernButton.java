package com.phantom.gui.framework;

import com.phantom.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * A modern, custom-rendered button with hover effects.
 */
public class ModernButton extends BaseComponent {
    private final Component label;
    private final Consumer<ModernButton> onClick;
    private float hoverFade = 0f;

    public ModernButton(int x, int y, int width, int height, Component label, Consumer<ModernButton> onClick) {
        super(x, y, width, height);
        this.label = label;
        this.onClick = onClick;
    }

    @Override
    protected void draw(GuiGraphics graphics, int mouseX, int mouseY, float dt) {
        // Liquid hover transition
        float target = hovered ? 1.0f : 0.0f;
        hoverFade = com.phantom.util.AnimationUtil.calculateDelta(hoverFade, target, dt, 10.0f);

        int bgColor = RenderUtil.interpolateColor(0x60101010, 0x90202020, hoverFade);
        int borderColor = RenderUtil.interpolateColor(0x40FFFFFF, 0xFFA8E6A3, hoverFade);

        RenderUtil.drawGlassPanel(graphics, x, y, width, height, bgColor, borderColor);
        
        int textColor = RenderUtil.interpolateColor(0xFFD0D0D0, 0xFFA8E6A3, hoverFade);
        graphics.pose().pushMatrix();
        graphics.pose().translate(x + width / 2, y + (height - 8) / 2 + 1);
        graphics.pose().scale(0.9f, 0.9f);
        graphics.drawCenteredString(font, styled(label), 0, 0, textColor);
        graphics.pose().popMatrix();
        
        if (hoverFade > 0.1f) {
            RenderUtil.drawGlow(graphics, x, y, width, height, (int)(hoverFade * 0x30) << 24 | 0xA8E6A3);
        }
    }

    public Component getMessage() { return label; }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            onClick.accept(this);
            return true;
        }
        return false;
    }
}
