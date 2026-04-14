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
        WAITING,
        SWAP_BUCKET, // switch hotbar to water bucket slot
        LOOK_DOWN, // smoothly rotate camera to 90 degrees, then start holding right-click
        WAIT_PLACED, // holding right-click — watch for bucket to become empty (water placed)
        WAIT_REFILLED, // still holding — watch for bucket to refill (water picked up)
        SWAP_BACK // release right-click, restore slot + pitch
    }

    private State currentState = State.WAITING;
    private int oldSlot = -1;
    private int waterSlot = -1;
    private int safetyTicks = 0; // counts up while waiting for refill; cancel at 30
    private int landedTicks = 0; // ticks with near-zero y velocity while sequence is active
    private int freefallTicks = 0; // consecutive ticks of genuine freefall before triggering

    // Camera
    private float originalPitch = 0f;
    private boolean restoringPitch = false;

    // Fire duration tracking
    private int fireTicks = 0;

    // Settings
    private boolean extinguish = true;
    private boolean clutch = true;
    private int triggerHeight = 8; // blocks above ground to trigger fall clutch
    private int fireDurationThreshold = 30; // ticks on fire before extinguishing (~1.5s)

    public WaterClutch() {
        super("WaterClutch",
                "Automatically uses a water bucket to extinguish fire or save you from a fall.\n" +
                        "Keep a water bucket anywhere in your hotbar. When triggered: smoothly looks\n" +
                        "straight down, holds right-click until water is placed, keeps holding until\n" +
                        "the bucket refills, then swaps back to your previous slot.\n" +
                        "Detectability: Blatant",
                ModuleCategory.COMBAT, -1);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null || mc.gameMode == null)
            return;

        // Track fire ticks
        if (mc.player.isOnFire())
            fireTicks++;
        else
            fireTicks = 0;

        // Smooth camera restoration after finishing
        if (restoringPitch) {
            float cur = mc.player.getXRot();
            float next = cur + (originalPitch - cur) * 0.4f;
            if (Math.abs(next - originalPitch) < 1.0f) {
                mc.player.setXRot(originalPitch);
                restoringPitch = false;
            } else {
                mc.player.setXRot(next);
            }
        }

        if (currentState == State.WAITING) {
            // Track how long we've been in genuine freefall (fast downward velocity, not on
            // ground)
            // -0.4 threshold: a normal jump's downward velocity only briefly dips to ~-0.35
            // at the very bottom of the arc, so this cleanly prevents jump-hops from
            // accumulating freefallTicks while still counting real falls immediately.
            boolean genuinelyFalling = !mc.player.onGround()
                    && !mc.player.isFallFlying()
                    && mc.player.getDeltaMovement().y < -0.4;
            if (genuinelyFalling)
                freefallTicks++;
            else
                freefallTicks = 0;

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

        // Safety: if we're active but the player has landed (y velocity near 0 for 20
        // ticks = 1s),
        // something went wrong — release control and reset
        if (currentState != State.WAITING) {
            if (Math.abs(mc.player.getDeltaMovement().y) < 0.08) {
                landedTicks++;
                if (landedTicks >= 20) {
                    mc.options.keyUse.setDown(false);
                    if (oldSlot != -1)
                        InventoryUtil.setSelectedSlot(oldSlot);
                    restoringPitch = true;
                    reset();
                    return;
                }
            } else {
                landedTicks = 0;
            }
        }

        switch (currentState) {

            case SWAP_BUCKET:
                InventoryUtil.setSelectedSlot(waterSlot);
                currentState = State.LOOK_DOWN;
                break;

            case LOOK_DOWN: {
                // Each tick lerp 50% of remaining angle toward 90 — smooth and fast
                float cur = mc.player.getXRot();
                float diff = 90.0f - cur;
                mc.player.setXRot(cur + diff * 0.5f);
                // Once within 10 degrees of straight down, snap and start holding right-click
                if (Math.abs(diff) <= 10.0f) {
                    mc.player.setXRot(90.0f);
                    mc.options.keyUse.setDown(true);
                    currentState = State.WAIT_PLACED;
                }
                break;
            }

            case WAIT_PLACED: {
                // Hold right-click, look straight down.
                // No timeout here — player could be falling from any height.
                // Watch for WATER_BUCKET -> BUCKET (bucket emptied = water placed).
                mc.player.setXRot(90.0f);
                ItemStack held = mc.player.getMainHandItem();
                if (held.getItem() == Items.BUCKET) {
                    safetyTicks = 0;
                    currentState = State.WAIT_REFILLED;
                }
                break;
            }

            case WAIT_REFILLED: {
                // Water is on the ground. Still holding right-click looking straight down.
                // Minecraft scoops it back up automatically.
                // Watch for BUCKET -> WATER_BUCKET (refilled = picked up).
                safetyTicks++;
                if (safetyTicks >= 30) {
                    // 1.5s timeout — give control back
                    mc.options.keyUse.setDown(false);
                    if (oldSlot != -1)
                        InventoryUtil.setSelectedSlot(oldSlot);
                    restoringPitch = true;
                    reset();
                    break;
                }
                ItemStack held = mc.player.getMainHandItem();
                if (held.getItem() == Items.WATER_BUCKET) {
                    currentState = State.SWAP_BACK;
                }
                break;
            }

            case SWAP_BACK:
                // Done — release right-click, restore previous slot and camera pitch
                mc.options.keyUse.setDown(false);
                if (oldSlot != -1)
                    InventoryUtil.setSelectedSlot(oldSlot);
                restoringPitch = true;
                reset();
                break;

            default:
                mc.options.keyUse.setDown(false);
                reset();
                break;
        }
    }

    /**
     * Exact distance (blocks, fractional) from the player's feet to the nearest
     * solid block surface below. Uses the raw Y coordinate rather than the floored
     * blockPosition so the reading is never off by up to 1 block.
     */
    private double getExactBlocksAboveGround() {
        double feetY = mc.player.getY();
        BlockPos pos = BlockPos.containing(mc.player.getX(), feetY - 0.01, mc.player.getZ());
        for (int i = 0; i <= 64; i++) {
            BlockPos check = pos.below(i);
            if (mc.level.getBlockState(check).isSolid()) {
                return feetY - (check.getY() + 1.0);
            }
        }
        return 64.0;
    }

    private boolean shouldExtinguish() {
        return fireTicks >= fireDurationThreshold
                && mc.level.dimension() != Level.NETHER
                && !mc.player.hasEffect(MobEffects.FIRE_RESISTANCE);
    }

    private boolean shouldClutch() {
        if (mc.level.dimension() == Level.NETHER)
            return false;
        if (mc.player.onGround())
            return false;
        if (mc.player.isFallFlying())
            return false;
        if (mc.player.isInWater() || mc.player.isInLava())
            return false;
        // Require real downward velocity — not a hop peak
        if (mc.player.getDeltaMovement().y >= -0.3)
            return false;
        // 3 ticks of confirmed freefall rules out 1-2 tick parkour gaps without
        // burning precious trigger window on longer falls.
        if (freefallTicks < 3)
            return false;

        // Lookahead: simulate player Y in 4 ticks (swap + look-down lerp + place lag)
        // and check if THAT position is within the trigger threshold. This means the
        // sequence fires early enough that water actually lands before the player does.
        double vy = mc.player.getDeltaMovement().y;
        double predictedY = mc.player.getY();
        for (int t = 0; t < 4; t++) {
            vy = (vy - 0.08) * 0.98; // Minecraft gravity + air drag
            predictedY += vy;
        }
        BlockPos predictedPos = BlockPos.containing(mc.player.getX(), predictedY - 0.01, mc.player.getZ());
        double predictedDist = 64.0;
        for (int i = 0; i <= 64; i++) {
            BlockPos check = predictedPos.below(i);
            if (mc.level.getBlockState(check).isSolid()) {
                predictedDist = predictedY - (check.getY() + 1.0);
                break;
            }
        }

        return predictedDist <= triggerHeight || getExactBlocksAboveGround() <= triggerHeight;
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
        safetyTicks = 0;
        freefallTicks = 0;
        landedTicks = 0;
    }

    @Override
    public void onDisable() {
        if (mc != null && mc.options != null)
            mc.options.keyUse.setDown(false);
        if (mc.player != null && (restoringPitch || currentState != State.WAITING))
            mc.player.setXRot(originalPitch);
        restoringPitch = false;
        fireTicks = 0;
        freefallTicks = 0;
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

    public int getFireDurationThreshold() {
        return fireDurationThreshold;
    }

    public void setFireDurationThreshold(int v) {
        fireDurationThreshold = Math.max(1, Math.min(100, v));
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
            fireDurationThreshold = Integer.parseInt(p.getProperty("waterclutch.fire_threshold", "30"));
        } catch (Exception ignored) {
        }
    }

    @Override
    public void saveConfig(Properties p) {
        super.saveConfig(p);
        p.setProperty("waterclutch.extinguish", Boolean.toString(extinguish));
        p.setProperty("waterclutch.clutch", Boolean.toString(clutch));
        p.setProperty("waterclutch.trigger_height", Integer.toString(triggerHeight));
        p.setProperty("waterclutch.fire_threshold", Integer.toString(fireDurationThreshold));
    }
}