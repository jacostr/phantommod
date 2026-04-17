/* Copyright (c) 2026 PhantomMod. All rights reserved. */
package com.phantom.module.impl.render;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.gui.screens.Screen;

import java.util.Properties;

    
public class TimeChanger extends Module {
    public enum WeatherMode {
        DEFAULT("Server"), CLEAR("Clear"), RAIN("Rain"), THUNDER("Thunder");
        private final String label;
        WeatherMode(String label) { this.label = label; }
        public String getLabel() { return label; }
        public WeatherMode next() {
            return values()[(this.ordinal() + 1) % values().length];
        }
    }

    private int targetTime = 6000;
    private boolean freezeTime = true;
    private WeatherMode weatherMode = WeatherMode.DEFAULT;

    public TimeChanger() {
        super("Environment", "Changes world time & weather locally.", ModuleCategory.RENDER, -1);
    }

    @Override
    public void onTick() {
        if (mc.level != null && mc.level.getLevelData() instanceof net.minecraft.client.multiplayer.ClientLevel.ClientLevelData data) {
            if (freezeTime) {
                data.setDayTime(targetTime);
            } else {
                long realTime = data.getDayTime();
                data.setDayTime(realTime + targetTime);
            }
            
            if (weatherMode != WeatherMode.DEFAULT) {
                boolean rain = weatherMode == WeatherMode.RAIN || weatherMode == WeatherMode.THUNDER;
                data.setRaining(rain);
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
    public WeatherMode getWeatherMode() { return weatherMode; }
    public void cycleWeather() { weatherMode = weatherMode.next(); saveConfig(); }

    @Override public boolean hasConfigurableSettings() { return true; }
    @Override public Screen createSettingsScreen(Screen parent) { return new ModuleSettingsScreen(parent, this); }

    @Override
    public void loadConfig(Properties p) {
        super.loadConfig(p);
        try { targetTime = Integer.parseInt(p.getProperty("environment.time", "6000")); } catch (Exception ignored) {}
        freezeTime = Boolean.parseBoolean(p.getProperty("environment.freeze", "true"));
        try { weatherMode = WeatherMode.valueOf(p.getProperty("environment.weather", "DEFAULT")); } catch (Exception ignored) {}
    }

    @Override
    public void saveConfig(Properties p) {
        super.saveConfig(p);
        p.setProperty("environment.time", Integer.toString(targetTime));
        p.setProperty("environment.freeze", Boolean.toString(freezeTime));
        p.setProperty("environment.weather", weatherMode.name());
    }
}
