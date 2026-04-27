/* Copyright (c) 2026 PhantomMod. All rights reserved. */
/*
 * HudModule.java — Top-right overlay showing active modules, FPS, ping, and CPS.
 *
 * Draws a compact, scaled text list in the top-right corner. Shows the mod name,
 * a list of currently enabled modules (toggleable), FPS counter, current ping,
 * and a CPS (clicks per second) counter.
 * Each element can be shown/hidden independently via settings toggles.
 * Detectability: Safe — purely client-side visual overlay.
 */
package com.phantom.module.impl.render;

import com.phantom.PhantomMod;
import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.lwjgl.glfw.GLFW;

public class HudModule extends Module {
    public enum CornerSide {
        LEFT("Left"),
        RIGHT("Right");

        private final String label;

        CornerSide(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public CornerSide next() {
            return this == LEFT ? RIGHT : LEFT;
        }
    }

    private boolean showModuleList = true;
    private boolean showFps = true;
    private boolean showPing = true;
    private boolean showCps = true;
    private boolean alignLeft;
    private CornerSide statsSide = CornerSide.LEFT;
    private boolean debugLogger = false;
    private boolean fileLogger = false;

    // CPS tracking — ring buffer of click timestamps over the last 1000 ms
    private final long[] clickTimes = new long[20];
    private int clickHead = 0;

    public HudModule() {
        super("HUD",
                "Shows a small top-right PhantomMod overlay with active features and optional FPS, ping, or CPS text.\nDetectability: Safe",
                ModuleCategory.RENDER, GLFW.GLFW_KEY_H);
    }

    @Override
    public void onTick() {
        if (mc.player == null)
            return;
        if (mc.options.keyAttack.isDown()) {
            recordClick();
        }
    }

    /** Records a click timestamp; ignores repeats within the same 50 ms window. */
    public void recordClick() {
        long now = System.currentTimeMillis();
        if (clickHead > 0 && (now - clickTimes[(clickHead - 1 + clickTimes.length) % clickTimes.length]) < 50)
            return;
        clickTimes[clickHead % clickTimes.length] = now;
        clickHead++;
    }

    private int getCps() {
        long now = System.currentTimeMillis();
        int count = 0;
        for (long t : clickTimes) {
            if (t > 0 && now - t <= 1000)
                count++;
        }
        return count;
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
    public void onHudRender(GuiGraphics graphics) {
        if (mc.options.hideGui)
            return;

        int moduleListHeight = drawModuleHud(graphics);
        drawCornerStats(graphics, moduleListHeight);
    }

    public boolean isShowModuleList() {
        return showModuleList;
    }

    public void setShowModuleList(boolean v) {
        this.showModuleList = v;
        saveConfig();
    }

    public boolean isShowFps() {
        return showFps;
    }

    public void setShowFps(boolean v) {
        this.showFps = v;
        saveConfig();
    }

    public boolean isShowPing() {
        return showPing;
    }

    public void setShowPing(boolean v) {
        this.showPing = v;
        saveConfig();
    }

    public boolean isShowCps() {
        return showCps;
    }

    public void setShowCps(boolean v) {
        this.showCps = v;
        saveConfig();
    }

    public boolean isAlignLeft() {
        return alignLeft;
    }

    public void setAlignLeft(boolean v) {
        this.alignLeft = v;
        saveConfig();
    }

    public CornerSide getStatsSide() {
        return statsSide;
    }

    public void cycleStatsSide() {
        statsSide = statsSide.next();
        saveConfig();
    }

    public boolean isDebugLogger() { return debugLogger; }
    public void setDebugLogger(boolean v) {
        this.debugLogger = v;
        com.phantom.util.Logger.setDebugEnabled(v);
        saveConfig();
    }

    public boolean isFileLogger() { return fileLogger; }
    public void setFileLogger(boolean v) {
        this.fileLogger = v;
        com.phantom.util.Logger.setFileLoggingEnabled(v);
        saveConfig();
    }

    @Override
    public void loadConfig(Properties p) {
        super.loadConfig(p);
        showModuleList = Boolean.parseBoolean(p.getProperty("hud.show_modules", Boolean.toString(showModuleList)));
        showFps = Boolean.parseBoolean(p.getProperty("hud.show_fps", Boolean.toString(showFps)));
        showPing = Boolean.parseBoolean(p.getProperty("hud.show_ping", Boolean.toString(showPing)));
        showCps = Boolean.parseBoolean(p.getProperty("hud.show_cps", Boolean.toString(showCps)));
        alignLeft = Boolean.parseBoolean(p.getProperty("hud.align_left", Boolean.toString(alignLeft)));
        String side = p.getProperty("hud.stats_side");
        if (side != null) {
            try {
                statsSide = CornerSide.valueOf(side);
            } catch (IllegalArgumentException ignored) {
            }
        }
        debugLogger = Boolean.parseBoolean(p.getProperty("hud.debug_logger", "false"));
        fileLogger = Boolean.parseBoolean(p.getProperty("hud.file_logger", "false"));
        com.phantom.util.Logger.setDebugEnabled(debugLogger);
        com.phantom.util.Logger.setFileLoggingEnabled(fileLogger);
    }

    @Override
    public void saveConfig(Properties p) {
        super.saveConfig(p);
        p.setProperty("hud.show_modules", Boolean.toString(showModuleList));
        p.setProperty("hud.show_fps", Boolean.toString(showFps));
        p.setProperty("hud.show_ping", Boolean.toString(showPing));
        p.setProperty("hud.show_cps", Boolean.toString(showCps));
        p.setProperty("hud.align_left", Boolean.toString(alignLeft));
        p.setProperty("hud.stats_side", statsSide.name());
        p.setProperty("hud.debug_logger", Boolean.toString(debugLogger));
        p.setProperty("hud.file_logger", Boolean.toString(fileLogger));
    }

    private int drawModuleHud(GuiGraphics graphics) {
        List<String> lines = new ArrayList<>();
        lines.add("PHANTOM MOD");

        if (showModuleList && PhantomMod.getModuleManager() != null) {
            for (Module module : PhantomMod.getModuleManager().getModules()) {
                if (module == this || !module.isEnabled())
                    continue;
                lines.add(module.getName());
            }
        }

        final float scale = 0.8F;
        int scaledWidth = (int) (mc.getWindow().getGuiScaledWidth() / scale);
        int y = 8;

        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);

        net.minecraft.network.chat.FontDescription cleanFont = new net.minecraft.network.chat.FontDescription.Resource(net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "uniform"));
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int color = i == 0 ? 0xFFA8E6A3 : 0xFFFFFFFF;
            int x = alignLeft ? 8 : scaledWidth - mc.font.width(line) - 8;
            
