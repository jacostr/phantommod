/* Copyright (c) 2026 PhantomMod. All rights reserved. */
/*
 * HitSelect.java — Advanced attack interruption for combat advantages.
 * Ported and enhanced for Fabric 1.21.11.
 */
package com.phantom.module.impl.combat;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import com.phantom.module.impl.player.AntiBot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

public class HitSelect extends Module {
    public enum Mode {
        BURST("Burst"),
        CRITICALS("Criticals");

        private final String label;
        Mode(String label) { this.label = label; }
        public String getLabel() { return label; }
        public Mode next() { return this == BURST ? CRITICALS : BURST; }
        public static Mode fromString(String value) {
            if (value == null) return BURST;
            try { return Mode.valueOf(value.trim().toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException ignored) { return BURST; }
        }
    }

    private Mode mode = Mode.BURST;
    private int pauseDurationMs = 500;
    private int waitForFirstHitMs = 0;
    private boolean disableDuringKnockback = false;
    private boolean onlyWhileDamaged = false;
    private double inCombatCancelRate = 100.0;
    private double missedSwingsCancelRate = 0.0;

    private Player currentTarget;
    private final Map<Integer, TargetState> targetStates = new HashMap<>();
    private int lastSelfHurtTime;
    private boolean takingKnockback;
    private long waitFirstStartMs = -1;
    private boolean waitFirstUnlocked;

    public HitSelect() {
        super("HitSelect",
                "Advanced attack filter for better combo timing.\nDetectability: Moderate",
                ModuleCategory.COMBAT,
                -1);
    }

    @Override
    public void onTick() {
        if (mc.player == null) {
            resetAllState();
            return;
        }

        pruneTargetStates();
        updateSelfDamage();
    }

    @Override
    public void onDisable() {
        resetAllState();
    }

    public boolean shouldCancelAttack(Entity target) {
        if (mc.player == null || target == null) return false;

        // Rate filtering
        if (target instanceof LivingEntity) {
            if (!shouldProcessByRate(inCombatCancelRate)) return false;
        } else {
            if (!shouldProcessByRate(missedSwingsCancelRate)) return false;
        }

        if (!(target instanceof Player playerTarget)) return false;

        updateCurrentTarget(playerTarget);
        TargetState state = getTargetState(playerTarget);

        if (disableDuringKnockback && isTakingKnockback()) return false;

        boolean shouldBlock = false;

        // Wait for first hit logic
        if (isWaitingForFirstHit()) {
            shouldBlock = true;
        }

        // Mode logic
        if (!shouldBlock) {
            if (mode == Mode.BURST) {
                shouldBlock = isPredictedBurstWindowActive(state);
            } else if (mode == Mode.CRITICALS) {
                shouldBlock = isCriticalsBlocked(state);
            }
        }

        if (shouldBlock) {
            return true;
        }

        // Record valid hit to start burst window if necessary
        recordPassedValidHit(playerTarget);
        return false;
    }

    private boolean isWaitingForFirstHit() {
        if (waitForFirstHitMs <= 0 || currentTarget == null || waitFirstUnlocked || waitFirstStartMs < 0) {
            return false;
        }
        return System.currentTimeMillis() - waitFirstStartMs < waitForFirstHitMs;
    }

    private boolean isCriticalsBlocked(TargetState state) {
        if (mc.player.onGround()) return false;
        if (onlyWhileDamaged && !state.firstSelfHitSeen) return false;
        if (disableDuringKnockback && isTakingKnockback()) return false;

        // Standard critical requirement: falling
        return mc.player.fallDistance <= 0.0F;
    }

    private boolean isPredictedBurstWindowActive(TargetState state) {
        if (state.predictedBurstWindowEndMs < 0) return false;
        long now = System.currentTimeMillis();
        return now < state.predictedBurstWindowEndMs;
    }

    private void recordPassedValidHit(Player target) {
        TargetState state = getTargetState(target);
        if (!isPredictedBurstWindowActive(state)) {
            long now = System.currentTimeMillis();
            state.predictedBurstWindowStartMs = now;
            state.predictedBurstWindowEndMs = now + pauseDurationMs;
        }
    }

    private void updateCurrentTarget(Player nextTarget) {
        if (currentTarget != null && currentTarget.getId() == nextTarget.getId()) return;

        currentTarget = nextTarget;
        waitFirstStartMs = System.currentTimeMillis();
        waitFirstUnlocked = false;
    }

