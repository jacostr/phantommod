package com.phantom.util;

import net.minecraft.client.gui.GuiGraphics;

import java.awt.*;

/**
 * Advanced rendering utilities for custom UI elements.
 * Provides methods for drawing gradients, rounded-style rectangles, and glow effects.
 */
public class RenderUtil {

    /**
     * Draws a premium glass-style panel with subtle rounded corners.
     */
    public static void drawGlassPanel(GuiGraphics graphics, int x, int y, int width, int height, int color, int borderColor) {
        // Clean flat fill
        graphics.fill(x, y, x + width, y + height, color);
        
        // Subtle top highlight
        graphics.fill(x + 1, y, x + width - 1, y + 1, 0x15FFFFFF);
        
        // Thin 1px outline
        drawOutline(graphics, x, y, width, height, borderColor);
    }

    /**
     * Draws a 1-pixel outline around a rectangular area.
     */
    public static void drawOutline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color); // Top
        graphics.fill(x, y + height - 1, x + width, y + height, color); // Bottom
        graphics.fill(x, y, x + 1, y + height, color); // Left
        graphics.fill(x + width - 1, y, x + width, y + height, color); // Right
    }

    /**
     * Draws a horizontal gradient rectangle.
     */
    public static void drawHorizontalGradient(GuiGraphics graphics, int x, int y, int width, int height, int startColor, int endColor) {
        graphics.fillGradient(x, y, x + width, y + height, startColor, endColor);
    }

    /**
     * Interpolates between two colors based on a factor (0.0 to 1.0).
     */
    public static int interpolateColor(int color1, int color2, float factor) {
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int) (a1 + (a2 - a1) * factor);
        int r = (int) (r1 + (r2 - r1) * factor);
        int g = (int) (g1 + (g2 - g1) * factor);
        int b = (int) (b1 + (b2 - b1) * factor);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    
    /**
     * Draws a selection glow/highlight with a "liquid" feel.
     */
    public static void drawGlow(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        // Draw multiple layers of fading rectangles to simulate a soft glow
        for (int i = 0; i < 4; i++) {
            int alpha = (color >> 24) & 0xFF;
            int layerAlpha = (int) (alpha * (0.25f / (i + 1)));
            int layerColor = (layerAlpha << 24) | (color & 0x00FFFFFF);
            graphics.fill(x - i, y - i, x + width + i, y + height + i, layerColor);
        }
    }

    public static void drawCircle(GuiGraphics graphics, float centerX, float centerY, float radius, int color) {
        int segments = (int) (radius * 6);
        segments = Math.max(64, Math.min(segments, 1024));
        
        for (int i = 0; i < segments; i++) {
            double angle = (i * 2.0 * Math.PI / segments);
            int x = (int) (centerX + Math.cos(angle) * radius);
            int y = (int) (centerY + Math.sin(angle) * radius);
            // Draw a 1.5x1.5 area for better visibility
            graphics.fill(x, y, x + 1, y + 1, color);
            if (i % 2 == 0) graphics.fill(x + 1, y, x + 2, y + 1, color); // slight thickening
        }
    }
}
