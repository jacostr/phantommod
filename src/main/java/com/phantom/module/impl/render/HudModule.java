/*
 * Render: small overlay listing enabled modules plus optional FPS and ping.
 */
package com.phantom.module.impl.render;

import com.phantom.PhantomMod;
import com.phantom.module.Module;
import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.lwjgl.glfw.GLFW;

public class HudModule extends Module {
    // Config toggles shown in the HUD settings screen.
    private boolean showFps = true;
    private boolean showPing = true;

    public HudModule() {
        super("HUD", "Shows a small top-right PhantomMod overlay with active features and optional FPS or ping text.",
                ModuleCategory.RENDER, GLFW.GLFW_KEY_H);
    }

    @Override
    public boolean hasConfigurableSettings() {
        return true;
    }

    @Override
    public Screen createSettingsScreen(Screen parent) {
        return new ModuleSettingsScreen(parent, this);
    }

    // Compact top-right overlay so the player gets useful info without covering the center of the screen.
    @Override
    public void onHudRender(GuiGraphics graphics) {
        if (mc.options.hideGui)
            return;

        List<String> lines = new ArrayList<>();
        lines.add("PhantomMod");

        if (PhantomMod.getModuleManager() != null) {
            for (Module module : PhantomMod.getModuleManager().getModules()) {
                if (module == this || !module.isEnabled()) {
                    continue;
                }

                lines.add(module.getName());
            }
        }

        if (showFps) {
            lines.add("FPS: " + mc.getFps());
        }

        if (showPing && mc.player != null && mc.getConnection() != null) {
            PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            if (playerInfo != null) {
                int latency = playerInfo.getLatency();
                lines.add(latency <= 0 ? "Ping: local" : "Ping: " + latency + "ms");
            }
        }

        drawCompactTopRight(graphics, lines);
    }

    public boolean isShowFps() {
        return showFps;
    }

    public void setShowFps(boolean showFps) {
        this.showFps = showFps;
        saveConfig();
    }

    public boolean isShowPing() {
        return showPing;
    }

    public void setShowPing(boolean showPing) {
        this.showPing = showPing;
        saveConfig();
    }

    @Override
    public void loadConfig(Properties properties) {
        super.loadConfig(properties);
        showFps = Boolean.parseBoolean(properties.getProperty("hud.show_fps", Boolean.toString(showFps)));
        showPing = Boolean.parseBoolean(properties.getProperty("hud.show_ping", Boolean.toString(showPing)));
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("hud.show_fps", Boolean.toString(showFps));
        properties.setProperty("hud.show_ping", Boolean.toString(showPing));
    }

    private void drawCompactTopRight(GuiGraphics graphics, List<String> lines) {
        final float scale = 0.8F;
        int scaledWidth = (int) (mc.getWindow().getGuiScaledWidth() / scale);
        int y = 8;

        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int color = i == 0 ? 0xFFA8E6A3 : 0xFFFFFFFF;
            int x = scaledWidth - mc.font.width(line) - 8;
            graphics.drawString(mc.font, Component.literal(line), x, y, color, true);
            y += i == 0 ? 11 : 10;
        }

        graphics.pose().popMatrix();
    }
}