    private void updateSelfDamage() {
        int hurtTime = mc.player.hurtTime;
        boolean hurtAgain = hurtTime > lastSelfHurtTime;

        if (hurtAgain) {
            waitFirstUnlocked = true;
            takingKnockback = true;
            if (currentTarget != null) {
                getTargetState(currentTarget).firstSelfHitSeen = true;
            }
        }

        if (takingKnockback && mc.player.onGround() && !hurtAgain) {
            takingKnockback = false;
        }

        lastSelfHurtTime = hurtTime;
    }

    private boolean isTakingKnockback() {
        return takingKnockback || mc.player.hurtTime > 0;
    }

    private boolean shouldProcessByRate(double rate) {
        if (rate <= 0) return false;
        if (rate >= 100) return true;
        return ThreadLocalRandom.current().nextDouble() * 100.0 < rate;
    }

    private TargetState getTargetState(Player target) {
        return targetStates.computeIfAbsent(target.getId(), k -> new TargetState());
    }

    private void pruneTargetStates() {
        if (mc.level == null) {
            targetStates.clear();
            return;
        }
        targetStates.entrySet().removeIf(entry -> {
            Entity en = mc.level.getEntity(entry.getKey());
            return !(en instanceof Player) || !en.isAlive();
        });
    }

    private void resetAllState() {
        currentTarget = null;
        targetStates.clear();
        lastSelfHurtTime = 0;
        takingKnockback = false;
        waitFirstStartMs = -1;
        waitFirstUnlocked = false;
    }

    private static class TargetState {
        boolean firstSelfHitSeen;
        long predictedBurstWindowStartMs = -1;
        long predictedBurstWindowEndMs = -1;
    }

    // Settings accessors
    public Mode getMode() { return mode; }
    public void cycleMode() { mode = mode.next(); saveConfig(); }
    public int getPauseDurationMs() { return pauseDurationMs; }
    public void setPauseDurationMs(int v) { this.pauseDurationMs = Math.max(0, Math.min(1000, v)); saveConfig(); }
    public int getWaitForFirstHitMs() { return waitForFirstHitMs; }
    public void setWaitForFirstHitMs(int v) { this.waitForFirstHitMs = Math.max(0, Math.min(1000, v)); saveConfig(); }
    public boolean isDisableDuringKnockback() { return disableDuringKnockback; }
    public void setDisableDuringKnockback(boolean v) { this.disableDuringKnockback = v; saveConfig(); }
    public boolean isOnlyWhileDamaged() { return onlyWhileDamaged; }
    public void setOnlyWhileDamaged(boolean v) { this.onlyWhileDamaged = v; saveConfig(); }
    public double getInCombatCancelRate() { return inCombatCancelRate; }
    public void setInCombatCancelRate(double v) { this.inCombatCancelRate = Math.max(0, Math.min(100, v)); saveConfig(); }
    public double getMissedSwingsCancelRate() { return missedSwingsCancelRate; }
    public void setMissedSwingsCancelRate(double v) { this.missedSwingsCancelRate = Math.max(0, Math.min(100, v)); saveConfig(); }

    @Override public boolean hasConfigurableSettings() { return true; }
    @Override public Screen createSettingsScreen(Screen parent) { return new ModuleSettingsScreen(parent, this); }

    @Override
    public void loadConfig(Properties properties) {
        super.loadConfig(properties);
        mode = Mode.fromString(properties.getProperty("hitselect.mode"));
        pauseDurationMs = Integer.parseInt(properties.getProperty("hitselect.pause_duration_ms", "500"));
        waitForFirstHitMs = Integer.parseInt(properties.getProperty("hitselect.wait_for_first_hit_ms", "0"));
        disableDuringKnockback = Boolean.parseBoolean(properties.getProperty("hitselect.disable_during_knockback", "false"));
        onlyWhileDamaged = Boolean.parseBoolean(properties.getProperty("hitselect.only_while_damaged", "false"));
        inCombatCancelRate = Double.parseDouble(properties.getProperty("hitselect.in_combat_cancel_rate", "100.0"));
        missedSwingsCancelRate = Double.parseDouble(properties.getProperty("hitselect.missed_swings_cancel_rate", "0.0"));
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("hitselect.mode", mode.name());
        properties.setProperty("hitselect.pause_duration_ms", Integer.toString(pauseDurationMs));
        properties.setProperty("hitselect.wait_for_first_hit_ms", Integer.toString(waitForFirstHitMs));
        properties.setProperty("hitselect.disable_during_knockback", Boolean.toString(disableDuringKnockback));
        properties.setProperty("hitselect.only_while_damaged", Boolean.toString(onlyWhileDamaged));
        properties.setProperty("hitselect.in_combat_cancel_rate", Double.toString(inCombatCancelRate));
        properties.setProperty("hitselect.missed_swings_cancel_rate", Double.toString(missedSwingsCancelRate));
    }
}