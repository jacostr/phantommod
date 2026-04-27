/* Copyright (c) 2026 PhantomMod. All rights reserved. */
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
import net.minecraft.world.entity.LivingEntity;

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
        Mode(String label) { this.label = label; }
        public String getLabel() { return label; }

        public Mode next() {
            return switch (this) {
                case MANUAL -> AUTO;
                case AUTO -> PREDICT;
                case PREDICT -> LAG;
                case LAG -> MANUAL;
            };
        }

        public static Mode fromString(String value) {
            if (value == null) return MANUAL;
            try {
                return Mode.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return MANUAL;
            }
        }
    }

    public enum Preset {
        LEGIT("Legit"), NORMAL("Normal"), OBVIOUS("Obvious"), BLATANT("Blatant");
        private final String name;
        Preset(String name) { this.name = name; }
        public String getName() { return name; }
        public Preset next() { return values()[(this.ordinal() + 1) % values().length]; }
    }

    private double chance = 0.65D;
    private boolean requireMouseDown = false;
    private boolean visualAnimation = true;
    private Mode mode = Mode.MANUAL;
    private Preset currentPreset = Preset.NORMAL;

    private int holdTicksRemaining = 0;
    private boolean holdingUse = false;

    // For PREDICT: track the last known hurtTime of our target
    private LivingEntity lastTarget = null;
    private float lastTargetAttackAnim = 0f;

    public BlockHit() {
        super("BlockHit",
                "Automatically performs sword block-hits when you attack.\nDetectability: Moderate to Blatant",
                ModuleCategory.COMBAT,
                -1);
    }

    public void onAttack(Entity target) {
        if (!isEnabled()) return;  // FIX: guard against disabled state
        if (mc.player == null || mc.options == null || target == null || !isHoldingSword()) return;
        if (requireMouseDown && !mc.options.keyUse.isDown()) return;
        if (ThreadLocalRandom.current().nextDouble() > chance) return;

        // Track target for PREDICT mode
        if (target instanceof LivingEntity le) {
            lastTarget = le;
        }

        holdTicksRemaining = switch (mode) {
            case MANUAL -> 2;
            case AUTO   -> 3;
            case PREDICT -> 4;
            // LAG: longer hold to ensure the block registers during high-latency windows
            case LAG    -> 6 + ThreadLocalRandom.current().nextInt(3); // 6–8 ticks, randomised
        };
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.options == null) {
            releaseUse();
            return;
        }

        // Release during open screens (chest, crafting, etc.)
        if (mc.screen != null) {
            releaseUse();
            holdTicksRemaining = 0;
            return;
        }

        // Release if no longer holding a sword mid-sequence
        if (holdTicksRemaining > 0 && !isHoldingSword()) {
            releaseUse();
            holdTicksRemaining = 0;
            return;
        }

        // PREDICT: watch the target's attack animation — if they're mid-swing, pre-block
        if (mode == Mode.PREDICT && lastTarget != null && lastTarget.isAlive()) {
            float targetAnim = lastTarget.attackAnim;
            // Target just started a swing (anim went from low to high)
            if (targetAnim > 0.5f && lastTargetAttackAnim <= 0.5f && isHoldingSword()) {
                holdTicksRemaining = Math.max(holdTicksRemaining, 4);
            }
            lastTargetAttackAnim = targetAnim;
        }

        // AUTO: hold block between your own swings, release as swing fires
        if (mode == Mode.AUTO && isHoldingSword()) {
            if (mc.options.keyAttack.isDown()) {
                float attackAnim = mc.player.attackAnim;
                if (attackAnim < 0.3f) {
                    // Cooldown is recovered / between swings — block
                    mc.options.keyUse.setDown(true);
                    holdingUse = true;
                } else {
                    // Mid-swing — release so the hit registers cleanly
                    releaseUse();
                }
                return;
            } else {
                releaseUse();
                return;
            }
        }

        // All other modes: tick down the hold counter
        if (holdTicksRemaining > 0) {
            holdTicksRemaining--;
            // If visualAnimation is off, only press on the first tick so no arm animation plays
            if (visualAnimation || holdTicksRemaining == (holdTicksRemaining + 1) - 1) {
                mc.options.keyUse.setDown(true);
                holdingUse = true;
            }
            if (holdTicksRemaining == 0) {
                releaseUse();
            }
            return;
        }

        releaseUse();
    }

    @Override
    public void onDisable() {
        releaseUse();
        holdTicksRemaining = 0;
        lastTarget = null;
    }

    private boolean isHoldingSword() {
        if (mc.player == null) return false;
        String id = mc.player.getMainHandItem().getItem().getDescriptionId().toLowerCase(Locale.ROOT);
        return id.contains("sword");
    }

    public boolean isHoldingUse() { return holdingUse; }

    private void releaseUse() {
        if (holdingUse && mc.options != null) {
            mc.options.keyUse.setDown(false);
        }
        holdingUse = false;
    }

    public Preset getCurrentPreset() { return currentPreset; }
    public void cyclePreset() {
        currentPreset = currentPreset.next();
        switch (currentPreset) {
            case LEGIT -> applyPresetLegit();
            case NORMAL -> applyPresetNormal();
            case OBVIOUS -> applyPresetObvious();
            case BLATANT -> applyPresetBlatant();
        }
    }

    // ── Getters / Setters ──────────────────────────────────────────────────────

    public double getChance() { return chance; }
    public void setChance(double chance) {
        this.chance = Math.max(0.0D, Math.min(1.0D, chance));
        saveConfig();
    }

    public boolean isRequireMouseDown() { return requireMouseDown; }
    public void setRequireMouseDown(boolean v) { requireMouseDown = v; saveConfig(); }

    /**
     * Whether the arm animation plays while blocking.
     * When false, keyUse is only pressed for a single tick so the server sees the
     * block but the arm raise animation is suppressed — less visually obvious.
     */
    public boolean isVisualAnimation() { return visualAnimation; }
    public void setVisualAnimation(boolean v) { visualAnimation = v; saveConfig(); }

    public Mode getMode() { return mode; }
    public void cycleMode() { mode = mode.next(); saveConfig(); }

    // ── Presets (matching Vape v4 style) ──────────────────────────────────────

    public void applyPresetLegit() {
        chance = 0.35;
        requireMouseDown = false;
        visualAnimation = true;
        mode = Mode.MANUAL;
        saveConfig();
    }

    public void applyPresetNormal() {
        chance = 0.55;
        requireMouseDown = false;
        visualAnimation = true;
        mode = Mode.AUTO;
        saveConfig();
    }

    public void applyPresetObvious() {
        chance = 0.80;
        requireMouseDown = true;
        visualAnimation = true;
        mode = Mode.PREDICT;
        saveConfig();
    }

    public void applyPresetBlatant() {
        chance = 1.0;
        requireMouseDown = false;
        visualAnimation = false; // suppress animation at max aggression
        mode = Mode.LAG;
        saveConfig();
    }

    // ── Config ─────────────────────────────────────────────────────────────────

    @Override public boolean hasConfigurableSettings() { return true; }
    @Override public Screen createSettingsScreen(Screen parent) { return new ModuleSettingsScreen(parent, this); }

    @Override
    public void loadConfig(Properties p) {
        super.loadConfig(p);
        String chanceValue = p.getProperty("blockhit.chance");
        if (chanceValue != null) {
            try { chance = Math.max(0.0D, Math.min(1.0D, Double.parseDouble(chanceValue.trim()))); }
            catch (NumberFormatException ignored) {}
        }
        requireMouseDown = Boolean.parseBoolean(p.getProperty("blockhit.require_mouse_down", Boolean.toString(requireMouseDown)));
        visualAnimation  = Boolean.parseBoolean(p.getProperty("blockhit.visual_animation",   Boolean.toString(visualAnimation)));
        mode = Mode.fromString(p.getProperty("blockhit.mode"));
    }

    @Override
    public void saveConfig(Properties p) {
        super.saveConfig(p);
        p.setProperty("blockhit.chance",            Double.toString(chance));
        p.setProperty("blockhit.require_mouse_down", Boolean.toString(requireMouseDown));
        p.setProperty("blockhit.visual_animation",   Boolean.toString(visualAnimation));
        p.setProperty("blockhit.mode",               mode.name());
    }
}