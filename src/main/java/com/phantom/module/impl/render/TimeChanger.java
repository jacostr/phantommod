/* Copyright (c) 2026 PhantomMod. All rights reserved. */
package com.phantom.module.impl.render;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.gui.screens.Screen;

import java.util.Properties;

public class TimeChanger extends Module {
    private int targetTime = 6000;
    private boolean freezeTime = true;
    private long lastServerTime = 0;

    public TimeChanger() {
        super("TimeChanger", "Changes the world time locally.\nPresets: Day, Dusk, Midnight.", ModuleCategory.RENDER, -1);
    }

    @Override
    public void onTick() {
        if (mc.level != null && mc.level.getLevelData() instanceof net.minecraft.client.multiplayer.ClientLevel.ClientLevelData data) {
            if (freezeTime) {
                data.setDayTime(targetTime);
            } else {
                // To cleanly follow the cycle with an offset without a mixin:
                // Minecraft time progresses automatically. We just apply the difference.
                // It's easier to just let it run if not frozen, but Minecraft sets time from packets.
                // We will implement offset via mixin ideally, but for now we do simple offset by tracking delta.
                long realTime = data.getDayTime();
                data.setDayTime(realTime + targetTime);
            }
        }
    }

    public void setPresetDay() { setTargetTime(6000); }
    public void setPresetDusk() { setTargetTime(13000); }
    public void setPresetMidnight() { setTargetTime(18000); }
    public void setPresetDawn() { setTargetTime(23000); }

    public int getTargetTime() { return targetTime; }
    public void setTargetTime(double time) { this.targetTime = (int) time; saveConfig(); }
    public boolean isFreezeTime() { return freezeTime; }
    public void setFreezeTime(boolean freezeTime) { this.freezeTime = freezeTime; saveConfig(); }

    @Override public boolean hasConfigurableSettings() { return true; }
    @Override public Screen createSettingsScreen(Screen parent) { return new ModuleSettingsScreen(parent, this); }

    @Override
    public void loadConfig(Properties p) {
        super.loadConfig(p);
        try { targetTime = Integer.parseInt(p.getProperty("timechanger.time", "6000")); } catch (Exception ignored) {}
        freezeTime = Boolean.parseBoolean(p.getProperty("timechanger.freeze", "true"));
    }

    @Override
    public void saveConfig(Properties p) {
        super.saveConfig(p);
        p.setProperty("timechanger.time", Integer.toString(targetTime));
        p.setProperty("timechanger.freeze", Boolean.toString(freezeTime));
    }
}
