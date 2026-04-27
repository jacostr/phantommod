/* Copyright (c) 2026 PhantomMod. All rights reserved. */
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
import net.minecraft.world.phys.Vec3;

import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

public class JumpReset extends Module {
    private double chance = 1.0D;
    private double accuracy = 0.8D;
    private double jumpChancePercent = 100.0D;
    private boolean onlyWhenTargeting = true;
    private boolean waterCheck = true;
    private boolean requireMouseDown = false;
    private boolean requireMovingForward = true;
    private boolean checkFOV = true;
    private int maxDelayTicks = 3;
    private int cooldownTicks = 6;

    private int lastHurtTime;
    private int scheduledJumpDelay = -1;
    private int cooldownRemaining;

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
            cooldownRemaining = 0;
            return;
        }

        if (waterCheck && (mc.player.isInWater() || mc.player.isInLava())) {
            scheduledJumpDelay = -1;
            cooldownRemaining = 0;
            lastHurtTime = mc.player.hurtTime;
            return;
        }

        if (mc.player.isOnFire() || hasBadEffect()) {
            scheduledJumpDelay = -1;
            cooldownRemaining = 0;
            lastHurtTime = mc.player.hurtTime;
            return;
        }

        if (cooldownRemaining > 0) {
            cooldownRemaining--;
        }

        int hurtTime = mc.player.hurtTime;
        if (hurtTime > lastHurtTime && cooldownRemaining <= 0 && shouldAttemptJump()) {
            scheduledJumpDelay = computeJumpDelay();
            cooldownRemaining = cooldownTicks;
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
        cooldownRemaining = 0;
    }

    private boolean shouldAttemptJump() {
        if (shouldPauseForBlockBreaking()) {
            return false;
        }

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

        if (requireMouseDown && !isAttackHeld()) {
            return false;
        }

        if (requireMovingForward && !mc.player.input.hasForwardImpulse()) {
            return false;
        }

        if (checkFOV && !isInFOV()) {
            return false;
        }

        return true;
    }

    private boolean hasBadEffect() {
        return mc.player.hasEffect(net.minecraft.world.effect.MobEffects.POISON) || 
               mc.player.hasEffect(net.minecraft.world.effect.MobEffects.WITHER);
    }

    private boolean isInFOV() {
        Entity focused = mc.crosshairPickEntity;
        if (focused == null) return false;
        
        Vec3 toTarget = focused.position().subtract(mc.player.position()).normalize();
        Vec3 look = mc.player.getLookAngle().normalize();
        double dot = toTarget.dot(look);
        // Approximately 90 degrees total FOV (45 degrees from center)
        return dot > 0.707; 
    }

    private int computeJumpDelay() {
        if (ThreadLocalRandom.current().nextDouble() <= accuracy) {
            return 0;
        }
        return ThreadLocalRandom.current().nextInt(1, Math.max(2, maxDelayTicks + 1));
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

    public boolean isRequireMouseDown() { return requireMouseDown; }
    public void setRequireMouseDown(boolean v) { this.requireMouseDown = v; saveConfig(); }

    public boolean isRequireMovingForward() { return requireMovingForward; }
    public void setRequireMovingForward(boolean v) { this.requireMovingForward = v; saveConfig(); }

    public boolean isCheckFOV() { return checkFOV; }
    public void setCheckFOV(boolean v) { this.checkFOV = v; saveConfig(); }

    public double getJumpChancePercent() {
        return jumpChancePercent;
    }

    public void setJumpChancePercent(double v) {
        this.jumpChancePercent = Mth.clamp(v, 0.0D, 100.0D);
        saveConfig();
    }

    public int getMaxDelayTicks() {
        return maxDelayTicks;
    }

    public void setMaxDelayTicks(int maxDelayTicks) {
        this.maxDelayTicks = Math.max(0, Math.min(6, maxDelayTicks));
        saveConfig();
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public void setCooldownTicks(int cooldownTicks) {
        this.cooldownTicks = Math.max(0, Math.min(20, cooldownTicks));
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
        requireMouseDown = Boolean.parseBoolean(properties.getProperty("jumpreset.require_mouse_down", Boolean.toString(requireMouseDown)));
        requireMovingForward = Boolean.parseBoolean(properties.getProperty("jumpreset.require_moving_forward", Boolean.toString(requireMovingForward)));
        checkFOV = Boolean.parseBoolean(properties.getProperty("jumpreset.check_fov", Boolean.toString(checkFOV)));
        String jcp = properties.getProperty("jumpreset.chance_percent");
        if (jcp != null) {
            try { jumpChancePercent = Mth.clamp(Double.parseDouble(jcp.trim()), 0.0D, 100.0D); } catch (NumberFormatException ignored) {}
        }
        String maxDelay = properties.getProperty("jumpreset.max_delay_ticks");
        if (maxDelay != null) {
            try { maxDelayTicks = Math.max(0, Math.min(6, Integer.parseInt(maxDelay.trim()))); } catch (NumberFormatException ignored) {}
        }
        String cooldown = properties.getProperty("jumpreset.cooldown_ticks");
        if (cooldown != null) {
            try { cooldownTicks = Math.max(0, Math.min(20, Integer.parseInt(cooldown.trim()))); } catch (NumberFormatException ignored) {}
        }
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("jumpreset.chance", Double.toString(chance));
        properties.setProperty("jumpreset.accuracy", Double.toString(accuracy));
        properties.setProperty("jumpreset.only_when_targeting", Boolean.toString(onlyWhenTargeting));
        properties.setProperty("jumpreset.water_check", Boolean.toString(waterCheck));
        properties.setProperty("jumpreset.require_mouse_down", Boolean.toString(requireMouseDown));
        properties.setProperty("jumpreset.require_moving_forward", Boolean.toString(requireMovingForward));
        properties.setProperty("jumpreset.check_fov", Boolean.toString(checkFOV));
        properties.setProperty("jumpreset.jump_chance_percent", Double.toString(jumpChancePercent));
        properties.setProperty("jumpreset.max_delay_ticks", Integer.toString(maxDelayTicks));
        properties.setProperty("jumpreset.cooldown_ticks", Integer.toString(cooldownTicks));
    }
}
