/* Copyright (c) 2026 PhantomMod. All rights reserved. */
/*
 * Velocity.java — Reduces incoming knockback by a configurable percentage (Combat module).
 *
 * Works via ClientPacketListenerMixin which scales player.deltaMovement after the server
 * applies knockback. A global kbPercent slider (0%=none, 100%=vanilla) controls strength.
 * Presets: Legit 90%, Subtle 75%, Blatant 40%, None 0%.
 * Detectability: Subtle at ≥70%, Blatant at low values.
 */
package com.phantom.module.impl.combat;

import com.phantom.gui.ModuleSettingsScreen;

import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.gui.screens.Screen;

import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

public class Velocity extends Module {
    public enum Mode {
        LEGIT("Legit"),
        SUBTLE("Subtle"),
        BLATANT("Blatant"),
        NONE("None");

        private final String label;
        Mode(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private Mode mode = Mode.LEGIT;
    private double kbPercent = 0.9;
    private double horizontalPercent = 0.9;
    private double verticalPercent = 1.0;
    private double chance = 1.0;
    private boolean onlyWhileTargeting = false;
    private boolean disableWhileHoldingS = false;
    private boolean pulseMode = false;
    private int pulseInterval = 20;
    private int pulseTicks = 0;

    public Velocity() {
        super("Velocity", "Reduces incoming knockback according to your percentage.\nDetectability: Blatant/Subtle", ModuleCategory.COMBAT, -1);
    }

    @Override
    public boolean hasConfigurableSettings() {
        return true;
    }

    @Override
    public Screen createSettingsScreen(Screen parent) {
        return new ModuleSettingsScreen(parent, this);
    }

    public double getKbPercent() {
        return kbPercent;
    }

    public void setKbPercent(double kbPercent) {
        this.kbPercent = Math.max(0.0, Math.min(1.0, kbPercent));
        this.horizontalPercent = this.kbPercent;
        this.verticalPercent = this.kbPercent;
        saveConfig();
    }

    public double getHorizontalPercent() {
        return horizontalPercent;
    }

    public void setHorizontalPercent(double horizontalPercent) {
        this.horizontalPercent = Math.max(0.0, Math.min(1.0, horizontalPercent));
        this.kbPercent = (this.horizontalPercent + this.verticalPercent) * 0.5;
        saveConfig();
    }

    public double getVerticalPercent() {
        return verticalPercent;
    }

    public void setVerticalPercent(double verticalPercent) {
        this.verticalPercent = Math.max(0.0, Math.min(1.0, verticalPercent));
        this.kbPercent = (this.horizontalPercent + this.verticalPercent) * 0.5;
        saveConfig();
    }

    public double getChance() {
        return chance;
    }

    public void setChance(double chance) {
        this.chance = Math.max(0.0, Math.min(1.0, chance));
        saveConfig();
    }

    public boolean isOnlyWhileTargeting() {
        return onlyWhileTargeting;
    }

    public void setOnlyWhileTargeting(boolean onlyWhileTargeting) {
        this.onlyWhileTargeting = onlyWhileTargeting;
        saveConfig();
    }

    public boolean isDisableWhileHoldingS() {
        return disableWhileHoldingS;
    }

    public void setDisableWhileHoldingS(boolean disableWhileHoldingS) {
        this.disableWhileHoldingS = disableWhileHoldingS;
        saveConfig();
    }

    public boolean shouldApplyVelocity() {
        if (mc.player == null) {
            return false;
        }
        if (onlyWhileTargeting && mc.crosshairPickEntity == null) {
            return false;
        }
        if (disableWhileHoldingS && mc.options != null && mc.options.keyDown.isDown()) {
            return false;
        }

        if (pulseMode) {
            pulseTicks++;
            if (pulseTicks >= pulseInterval) {
                pulseTicks = 0;
            } else {
                return false;
            }
        }

        return chance >= 1.0 || ThreadLocalRandom.current().nextDouble() <= chance;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        switch (mode) {
            case LEGIT -> { horizontalPercent = 0.90; verticalPercent = 1.0; }
            case SUBTLE -> { horizontalPercent = 0.75; verticalPercent = 0.90; }
            case BLATANT -> { horizontalPercent = 0.40; verticalPercent = 0.60; }
            case NONE -> { horizontalPercent = 0.0; verticalPercent = 0.0; }
        }
        kbPercent = (horizontalPercent + verticalPercent) * 0.5;
        saveConfig();
    }

    public Mode getMode() { return mode; }

    public void cycleMode() {
        Mode[] modes = Mode.values();
        mode = modes[(mode.ordinal() + 1) % modes.length];
        setMode(mode);
    }

    public boolean isPulseMode() { return pulseMode; }
    public void setPulseMode(boolean v) { pulseMode = v; saveConfig(); }
    public int getPulseInterval() { return pulseInterval; }
    public void setPulseInterval(int v) { pulseInterval = Math.max(1, v); saveConfig(); }

    @Override
    public void loadConfig(Properties properties) {
        super.loadConfig(properties);
        try {
            mode = Mode.valueOf(properties.getProperty("velocity.mode", "LEGIT"));
        } catch (Exception ignored) {}
        setMode(mode);

        String h = properties.getProperty("velocity.horizontal");
        if (h != null) {
            try { horizontalPercent = Double.parseDouble(h); } catch (Exception ignored) {}
        }
        String v = properties.getProperty("velocity.vertical");
        if (v != null) {
            try { verticalPercent = Double.parseDouble(v); } catch (Exception ignored) {}
        }
        String interval = properties.getProperty("velocity.pulse_interval");
        if (interval != null) {
            try { pulseInterval = Integer.parseInt(interval); } catch (Exception ignored) {}
        }

        String chanceValue = properties.getProperty("velocity.chance");
        if (chanceValue != null) {
            try {
                chance = Math.max(0.0, Math.min(1.0, Double.parseDouble(chanceValue.trim())));
            } catch (NumberFormatException ignored) {}
        }
        onlyWhileTargeting = Boolean.parseBoolean(
                properties.getProperty("velocity.only_while_targeting", Boolean.toString(onlyWhileTargeting)));
        disableWhileHoldingS = Boolean.parseBoolean(
                properties.getProperty("velocity.disable_while_holding_s", Boolean.toString(disableWhileHoldingS)));
        pulseMode = Boolean.parseBoolean(properties.getProperty("velocity.pulse", "false"));
        try { pulseInterval = Integer.parseInt(properties.getProperty("velocity.pulse_interval", "20")); } catch (Exception ignored) {}
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("velocity.mode", mode.name());
        properties.setProperty("velocity.horizontal", Double.toString(horizontalPercent));
        properties.setProperty("velocity.vertical", Double.toString(verticalPercent));
        properties.setProperty("velocity.chance", Double.toString(chance));
        properties.setProperty("velocity.only_while_targeting", Boolean.toString(onlyWhileTargeting));
        properties.setProperty("velocity.disable_while_holding_s", Boolean.toString(disableWhileHoldingS));
        properties.setProperty("velocity.pulse", Boolean.toString(pulseMode));
        properties.setProperty("velocity.pulse_interval", Integer.toString(pulseInterval));
    }
}
