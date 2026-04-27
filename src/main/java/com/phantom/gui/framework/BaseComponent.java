package com.phantom.gui.framework;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

/**
 * Base class for all custom UI components.
 */
public abstract class BaseComponent {
    /** Clean non-pixelated font for the mod UI */
    protected static final FontDescription CLEAN_FONT = new FontDescription.Resource(Identifier.fromNamespaceAndPath("minecraft", "uniform"));

    protected int x, y, width, height;
    protected boolean visible = true;
    protected boolean hovered;
    protected long lastTime = -1;
    protected final Minecraft mc = Minecraft.getInstance();
    protected final Font font = mc.font;

    /** Creates a Component styled with the clean uniform font. */
    protected static Component styledText(String text) {
        return Component.literal(text).withStyle(s -> s.withFont(CLEAN_FONT));
    }

    /** Applies the clean font to an existing Component. */
    protected static Component styled(Component comp) {
        return comp.copy().withStyle(s -> s.withFont(CLEAN_FONT));
    }

    public BaseComponent(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        
        long now = System.currentTimeMillis();
        if (lastTime == -1) lastTime = now;
        float dt = (now - lastTime) / 1000f;
        lastTime = now;

        this.hovered = isMouseOver(mouseX, mouseY);
        draw(graphics, mouseX, mouseY, dt);
    }

    protected abstract void draw(GuiGraphics graphics, int mouseX, int mouseY, float dt);

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return visible && isMouseOver(mouseX, mouseY);
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {}

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) { return false; }

    public boolean charTyped(char codePoint, int modifiers) { return false; }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
}
