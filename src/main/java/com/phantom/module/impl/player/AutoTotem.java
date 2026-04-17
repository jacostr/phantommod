/* Copyright (c) 2026 PhantomMod. All rights reserved. */
/*
 * AutoTotem.java — Automatically equips a Totem of Undying to the offhand.
 *
 * Scans the player inventory each tick for totems and swaps one to the offhand
 * when available, with an optional health threshold to only equip when low.
 * Detectability: Moderate — fast inventory clicks visible in packets.
 */
package com.phantom.module.impl.player;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Properties;

public class AutoTotem extends Module {
    private double healthThreshold = 10.0;
    private boolean alwaysEquip = true;

    // After a totem pops (explosion/crystal), the server takes 1-2 ticks to clear
    // slot 45 and the cursor slot. We skip those ticks to let the sync settle before
    // attempting re-equip, otherwise the PICKUP clicks misfire against ghost items.
    private int syncCooldown = 0;
    private boolean hadTotemLastTick = false;

    public AutoTotem() {
        super("AutoTotem",
                "Automatically moves a Totem of Undying to your offhand.\nDetectability: Moderate",
                ModuleCategory.SMP,
                -1);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.gameMode == null || mc.screen != null) return;

        ItemStack offhand = mc.player.getOffhandItem();
        boolean hasTotemNow = !offhand.isEmpty() && offhand.getCount() > 0
                && offhand.getItem() == Items.TOTEM_OF_UNDYING;

        // Detect a totem pop: we had one last tick, now we don't.
        // Start a 2-tick cooldown to let the server sync slot 45 and the cursor slot
        // fully before we attempt to re-equip. Without this, the PICKUP clicks fire
        // against ghost/stale items and the totem ends up nowhere.
        if (hadTotemLastTick && !hasTotemNow) {
            syncCooldown = 2;
        }
        hadTotemLastTick = hasTotemNow;

        if (syncCooldown > 0) {
            syncCooldown--;
            return;
        }

        // Already equipped
        if (hasTotemNow) return;

        // Health threshold check
        if (!alwaysEquip && mc.player.getHealth() > healthThreshold) return;

        // If the cursor slot is still holding an item (e.g. leftover from a previous
        // failed pickup), drop it back into any free slot before starting our swap.
        // Sending PICKUP on slot -999 drops cursor contents onto the ground if no
        // slot is free, but we try to place it cleanly first.
        ItemStack cursor = mc.player.inventoryMenu.getCarried();
        if (!cursor.isEmpty()) {
            // Click slot 0 to dump cursor back into inventory
            mc.gameMode.handleInventoryMouseClick(
                    mc.player.inventoryMenu.containerId,
                    0,
                    0,
                    ClickType.PICKUP,
                    mc.player);
            // Give it a tick to settle
            syncCooldown = 1;
            return;
        }

        // Find a totem in inventory
        int totemSlot = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == Items.TOTEM_OF_UNDYING) {
                totemSlot = i;
                break;
            }
        }

        if (totemSlot == -1) return;

        // Convert inventory slot to container slot index
        // Hotbar: 0-8 -> container slots 36-44
        // Main inventory: 9-35 -> container slots 9-35
        int containerSlot = totemSlot < 9 ? totemSlot + 36 : totemSlot;

        // Pick up totem
        mc.gameMode.handleInventoryMouseClick(
                mc.player.inventoryMenu.containerId,
                containerSlot,
                0,
                ClickType.PICKUP,
                mc.player);

        // Place in offhand slot (slot 45)
        mc.gameMode.handleInventoryMouseClick(
                mc.player.inventoryMenu.containerId,
                45,
                0,
                ClickType.PICKUP,
                mc.player);

        // If there was something in offhand before, put it back in the original slot
        if (!offhand.isEmpty()) {
            mc.gameMode.handleInventoryMouseClick(
                    mc.player.inventoryMenu.containerId,
                    containerSlot,
                    0,
                    ClickType.PICKUP,
                    mc.player);
        }
    }

    public double getHealthThreshold() { return healthThreshold; }
    public void setHealthThreshold(double v) { healthThreshold = Mth.clamp(v, 0.0, 20.0); saveConfig(); }
    public boolean isAlwaysEquip() { return alwaysEquip; }
    public void setAlwaysEquip(boolean v) { alwaysEquip = v; saveConfig(); }

    @Override public boolean hasConfigurableSettings() { return true; }
    @Override public Screen createSettingsScreen(Screen parent) { return new ModuleSettingsScreen(parent, this); }

    @Override
    public void loadConfig(Properties p) {
        super.loadConfig(p);
        try { healthThreshold = Mth.clamp(Double.parseDouble(p.getProperty("autototem.threshold", "10.0")), 0.0, 20.0); } catch (Exception ignored) {}
        alwaysEquip = Boolean.parseBoolean(p.getProperty("autototem.always", Boolean.toString(alwaysEquip)));
    }

    @Override
    public void saveConfig(Properties p) {
        super.saveConfig(p);
        p.setProperty("autototem.threshold", Double.toString(healthThreshold));
        p.setProperty("autototem.always", Boolean.toString(alwaysEquip));
    }
}