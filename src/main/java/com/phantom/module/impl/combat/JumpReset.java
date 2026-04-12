/*
 * JumpReset.java — Attempts a timed jump when you are hit to reduce knockback.
 *
 * Detectability: Subtle to Moderate — inaccurate/random timing is safer than perfect repeats.
 */
package com.phantom.module.impl.combat;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

public class JumpReset extends Module {
    private double chance = 1.0D;
    private double accuracy = 0.8D;
    private double jumpChancePercent = 100.0D;
    private boolean onlyWhenTargeting = true;
    private boolean waterCheck = true;

    private int lastHurtTime;
    private int scheduledJumpDelay = -1;

    public JumpReset() {
        super("JumpReset",
                "Attempts a timed jump when you are hit to reduce knockback.\nDetectability: Subtle to Moderate",
                ModuleCategory.COMBAT,
                -1);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.options == null) {
            scheduledJumpDelay = -1;
            return;
        }

        if (waterCheck && (mc.player.isInWater() || mc.player.isInLava())) {
            scheduledJumpDelay = -1;
            lastHurtTime = mc.player.hurtTime;
            return;
        }

        int hurtTime = mc.player.hurtTime;
        if (hurtTime > lastHurtTime && shouldAttemptJump()) {
            scheduledJumpDelay = computeJumpDelay();
        }
        lastHurtTime = hurtTime;

        if (scheduledJumpDelay < 0) {
            return;
        }

        if (scheduledJumpDelay == 0) {
            if (mc.player.onGround()) {
                mc.player.jumpFromGround();
            }
            scheduledJumpDelay = -1;
            return;
        }

        scheduledJumpDelay--;
    }

    @Override
    public void onDisable() {
        scheduledJumpDelay = -1;
        lastHurtTime = 0;
    }

    private boolean shouldAttemptJump() {
        if (ThreadLocalRandom.current().nextDouble() > chance) {
            return false;
        }

        // Jump chance % — additional roll to decide if the jump actually fires
        if (ThreadLocalRandom.current().nextDouble() * 100.0 > jumpChancePercent) {
            return false;
        }

        if (onlyWhenTargeting) {
            Entity focused = mc.crosshairPickEntity;
            if (!(focused instanceof LivingEntity living) || !living.isAlive()) {
                return false;
            }
        }

        return true;
    }

    private int computeJumpDelay() {
        if (ThreadLocalRandom.current().nextDouble() <= accuracy) {
            return 0;
        }
        return ThreadLocalRandom.current().nextInt(1, 4);
    }

    public double getChance() {
        return chance;
    }

    public void setChance(double chance) {
        this.chance = Mth.clamp(chance, 0.0D, 1.0D);
        saveConfig();
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = Mth.clamp(accuracy, 0.0D, 1.0D);
        saveConfig();
    }

    public boolean isOnlyWhenTargeting() {
        return onlyWhenTargeting;
    }

    public void setOnlyWhenTargeting(boolean onlyWhenTargeting) {
        this.onlyWhenTargeting = onlyWhenTargeting;
        saveConfig();
    }

    public boolean isWaterCheck() {
        return waterCheck;
    }

    public void setWaterCheck(boolean waterCheck) {
        this.waterCheck = waterCheck;
        saveConfig();
    }

    public double getJumpChancePercent() {
        return jumpChancePercent;
    }

    public void setJumpChancePercent(double v) {
        this.jumpChancePercent = Mth.clamp(v, 0.0D, 100.0D);
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
        String chanceValue = properties.getProperty("jumpreset.chance");
        if (chanceValue != null) {
            try {
                chance = Mth.clamp(Double.parseDouble(chanceValue.trim()), 0.0D, 1.0D);
            } catch (NumberFormatException ignored) {
            }
        }
        String accuracyValue = properties.getProperty("jumpreset.accuracy");
        if (accuracyValue != null) {
            try {
                accuracy = Mth.clamp(Double.parseDouble(accuracyValue.trim()), 0.0D, 1.0D);
            } catch (NumberFormatException ignored) {
            }
        }
        onlyWhenTargeting = Boolean.parseBoolean(properties.getProperty("jumpreset.only_when_targeting", Boolean.toString(onlyWhenTargeting)));
        waterCheck = Boolean.parseBoolean(properties.getProperty("jumpreset.water_check", Boolean.toString(waterCheck)));
        String jcp = properties.getProperty("jumpreset.jump_chance_percent");
        if (jcp != null) {
            try { jumpChancePercent = Mth.clamp(Double.parseDouble(jcp.trim()), 0.0D, 100.0D); } catch (NumberFormatException ignored) {}
        }
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("jumpreset.chance", Double.toString(chance));
        properties.setProperty("jumpreset.accuracy", Double.toString(accuracy));
        properties.setProperty("jumpreset.only_when_targeting", Boolean.toString(onlyWhenTargeting));
        properties.setProperty("jumpreset.water_check", Boolean.toString(waterCheck));
        properties.setProperty("jumpreset.jump_chance_percent", Double.toString(jumpChancePercent));
    }
}
