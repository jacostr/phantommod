package com.phantom.module.impl.render;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class HudModule extends Module {
    // Config toggles shown in the HUD settings screen.
    private boolean showFps = true;
    private boolean showPing = true;

    public HudModule() {
        super("HUD", "Shows lightweight FPS and ping text.", ModuleCategory.RENDER, GLFW.GLFW_KEY_H);
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    public Screen createSettingsScreen(Screen parent) {
        return new ModuleSettingsScreen(parent, this);
    }

    // Draw a small top-left overlay so the player gets useful info without covering the
    // center of the screen during PvP.
    @Override
    public void onHudRender(GuiGraphics graphics) {
        if (mc.options.hideGui) return;

        int x = 8;
        int y = 8;

        // Keep the HUD intentionally simple so it feels like a clean PvP overlay.
        if (showFps) {
            graphics.drawString(mc.font, Component.literal("FPS: " + mc.getFps()), x, y, 0xFFFFFFFF, true);
            y += 12;
        }

        // In singleplayer or other local contexts, latency may report as 0, so show a
        // friendlier label instead of pretending the network ping is literally zero.
        if (showPing && mc.player != null && mc.getConnection() != null) {
            PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            if (playerInfo != null) {
                int latency = playerInfo.getLatency();
                String pingText = latency <= 0 ? "Ping: local" : "Ping: " + latency + "ms";
                graphics.drawString(mc.font, Component.literal(pingText), x, y, 0xFFFFFFFF, true);
            }
        }
    }

    public boolean isShowFps() {
        return showFps;
    }

    public void setShowFps(boolean showFps) {
        this.showFps = showFps;
    }

    public boolean isShowPing() {
        return showPing;
    }

    public void setShowPing(boolean showPing) {
        this.showPing = showPing;
    }
}
