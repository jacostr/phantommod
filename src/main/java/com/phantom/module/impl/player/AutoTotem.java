/* Copyright (c) 2026 PhantomMod. All rights reserved. */
/*
 * AutoTotem.java — Automatically equips a Totem of Undying to the offhand.
 *
 * Pop detection is packet-driven (ClientboundEntityEventPacket status 35) via
 * the onEntityEvent hook added to Module/ModuleManager. A randomised millisecond
 * delay is applied between the pickup and place clicks to reduce the fixed-timing
 * fingerprint of the original back-to-back same-tick clicks.
 * Detectability: Low-Moderate
 */
package com.phantom.module.impl.player;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.util.Logger;
import com.phantom.module.Module;

import com.phantom.module.ModuleCategory;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Properties;
import java.util.Random;

public class AutoTotem extends Module {

    // ── Settings ──────────────────────────────────────────────────────────────

    /** Only re-equip when health is at or below this value (ignored when alwaysEquip). */
    private double healthThreshold = 10.0;

    /** When true, keep a totem equipped regardless of current health. */
    private boolean alwaysEquip = true;

    /**
     * Random delay range (ms) applied between the two inventory clicks after a pop.
     * Jittering the interval avoids a fixed packet-timing fingerprint.
     */
    private int delayMin = 30;
    private int delayMax = 60;

    // ── Internal state ────────────────────────────────────────────────────────

    /** Set true by onEntityEvent when a totem pop packet arrives. */
    private volatile boolean popPending = false;

    /** Millis timestamp after which the next click step may fire. */
    private long nextClickTime = 0L;

    /**
     * Equip sequence step:
     *   0 = pick up totem from inventory slot
     *   1 = place totem into offhand slot 45
     *   2 = (optional) return displaced offhand item to source slot
     */
    private int equipStep = 0;

    /** Container slot we grabbed the totem from, so we can return displaced items. */
    private int pendingContainerSlot = -1;

    /** Snapshot of the offhand item at the moment we started the sequence. */
    private ItemStack offhandSnapshot = ItemStack.EMPTY;

    private final Random rng = new Random();

    // ─────────────────────────────────────────────────────────────────────────

    public AutoTotem() {
        super("AutoTotem",
                "Automatically moves a Totem of Undying to your offhand.\nDetectability: Low-Moderate",
                ModuleCategory.SMP,
                -1);
    }

    // ── Packet hook (called by ModuleManager.onEntityEvent) ───────────────────

    /**
     * Entity status 35 = totem of undying activated.
     * Flag popPending so onTick() schedules the re-equip sequence with a random delay.
     */
    @Override
    public void onEntityEvent(ClientboundEntityEventPacket packet) {
        if (mc.player == null || mc.level == null) return;
        if (packet.getEntity(mc.level) != mc.player) return;
        if (packet.getEventId() != 35) return;

        popPending = true;
        equipStep = 0;
        pendingContainerSlot = -1;
        offhandSnapshot = ItemStack.EMPTY;
        nextClickTime = System.currentTimeMillis() + randomDelay();
    }

    // ── Tick loop ─────────────────────────────────────────────────────────────

