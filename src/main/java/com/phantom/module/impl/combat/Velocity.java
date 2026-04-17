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
    private double kbPercent = 0.0; // 0.0 = 0% kb (None), 1.0 = 100% (Vanilla)
    private double horizontalPercent = 0.0;
    private double verticalPercent = 0.0;
    private double chance = 1.0;
    private boolean hypixelMode = false;
    private boolean onlyWhileTargeting = false;
    private boolean disableWhileHoldingS = false;

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

    public boolean isHypixelMode() {
        return hypixelMode;
    }

    public void setHypixelMode(boolean hypixelMode) {
        this.hypixelMode = hypixelMode;
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
        return chance >= 1.0 || ThreadLocalRandom.current().nextDouble() <= chance;
    }

    public void applyPresetLegit() {
        setHorizontalPercent(0.90);
        setVerticalPercent(1.0);
    }

    public void applyPresetSubtle() {
        setHorizontalPercent(0.75);
        setVerticalPercent(0.90);
    }

    public void applyPresetBlatant() {
        setHorizontalPercent(0.40);
        setVerticalPercent(0.60);
    }

    public void applyPresetNone() {
        setHorizontalPercent(0.0);
        setVerticalPercent(0.0);
    }

    @Override
    public void loadConfig(Properties properties) {
        super.loadConfig(properties);
        String v = properties.getProperty("velocity.kb");
        if (v != null) {
            try {
                kbPercent = Math.max(0.0, Math.min(1.0, Double.parseDouble(v.trim())));
            } catch (NumberFormatException ignored) {}
        }
        String horizontal = properties.getProperty("velocity.horizontal");
        if (horizontal != null) {
            try {
                horizontalPercent = Math.max(0.0, Math.min(1.0, Double.parseDouble(horizontal.trim())));
            } catch (NumberFormatException ignored) {
            }
        } else {
            horizontalPercent = kbPercent;
        }
        String vertical = properties.getProperty("velocity.vertical");
        if (vertical != null) {
            try {
                verticalPercent = Math.max(0.0, Math.min(1.0, Double.parseDouble(vertical.trim())));
            } catch (NumberFormatException ignored) {
            }
        } else {
            verticalPercent = kbPercent;
        }
        String chanceValue = properties.getProperty("velocity.chance");
        if (chanceValue != null) {
            try {
                chance = Math.max(0.0, Math.min(1.0, Double.parseDouble(chanceValue.trim())));
            } catch (NumberFormatException ignored) {
            }
        }
        hypixelMode = Boolean.parseBoolean(properties.getProperty("velocity.hypixel", Boolean.toString(hypixelMode)));
        onlyWhileTargeting = Boolean.parseBoolean(
                properties.getProperty("velocity.only_while_targeting", Boolean.toString(onlyWhileTargeting)));
        disableWhileHoldingS = Boolean.parseBoolean(
                properties.getProperty("velocity.disable_while_holding_s", Boolean.toString(disableWhileHoldingS)));
        kbPercent = (horizontalPercent + verticalPercent) * 0.5;
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("velocity.kb", Double.toString(kbPercent));
        properties.setProperty("velocity.horizontal", Double.toString(horizontalPercent));
        properties.setProperty("velocity.vertical", Double.toString(verticalPercent));
        properties.setProperty("velocity.chance", Double.toString(chance));
        properties.setProperty("velocity.hypixel", Boolean.toString(hypixelMode));
        properties.setProperty("velocity.only_while_targeting", Boolean.toString(onlyWhileTargeting));
        properties.setProperty("velocity.disable_while_holding_s", Boolean.toString(disableWhileHoldingS));
    }
}