            if (i == 0) {
                graphics.pose().pushMatrix();
                graphics.pose().scale(1.1f, 1.1f);
                graphics.drawString(mc.font, net.minecraft.network.chat.Component.literal(line).withStyle(s -> s.withFont(cleanFont)), (int)(x / 1.1f), (int)(y / 1.1f), color, true);
                graphics.pose().popMatrix();
                y += 12;
            } else {
                graphics.drawString(mc.font, net.minecraft.network.chat.Component.literal(line).withStyle(s -> s.withFont(cleanFont)), x, y, color, true);
                y += 10;
            }
        }

        graphics.pose().popMatrix();

        int rawHeight = 8 + (lines.isEmpty() ? 0 : 11 + Math.max(0, lines.size() - 1) * 10);
        return (int) (rawHeight * scale);
    }

    private void drawCornerStats(GuiGraphics graphics, int moduleListHeight) {
        final float scale = 0.8F;
        int scaledWidth = (int) (mc.getWindow().getGuiScaledWidth() / scale);

        boolean statsOnLeft = statsSide == CornerSide.LEFT;
        boolean sameSide = alignLeft == statsOnLeft;
        int y = sameSide ? (int) (moduleListHeight / scale) + 4 : 8;

        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);

        net.minecraft.network.chat.FontDescription cleanFont = new net.minecraft.network.chat.FontDescription.Resource(net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "uniform"));
        if (showFps) {
            String txt = "FPS: " + mc.getFps();
            graphics.drawString(mc.font, net.minecraft.network.chat.Component.literal(txt).withStyle(s -> s.withFont(cleanFont)),
                    statsOnLeft ? 8 : scaledWidth - mc.font.width(txt) - 8, y, 0xFFFFFFFF, true);
            y += 10;
        }

        if (showPing) {
            String txt = getPingText();
            graphics.drawString(mc.font, net.minecraft.network.chat.Component.literal(txt).withStyle(s -> s.withFont(cleanFont)),
                    statsOnLeft ? 8 : scaledWidth - mc.font.width(txt) - 8, y, 0xFFFFFFFF, true);
            y += 10;
        }

        if (showCps) {
            String txt = "CPS: " + getCps();
            graphics.drawString(mc.font, net.minecraft.network.chat.Component.literal(txt).withStyle(s -> s.withFont(cleanFont)),
                    statsOnLeft ? 8 : scaledWidth - mc.font.width(txt) - 8, y, 0xFFFFFFFF, true);
        }

        graphics.pose().popMatrix();
    }

    private String getPingText() {
        if (mc.player != null && mc.getConnection() != null) {
            PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            if (info != null && info.getLatency() > 1)
                return "Ping: " + info.getLatency() + "ms";
        }
        ServerData srv = mc.getCurrentServer();
        if (srv != null && srv.ping > 1L)
            return "Ping: " + srv.ping + "ms";
        return "Ping: local";
    }
}