    @Override
    public void onTick() {
        if (mc.player == null || mc.gameMode == null) return;

        ItemStack offhand = mc.player.getOffhandItem();
        boolean hasTotem = !offhand.isEmpty() && offhand.getItem() == Items.TOTEM_OF_UNDYING;

        // ── Normal equip path: no pop pending, totem just missing ─────────────
        if (!popPending) {
            if (hasTotem) return;
            if (mc.screen != null) return;
            if (!alwaysEquip && mc.player.getHealth() > healthThreshold) return;

            // Clear stale cursor item before touching inventory
            if (!mc.player.inventoryMenu.getCarried().isEmpty()) {
                mc.gameMode.handleInventoryMouseClick(
                        mc.player.inventoryMenu.containerId, 0, 0, ClickType.PICKUP, mc.player);
                return;
            }

            int slot = findTotemContainerSlot();
            if (slot == -1) return;

            // Grab and place in the same tick — fine for the non-pop case
            mc.gameMode.handleInventoryMouseClick(
                    mc.player.inventoryMenu.containerId, slot, 0, ClickType.PICKUP, mc.player);
            mc.gameMode.handleInventoryMouseClick(
                    mc.player.inventoryMenu.containerId, 45, 0, ClickType.PICKUP, mc.player);

            // Return any displaced offhand item
            if (!offhand.isEmpty()) {
                mc.gameMode.handleInventoryMouseClick(
                        mc.player.inventoryMenu.containerId, slot, 0, ClickType.PICKUP, mc.player);
            }
            return;
        }

        // ── Post-pop re-equip path: randomised, multi-step ────────────────────
        if (System.currentTimeMillis() < nextClickTime) return;

        switch (equipStep) {
            case 0 -> {
                // Step 0: clear dirty cursor then pick up a totem
                if (!mc.player.inventoryMenu.getCarried().isEmpty()) {
                    mc.gameMode.handleInventoryMouseClick(
                            mc.player.inventoryMenu.containerId, 0, 0, ClickType.PICKUP, mc.player);
                    nextClickTime = System.currentTimeMillis() + randomDelay();
                    return;
                }

                pendingContainerSlot = findTotemContainerSlot();
                if (pendingContainerSlot == -1) {
                    popPending = false;
                    return;
                }

                offhandSnapshot = mc.player.getOffhandItem().copy();
                mc.gameMode.handleInventoryMouseClick(
                        mc.player.inventoryMenu.containerId,
                        pendingContainerSlot, 0, ClickType.PICKUP, mc.player);

                equipStep = 1;
                nextClickTime = System.currentTimeMillis() + randomDelay();
            }
            case 1 -> {
                // Step 1: place totem into offhand slot 45
                mc.gameMode.handleInventoryMouseClick(
                        mc.player.inventoryMenu.containerId, 45, 0, ClickType.PICKUP, mc.player);

                if (!offhandSnapshot.isEmpty()) {
                    equipStep = 2;
                    nextClickTime = System.currentTimeMillis() + randomDelay();
                } else {
                    popPending = false;
                    equipStep = 0;
                }
            }
            case 2 -> {
                // Step 2 (optional): return displaced offhand item to source slot
                mc.gameMode.handleInventoryMouseClick(
                        mc.player.inventoryMenu.containerId,
                        pendingContainerSlot, 0, ClickType.PICKUP, mc.player);
                popPending = false;
                equipStep = 0;
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int findTotemContainerSlot() {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == Items.TOTEM_OF_UNDYING) {
                return i < 9 ? i + 36 : i;
            }
        }
        return -1;
    }

    private long randomDelay() {
        return delayMin + rng.nextInt(Math.max(1, delayMax - delayMin + 1));
    }

    // ── Getters / setters ─────────────────────────────────────────────────────

    public double getHealthThreshold()         { return healthThreshold; }
    public void   setHealthThreshold(double v) { healthThreshold = Mth.clamp(v, 0.0, 20.0); saveConfig(); }
    public boolean isAlwaysEquip()             { return alwaysEquip; }
    public void    setAlwaysEquip(boolean v)   { alwaysEquip = v; saveConfig(); }
    public int  getDelayMin()                  { return delayMin; }
    public void setDelayMin(int v)             { delayMin = Mth.clamp(v, 0, delayMax); saveConfig(); }
    public int  getDelayMax()                  { return delayMax; }
    public void setDelayMax(int v)             { delayMax = Mth.clamp(v, delayMin, 500); saveConfig(); }

    // ── Module boilerplate ────────────────────────────────────────────────────

    @Override public boolean hasConfigurableSettings() { return true; }
    @Override public Screen createSettingsScreen(Screen parent) { return new ModuleSettingsScreen(parent, this); }

    @Override
    public void loadConfig(Properties p) {
        super.loadConfig(p);
        try { healthThreshold = Mth.clamp(Double.parseDouble(p.getProperty("autototem.threshold", "10.0")), 0.0, 20.0); } catch (Exception e) { Logger.warn("AutoTotem: Failed to parse threshold"); }
        alwaysEquip = Boolean.parseBoolean(p.getProperty("autototem.always", Boolean.toString(alwaysEquip)));
        try { delayMin = Mth.clamp(Integer.parseInt(p.getProperty("autototem.delayMin", "30")), 0, 500); } catch (Exception e) { Logger.warn("AutoTotem: Failed to parse delayMin"); }
        try { delayMax = Mth.clamp(Integer.parseInt(p.getProperty("autototem.delayMax", "60")), 0, 500); } catch (Exception e) { Logger.warn("AutoTotem: Failed to parse delayMax"); }
    }

    @Override
    public void saveConfig(Properties p) {
        super.saveConfig(p);
        p.setProperty("autototem.threshold", Double.toString(healthThreshold));
        p.setProperty("autototem.always",    Boolean.toString(alwaysEquip));
        p.setProperty("autototem.delayMin",  Integer.toString(delayMin));
        p.setProperty("autototem.delayMax",  Integer.toString(delayMax));
    }
}
