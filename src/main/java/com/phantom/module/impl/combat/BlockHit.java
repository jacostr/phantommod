/*
 * BlockHit.java — Performs timed sword blocks around attack events.
 *
 * Detectability: Moderate to Blatant — aggressive use patterns are easy to profile.
 */
package com.phantom.module.impl.combat;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.Entity;

import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

public class BlockHit extends Module {
    public enum Mode {
        MANUAL("Manual"),
        AUTO("Auto"),
        PREDICT("Predict"),
        LAG("Lag");

        private final String label;

        Mode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public Mode next() {
            return switch (this) {
                case MANUAL -> AUTO;
                case AUTO -> PREDICT;
                case PREDICT -> LAG;
                case LAG -> MANUAL;
            };
        }

        public static Mode fromString(String value) {
            if (value == null) {
                return MANUAL;
            }
            try {
                return Mode.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return MANUAL;
            }
        }
    }

    private double chance = 0.65D;
    private boolean requireMouseDown;
    private Mode mode = Mode.MANUAL;

    private int holdTicksRemaining;
    private boolean holdingUse;

    public BlockHit() {
        super("BlockHit",
                "Automatically performs sword block-hits when you attack.\nDetectability: Moderate to Blatant",
                ModuleCategory.COMBAT,
                -1);
    }

    public void onAttack(Entity target) {
        if (mc.player == null || mc.options == null || target == null || !isHoldingSword()) {
            return;
        }
        if (requireMouseDown && !mc.options.keyUse.isDown()) {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() > chance) {
            return;
        }

        holdTicksRemaining = switch (mode) {
            case MANUAL -> 2;
            case AUTO -> 4;
            case PREDICT -> 5;
            case LAG -> 7;
        };
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.options == null || mc.screen != null) {
            releaseUse();
            return;
        }
        if (shouldPauseForBedMining()) {
            releaseUse();
            holdTicksRemaining = 0;
            return;
        }

        if (mode == Mode.PREDICT && mc.player.hurtTime > 0 && isHoldingSword()) {
            holdTicksRemaining = Math.max(holdTicksRemaining, 3);
        }

        if (holdTicksRemaining > 0) {
            holdTicksRemaining--;
            mc.options.keyUse.setDown(true);
            holdingUse = true;
            if (holdTicksRemaining == 0) {
                releaseUse();
            }
            return;
        }

        if (mode == Mode.AUTO && mc.options.keyAttack.isDown() && isHoldingSword()) {
            mc.options.keyUse.setDown(true);
            holdingUse = true;
            return;
        }

        releaseUse();
    }

    @Override
    public void onDisable() {
        releaseUse();
        holdTicksRemaining = 0;
    }

    private boolean isHoldingSword() {
        String id = mc.player.getMainHandItem().getItem().getDescriptionId().toLowerCase(Locale.ROOT);
        return id.contains("sword");
    }

    private void releaseUse() {
        if (holdingUse && mc.options != null) {
            mc.options.keyUse.setDown(false);
        }
        holdingUse = false;
    }

    public double getChance() {
        return chance;
    }

    public void setChance(double chance) {
        this.chance = Math.max(0.0D, Math.min(1.0D, chance));
        saveConfig();
    }

    public boolean isRequireMouseDown() {
        return requireMouseDown;
    }

    public void setRequireMouseDown(boolean requireMouseDown) {
        this.requireMouseDown = requireMouseDown;
        saveConfig();
    }

    public Mode getMode() {
        return mode;
    }

    public void cycleMode() {
        mode = mode.next();
        saveConfig();
    }

    public void applyPresetLegit() {
        setChance(0.35);
        setRequireMouseDown(false);
        mode = Mode.MANUAL;
        saveConfig();
    }

    public void applyPresetNormal() {
        setChance(0.55);
        setRequireMouseDown(false);
        mode = Mode.AUTO;
        saveConfig();
    }

    public void applyPresetObvious() {
        setChance(0.80);
        setRequireMouseDown(true);
        mode = Mode.PREDICT;
        saveConfig();
    }

    public void applyPresetBlatant() {
        setChance(1.0);
        setRequireMouseDown(false);
        mode = Mode.LAG;
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
        String chanceValue = properties.getProperty("blockhit.chance");
        if (chanceValue != null) {
            try {
                chance = Math.max(0.0D, Math.min(1.0D, Double.parseDouble(chanceValue.trim())));
            } catch (NumberFormatException ignored) {
            }
        }
        requireMouseDown = Boolean.parseBoolean(properties.getProperty("blockhit.require_mouse_down", Boolean.toString(requireMouseDown)));
        mode = Mode.fromString(properties.getProperty("blockhit.mode"));
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("blockhit.chance", Double.toString(chance));
        properties.setProperty("blockhit.require_mouse_down", Boolean.toString(requireMouseDown));
        properties.setProperty("blockhit.mode", mode.name());
    }
}
