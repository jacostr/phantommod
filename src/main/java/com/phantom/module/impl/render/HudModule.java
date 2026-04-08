/*
 * HudModule.java — Top-right overlay showing active modules, FPS, and ping (Player module).
 *
 * Draws a compact, scaled text list in the top-right corner. Shows the mod name,
 * a list of currently enabled modules (toggleable), FPS counter, and current ping.
 * Each element can be shown/hidden independently via settings toggles.
 * Detectability: Safe — purely client-side visual overlay.
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
    private boolean showModuleList = true;
    private boolean showFps = true;
    private boolean showPing = true;
    private boolean alignLeft;

    public HudModule() {
        // HUD lives in the Player tab alongside other non-combat visual aids.
        super("HUD", "Shows a small top-right PhantomMod overlay with active features and optional FPS or ping text.\nDetectability: Safe",
                ModuleCategory.PLAYER, GLFW.GLFW_KEY_H);
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

        // Only show the active-module list if the user has it enabled in settings.
        if (showModuleList && PhantomMod.getModuleManager() != null) {
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

        drawCompactHud(graphics, lines);
    }

    public boolean isShowModuleList() {
        return showModuleList;
    }

    public void setShowModuleList(boolean showModuleList) {
        this.showModuleList = showModuleList;
        saveConfig();
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

    public boolean isAlignLeft() {
        return alignLeft;
    }

    public void setAlignLeft(boolean alignLeft) {
        this.alignLeft = alignLeft;
        saveConfig();
    }

    @Override
    public void loadConfig(Properties properties) {
        super.loadConfig(properties);
        showModuleList = Boolean.parseBoolean(properties.getProperty("hud.show_modules", Boolean.toString(showModuleList)));
        showFps = Boolean.parseBoolean(properties.getProperty("hud.show_fps", Boolean.toString(showFps)));
        showPing = Boolean.parseBoolean(properties.getProperty("hud.show_ping", Boolean.toString(showPing)));
        alignLeft = Boolean.parseBoolean(properties.getProperty("hud.align_left", Boolean.toString(alignLeft)));
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("hud.show_modules", Boolean.toString(showModuleList));
        properties.setProperty("hud.show_fps", Boolean.toString(showFps));
        properties.setProperty("hud.show_ping", Boolean.toString(showPing));
        properties.setProperty("hud.align_left", Boolean.toString(alignLeft));
    }

    private void drawCompactHud(GuiGraphics graphics, List<String> lines) {
        final float scale = 0.8F;
        int scaledWidth = (int) (mc.getWindow().getGuiScaledWidth() / scale);
        int y = 8;

        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int color = i == 0 ? 0xFFA8E6A3 : 0xFFFFFFFF;
            int x = alignLeft ? 8 : scaledWidth - mc.font.width(line) - 8;
            graphics.drawString(mc.font, Component.literal(line), x, y, color, true);
            y += i == 0 ? 11 : 10;
        }

        graphics.pose().popMatrix();
    }
}
