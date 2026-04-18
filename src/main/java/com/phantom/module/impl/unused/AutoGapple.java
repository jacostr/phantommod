/* Copyright (c) 2026 PhantomMod. All rights reserved. */
/*
 * AutoGapple.java — Swaps to a golden apple when health drops below threshold, eats it, swaps back.
 *
 * Scans hotbar for golden apples when health is low.
 * Uses InventoryUtil for reliable slot switching and synchronization.
 * Supports both regular (golden apple) and enchanted (notch apple).
 * Detectability: Moderate
 */
package com.phantom.module.impl.smp;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import com.phantom.util.InventoryUtil;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Properties;

public class AutoGapple extends Module {

    private float healthThreshold = 14.0f; // raw HP (7 hearts)
    private boolean preferEnchanted = false; // prefer notch apple over regular
    private boolean allowEnchanted = true;

    private enum State { WAITING, SWAP, EATING, SWAP_BACK }
    private State state = State.WAITING;
    private int oldSlot = -1;
    private int gappleSlot = -1;
    private int eatTicks = 0;
    private static final int EAT_DURATION = 32; // ticks to hold use for a full eat

    public AutoGapple() {
        super("AutoGapple",
                "Automatically eats a golden apple when health is low, then swaps back.\nDetectability: Moderate",
                ModuleCategory.SMP, -1);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.options == null) return;

        if (state == State.WAITING) {
            if (shouldEat()) {
                gappleSlot = findGapple();
                if (gappleSlot == -1) return;
                oldSlot = InventoryUtil.getSelectedSlot();
                state = State.SWAP;
            }
            return;
        }

        // Abort if health recovered mid-eat (e.g. totem popped or already ate)
        if (state == State.EATING && (mc.player.getHealth() > healthThreshold + 4.0f || mc.player.getAbsorptionAmount() > 0)) {
            stopEating();
            restoreSlotAndReset();
            return;
        }

        switch (state) {
            case SWAP:
                InventoryUtil.setSelectedSlot(gappleSlot);
                state = State.EATING;
                eatTicks = EAT_DURATION;
                break;

            case EATING:
                if (eatTicks > 0) {
                    mc.options.keyUse.setDown(true);
                    eatTicks--;
                } else {
                    stopEating();
                    state = State.SWAP_BACK;
                }
                break;

            case SWAP_BACK:
                restoreSlotAndReset();
                break;

            default:
                stopEating();
                restoreSlotAndReset();
                break;
        }
    }

    private boolean shouldEat() {
        if (mc.player.getHealth() > healthThreshold) return false;
        // Don't eat if already have absorption (gapple effect active)
        if (mc.player.getAbsorptionAmount() > 0) return false;
        return true;
    }

    private int findGapple() {
        int regularSlot = -1;
        int enchantedSlot = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE && allowEnchanted) {
                enchantedSlot = i;
            } else if (stack.getItem() == Items.GOLDEN_APPLE) {
                regularSlot = i;
            }
        }

        if (preferEnchanted && enchantedSlot != -1) return enchantedSlot;
        if (regularSlot != -1) return regularSlot;
        if (enchantedSlot != -1) return enchantedSlot;
        return -1;
    }

    private void stopEating() {
        if (mc.options != null) mc.options.keyUse.setDown(false);
    }

    private void restoreSlotAndReset() {
        if (oldSlot != -1) InventoryUtil.setSelectedSlot(oldSlot);
        reset();
    }

    private void reset() {
        state = State.WAITING;
        oldSlot = -1;
        gappleSlot = -1;
        eatTicks = 0;
    }

    @Override
    public void onDisable() {
        stopEating();
        restoreSlotAndReset();
    }

    public float getHealthThreshold() { return healthThreshold; }
    public void setHealthThreshold(float v) { healthThreshold = Math.max(2.0f, Math.min(18.0f, v)); saveConfig(); }
    public boolean isPreferEnchanted() { return preferEnchanted; }
    public void setPreferEnchanted(boolean v) { preferEnchanted = v; saveConfig(); }
    public boolean isAllowEnchanted() { return allowEnchanted; }
    public void setAllowEnchanted(boolean v) { allowEnchanted = v; saveConfig(); }

    @Override public boolean hasConfigurableSettings() { return true; }
    @Override public Screen createSettingsScreen(Screen parent) { return new ModuleSettingsScreen(parent, this); }

    @Override
    public void loadConfig(Properties p) {
        super.loadConfig(p);
        try { healthThreshold = Float.parseFloat(p.getProperty("autogapple.threshold", "14.0")); } catch (Exception ignored) {}
        preferEnchanted = Boolean.parseBoolean(p.getProperty("autogapple.prefer_enchanted", "false"));
        allowEnchanted  = Boolean.parseBoolean(p.getProperty("autogapple.allow_enchanted", "true"));
    }

    @Override
    public void saveConfig(Properties p) {
        super.saveConfig(p);
        p.setProperty("autogapple.threshold",        Float.toString(healthThreshold));
        p.setProperty("autogapple.prefer_enchanted",  Boolean.toString(preferEnchanted));
        p.setProperty("autogapple.allow_enchanted",   Boolean.toString(allowEnchanted));
    }
}
