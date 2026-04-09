/*
 * ReachDisplay.java — HUD widget showing the last hit distance.
 *
 * Detectability: Safe — purely visual client-side overlay.
 */
package com.phantom.module.impl.render;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import java.util.Locale;
import java.util.Properties;

public class ReachDisplay extends Module {
    private double lastReach = -1.0D;
    private boolean renderBackground = true;

    public ReachDisplay() {
        super("Reach Display",
                "Shows the distance to the last entity you hit as a small HUD element.\nDetectability: Safe",
                ModuleCategory.PLAYER,
                -1);
    }

    public void recordHit(Entity target) {
        if (mc.player == null || target == null) {
            return;
        }

        lastReach = mc.player.distanceTo(target);
    }

    @Override
    public void onHudRender(GuiGraphics graphics) {
        if (mc.options.hideGui) {
            return;
        }

        String value = lastReach < 0.0D ? "--" : String.format(Locale.ROOT, "%.2f", Mth.clamp(lastReach, 0.0D, 99.99D));
        String text = "Reach: " + value;
        int x = 8;
        int y = mc.getWindow().getGuiScaledHeight() - 22;
        int width = mc.font.width(text) + 8;

        if (renderBackground) {
            graphics.fill(x - 4, y - 3, x - 4 + width, y + 11, 0x90303030);
        }
        graphics.drawString(mc.font, Component.literal(text), x, y, 0xFFFFFFFF, true);
    }

    public boolean isRenderBackground() {
        return renderBackground;
    }

    public void setRenderBackground(boolean renderBackground) {
        this.renderBackground = renderBackground;
        saveConfig();
    }

    @Override
    public boolean hasConfigurableSettings() {
        return true;
    }

    @Override
    public Screen createSettingsScreen(Screen parent) {
        return new ModuleSettingsScreen(parent, this);
    }

    @Override
    public void loadConfig(Properties properties) {
        super.loadConfig(properties);
        renderBackground = Boolean.parseBoolean(properties.getProperty(
                "reach_display.render_background",
                Boolean.toString(renderBackground)));
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("reach_display.render_background", Boolean.toString(renderBackground));
    }
}
