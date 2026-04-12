package com.phantom.module.impl.combat;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import com.phantom.util.InventoryUtil;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.Properties;

public class WaterClutch extends Module {

    private enum State {
        WAITING, SWAP_BUCKET, PLACE, RELEASE_PLACE, PICKUP, RELEASE_PICKUP, SWAP_BACK
    }

    private State currentState = State.WAITING;
    private int oldSlot = -1;
    private int waterSlot = -1;
    private int waitTicks = 0;

    // Camera
    private float originalPitch = 0f;
    private boolean restoringPitch = false;

    // Fire duration tracking
    private int fireTicks = 0;
    private static final int FIRE_DURATION_THRESHOLD = 30;

    // Settings
    private boolean extinguish = true;
    private boolean clutch = true;
    private int triggerHeight = 8; // blocks above ground to trigger
    private int pickupDelay = 4; // ticks between place and pickup

    public WaterClutch() {
        super("WaterClutch",
                "Automatically uses a water bucket to extinguish fire (after 1.5s) or save you from a fall.\nDetectability: Blatant",
                ModuleCategory.COMBAT, -1);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null || mc.gameMode == null)
            return;

        if (mc.player.isOnFire())
            fireTicks++;
        else
            fireTicks = 0;

        // Smooth camera restoration
        if (restoringPitch) {
            float current = mc.player.getXRot();
            float next = current + (originalPitch - current) * 0.4f;
            if (Math.abs(next - originalPitch) < 1.0f) {
                mc.player.setXRot(originalPitch);
                restoringPitch = false;
            } else {
                mc.player.setXRot(next);
            }
        }

        if (currentState == State.WAITING) {
            boolean triggerClutch = clutch && shouldClutch();
            boolean triggerExtinguish = extinguish && shouldExtinguish();
            if (triggerClutch || triggerExtinguish) {
                waterSlot = findWaterBucket();
                if (waterSlot != -1) {
                    oldSlot = InventoryUtil.getSelectedSlot();
                    originalPitch = mc.player.getXRot();
                    currentState = State.SWAP_BUCKET;
                }
            }
            return;
        }

        if (waitTicks > 0) {
            waitTicks--;
            if (currentState == State.PLACE) {
                float current = mc.player.getXRot();
                mc.player.setXRot(current + (90.0f - current) * 0.5f);
            }
            return;
        }

        switch (currentState) {
            case SWAP_BUCKET:
                InventoryUtil.setSelectedSlot(waterSlot);
                currentState = State.PLACE;
                // Wait longer the higher up we are so water lands closer to ground
                waitTicks = calcPlaceDelay();
                break;

            case PLACE:
                mc.player.setXRot(90.0f);
                ItemStack heldPlace = mc.player.getMainHandItem();
                if (heldPlace.getItem() == Items.WATER_BUCKET) {
                    mc.options.keyUse.setDown(true);
                    currentState = State.RELEASE_PLACE;
                    waitTicks = 1;
                } else {
                    currentState = State.SWAP_BACK;
                }
                break;

            case RELEASE_PLACE:
                mc.options.keyUse.setDown(false);
                currentState = State.PICKUP;
                waitTicks = pickupDelay;
                break;

            case PICKUP:
                ItemStack heldPickup = mc.player.getMainHandItem();
                if (heldPickup.getItem() == Items.BUCKET) {
                    mc.options.keyUse.setDown(true);
                    currentState = State.RELEASE_PICKUP;
                    waitTicks = 1;
                } else {
                    currentState = State.SWAP_BACK;
                }
                break;

            case RELEASE_PICKUP:
                mc.options.keyUse.setDown(false);
                currentState = State.SWAP_BACK;
                waitTicks = 1;
                break;

            case SWAP_BACK:
                if (oldSlot != -1)
                    InventoryUtil.setSelectedSlot(oldSlot);
                restoringPitch = true;
                reset();
                break;

            default:
                reset();
                break;
        }
    }

    /**
     * Waits longer the higher up the player is so the water
     * lands near the ground rather than too high up.
     * 1 tick per 2 blocks above ground, capped at 14 ticks.
     */
    private int calcPlaceDelay() {
        int blocksAbove = getBlocksAboveGround();
        return Math.min(14, Math.max(1, blocksAbove / 2));
    }

    private int getBlocksAboveGround() {
        BlockPos pos = mc.player.blockPosition();
        for (int i = 1; i <= 64; i++) {
            if (mc.level.getBlockState(pos.below(i)).isSolid())
                return i;
        }
        return 64;
    }

    private boolean shouldExtinguish() {
        return fireTicks >= FIRE_DURATION_THRESHOLD
                && mc.level.dimension() != Level.NETHER
                && !mc.player.hasEffect(MobEffects.FIRE_RESISTANCE);
    }

    private boolean shouldClutch() {
        if (mc.level.dimension() == Level.NETHER)
            return false;
        if (mc.player.isFallFlying())
            return false;
        if (mc.player.getDeltaMovement().y >= -0.1)
            return false;
        return getBlocksAboveGround() >= triggerHeight;
    }

    private int findWaterBucket() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == Items.WATER_BUCKET)
                return i;
        }
        return -1;
    }

    private void reset() {
        currentState = State.WAITING;
        oldSlot = -1;
        waterSlot = -1;
        waitTicks = 0;
    }

    @Override
    public void onDisable() {
        if (mc != null && mc.options != null)
            mc.options.keyUse.setDown(false);
        if (mc.player != null && (restoringPitch || currentState != State.WAITING))
            mc.player.setXRot(originalPitch);
        restoringPitch = false;
        fireTicks = 0;
        reset();
    }

    // Settings
    public boolean isExtinguish() {
        return extinguish;
    }

    public void setExtinguish(boolean v) {
        extinguish = v;
        saveConfig();
    }

    public boolean isClutch() {
        return clutch;
    }

    public void setClutch(boolean v) {
        clutch = v;
        saveConfig();
    }

    public int getTriggerHeight() {
        return triggerHeight;
    }

    public void setTriggerHeight(int v) {
        triggerHeight = Math.max(1, Math.min(64, v));
        saveConfig();
    }

    public int getPickupDelay() {
        return pickupDelay;
    }

    public void setPickupDelay(int v) {
        pickupDelay = Math.max(1, Math.min(20, v));
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
    public void loadConfig(Properties p) {
        super.loadConfig(p);
        extinguish = Boolean.parseBoolean(p.getProperty("waterclutch.extinguish", "true"));
        clutch = Boolean.parseBoolean(p.getProperty("waterclutch.clutch", "true"));
        try {
            triggerHeight = Integer.parseInt(p.getProperty("waterclutch.trigger_height", "8"));
        } catch (Exception ignored) {
        }
        try {
            pickupDelay = Integer.parseInt(p.getProperty("waterclutch.pickup_delay", "4"));
        } catch (Exception ignored) {
        }
    }

    @Override
    public void saveConfig(Properties p) {
        super.saveConfig(p);
        p.setProperty("waterclutch.extinguish", Boolean.toString(extinguish));
        p.setProperty("waterclutch.clutch", Boolean.toString(clutch));
        p.setProperty("waterclutch.trigger_height", Integer.toString(triggerHeight));
        p.setProperty("waterclutch.pickup_delay", Integer.toString(pickupDelay));
    }
}