/* Copyright (c) 2026 PhantomMod. All rights reserved. */
package com.phantom.module.impl.movement;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Properties;

/**
 * Clutch — Automatically prevents fall damage by placing items.
 *
 * Scans the hotbar for clutching resources (Water Buckets, Slime Blocks, Hay
 * Bales,
 * or Cobwebs) and automatically deploys them under your feet when falling from
 * a dangerous height.
 *
 * Detectability: Blatant
 */
public class Clutch extends Module {

    private boolean useWater = true;
    private boolean useSlime = true;
    private boolean useHay = true;
    private boolean useCobweb = true;
    private double triggerDistance = 4.0;

    /** Cooldown to avoid placing every tick after a clutch fires. */
    private int cooldownTicks = 0;
    private static final int COOLDOWN_DURATION = 20; // 1 second

    public Clutch() {
        super("Clutch", "Automatically prevents fall damage by placing blocks or water.\nDetectability: Blatant",
                ModuleCategory.MOVEMENT, -1);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null)
            return;

        // Tick down cooldown
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        // Only act when falling downward with meaningful speed
        double yVelocity = mc.player.getDeltaMovement().y;
        if (yVelocity >= -0.1)
            return;

        // Skip if already on the ground or in water
        if (mc.player.onGround())
            return;
        if (mc.player.isInWater() || mc.player.isInLava())
            return;

        // Check distance to the first solid block below
        double distanceToGround = getDistanceToGround();
        if (distanceToGround > triggerDistance)
            return;

        // Find a suitable clutch item in the hotbar
        int slot = findClutchSlot();
        if (slot == -1)
            return;

        // Save current slot, switch to clutch item
        int prevSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(slot);

        ItemStack held = mc.player.getMainHandItem();
        BlockPos below = BlockPos.containing(mc.player.position()).below();

        if (held.getItem() == Items.WATER_BUCKET) {
            // Right-click the block below to pour water
            BlockHitResult hit = new BlockHitResult(
                    Vec3.atCenterOf(below),
                    Direction.UP,
                    below,
                    false);
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        } else {
            // Place solid block (slime, hay bale, cobweb) at feet
            // Target the top face of the block below player's feet
            BlockHitResult hit = new BlockHitResult(
                    new Vec3(mc.player.getX(), mc.player.getY(), mc.player.getZ()),
                    Direction.UP,
                    below,
                    false);
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        }

        // Restore previous slot and apply cooldown
        mc.player.getInventory().setSelectedSlot(prevSlot);
        cooldownTicks = COOLDOWN_DURATION;
    }

    /**
     * Walks downward from the player's feet to find the nearest solid block.
     * Returns the block distance, or {@link Double#MAX_VALUE} if no ground found
     * (void).
     */
    private double getDistanceToGround() {
        BlockPos origin = BlockPos.containing(mc.player.position());
        int maxScan = (int) Math.ceil(triggerDistance) + 2;
        for (int i = 1; i <= maxScan; i++) {
            BlockPos check = origin.below(i);
            if (!mc.level.getBlockState(check).isAir()) {
                return i;
            }
        }
        return Double.MAX_VALUE;
    }

    /**
     * Scans hotbar slots 0–8 for a usable clutch item, respecting toggle settings.
     * Priority: Water Bucket → Slime Block → Hay Bale → Cobweb.
     *
     * @return hotbar slot index, or -1 if nothing usable was found.
     */
    private int findClutchSlot() {
        Inventory inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            Item item = inv.getItem(i).getItem();
            if (useWater && item == Items.WATER_BUCKET)
                return i;
            if (useSlime && item == Items.SLIME_BLOCK)
                return i;
            if (useHay && item == Items.HAY_BLOCK)
                return i;
            if (useCobweb && item == Items.COBWEB)
                return i;
        }
        return -1;
    }

    // -------------------------------------------------------------------------
    // Settings Getters & Setters
    // -------------------------------------------------------------------------

    public boolean isUseWater() {
        return useWater;
    }

    public void setUseWater(boolean useWater) {
        this.useWater = useWater;
        saveConfig();
    }

    public boolean isUseSlime() {
        return useSlime;
    }

    public void setUseSlime(boolean useSlime) {
        this.useSlime = useSlime;
        saveConfig();
    }

    public boolean isUseHay() {
        return useHay;
    }

    public void setUseHay(boolean useHay) {
        this.useHay = useHay;
        saveConfig();
    }

    public boolean isUseCobweb() {
        return useCobweb;
    }

    public void setUseCobweb(boolean useCobweb) {
        this.useCobweb = useCobweb;
        saveConfig();
    }

    public double getTriggerDistance() {
        return triggerDistance;
    }

    public void setTriggerDistance(double triggerDistance) {
        this.triggerDistance = Math.max(1.0, Math.min(20.0, triggerDistance));
        saveConfig();
    }

    // -------------------------------------------------------------------------
    // Module boilerplate
    // -------------------------------------------------------------------------

    @Override
    public boolean hasConfigurableSettings() {
        return true;
    }

    @Override
    public Screen createSettingsScreen(Screen parent) {
        return new ModuleSettingsScreen(parent, this);
    }

    @Override
    public void loadConfig(Properties props) {
        super.loadConfig(props);
        this.useWater = Boolean.parseBoolean(props.getProperty("clutch.use_water", "true"));
        this.useSlime = Boolean.parseBoolean(props.getProperty("clutch.use_slime", "true"));
        this.useHay = Boolean.parseBoolean(props.getProperty("clutch.use_hay", "true"));
        this.useCobweb = Boolean.parseBoolean(props.getProperty("clutch.use_cobweb", "true"));
        try {
            this.triggerDistance = Double.parseDouble(props.getProperty("clutch.trigger_distance", "4.0"));
        } catch (NumberFormatException ignored) {
        }
    }

    @Override
    public void saveConfig(Properties props) {
        super.saveConfig(props);
        props.setProperty("clutch.use_water", String.valueOf(useWater));
        props.setProperty("clutch.use_slime", String.valueOf(useSlime));
        props.setProperty("clutch.use_hay", String.valueOf(useHay));
        props.setProperty("clutch.use_cobweb", String.valueOf(useCobweb));
        props.setProperty("clutch.trigger_distance", String.valueOf(triggerDistance));
    }
}