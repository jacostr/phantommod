package com.phantom.gui.framework;

import com.phantom.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/**
 * A modern custom text field for the search bar.
 */
public class ModernTextField extends BaseComponent {
    private String text = "";
    private final String placeholder;
    private final Consumer<String> onChanged;
    private boolean focused;
    private int cursorCounter;

    public ModernTextField(int x, int y, int width, int height, String placeholder, Consumer<String> onChanged) {
        super(x, y, width, height);
        this.placeholder = placeholder;
        this.onChanged = onChanged;
    }

    @Override
    protected void draw(GuiGraphics graphics, int mouseX, int mouseY, float dt) {
        cursorCounter++;
        
        int bgColor = focused ? 0x80151515 : 0x60101010;
        int borderColor = focused ? 0xFFA8E6A3 : 0x40FFFFFF;
        
        RenderUtil.drawGlassPanel(graphics, x, y, width, height, bgColor, borderColor);

        String displayText = text;
        int textColor = 0xFFFFFFFF;
        
        if (text.isEmpty() && !focused) {
            displayText = placeholder;
            textColor = 0xFF808080;
        }

        graphics.drawString(font, styledText(displayText), x + 6, y + (height - 8) / 2, textColor, false);

        if (focused && (cursorCounter / 20) % 2 == 0) {
            int cursorX = x + 6 + font.width(text);
            graphics.fill(cursorX, y + 4, cursorX + 1, y + height - 4, 0xFFA8E6A3);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.focused = isMouseOver(mouseX, mouseY);
        return focused;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused) return false;

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!text.isEmpty()) {
                text = text.substring(0, text.length() - 1);
                onChanged.accept(text);
            }
            return true;
        } else if (((modifiers & GLFW.GLFW_MOD_CONTROL) != 0) && keyCode == GLFW.GLFW_KEY_A) {
             // Handle select all if needed
             return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!focused) return false;
        
        if (font.width(text + codePoint) < width - 12) {
            text += codePoint;
            onChanged.accept(text);
            return true;
        }
        return false;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }
}
