/*
 * SpeedBridge.java — Edge-detection bridging assist with auto block refill (Movement module).
 *
 * Captures bridge direction on enable. Each tick, checks if the player is hanging over
 * air and auto-sneaks to prevent falling. When the current block stack is depleted,
 * scans the hotbar for the next BlockItem and auto-swaps to it.
 * Detectability: Safe/Subtle — only sneak timing is automated; placement is manual.
 */
package com.phantom.module.impl.movement;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public class SpeedBridge extends Module {
    private static final double EDGE_CHECK_OFFSET = 0.32D;

    private float moveYaw;
    private double startX;
    private double startZ;

    private long lastPlaceTime;
    private double autoOffDelay = 3.0;

    public SpeedBridge() {
        super(
                "SpeedBridge Assist",
                "Re-sneaks at the edge of blocks automatically. Face away from the void, hold right click and place buttons, then hold s, it will sneak for you.\nDetectability: Safe/Subtle",
                ModuleCategory.MOVEMENT,
                -1);
    }

    @Override
    public void onEnable() {
        if (mc.player == null) {
            setEnabled(false);
            return;
        }

        moveYaw = mc.player.getYRot();
        startX = mc.player.getX();
        startZ = mc.player.getZ();
        lastPlaceTime = System.currentTimeMillis();
    }

    @Override
    public void onDisable() {
        releaseSneak();
    }

    @Override
    public void onTick() {
        if (mc.level == null || mc.player == null || mc.options == null)
            return;

        if (!(mc.player.getMainHandItem().getItem() instanceof BlockItem)) {
            int nextSlot = findNextBlockSlot();
            if (nextSlot != -1) {
                mc.player.getInventory().setSelectedSlot(nextSlot);
            } else {
                releaseSneak();
                checkAutoDisable();
                return;
            }
        }

        if (mc.player.getMainHandItem().isEmpty()) {
            int nextSlot = findNextBlockSlot();
            if (nextSlot != -1) {
                mc.player.getInventory().setSelectedSlot(nextSlot);
            } else {
                releaseSneak();
                checkAutoDisable();
                return;
            }
        }

        if (mc.options.keyUse.isDown()) {
            lastPlaceTime = System.currentTimeMillis();
        }

        if (checkAutoDisable()) return;

        TravelVector travelVector = getTravelVector();
        if (!isExpectedBridgeInputHeld(travelVector)) {
            updateSneakState(false);
            return;
        }

        if (!mc.options.keyUse.isDown()) {
            updateSneakState(false);
            return;
        }

        boolean bridgeStarted = hasBridgeStarted(travelVector);
        boolean overEdge = isOverEdge(travelVector.x(), travelVector.z());
        updateSneakState(bridgeStarted && overEdge);
    }

    private boolean checkAutoDisable() {
        if (System.currentTimeMillis() - lastPlaceTime > autoOffDelay * 1000) {
            setEnabled(false);
            return true;
        }
        return false;
    }

    public double getAutoOffDelay() {
        return autoOffDelay;
    }

    public void setAutoOffDelay(double autoOffDelay) {
        this.autoOffDelay = Math.max(0.5, Math.min(10.0, autoOffDelay));
        saveConfig();
    }

    @Override
    public void loadConfig(java.util.Properties properties) {
        super.loadConfig(properties);
        String v = properties.getProperty("speedbridge.auto_off_delay");
        if (v != null) {
            try {
                autoOffDelay = Double.parseDouble(v);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void saveConfig(java.util.Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("speedbridge.auto_off_delay", Double.toString(autoOffDelay));
    }

    /**
     * Scans hotbar slots (0-8) for the first slot that has a block item with at
     * least 1 count. Returns -1 if no blocks are found.
     */
    private int findNextBlockSlot() {
        if (mc.player == null) return -1;
        int current = mc.player.getInventory().getSelectedSlot();
        for (int offset = 1; offset <= 9; offset++) {
            int slot = (current + offset) % 9;
            ItemStack stack = mc.player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
                return slot;
            }
        }
        return -1;
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    public Screen createSettingsScreen(Screen parent) {
        return new ModuleSettingsScreen(parent, this);
    }

    private TravelVector getTravelVector() {
        double backwardYawRadians = Math.toRadians(moveYaw);
        double backwardX = Math.sin(backwardYawRadians);
        double backwardZ = -Math.cos(backwardYawRadians);

        double rightYawRadians = Math.toRadians(moveYaw + 90.0F);
        double rightX = Math.sin(rightYawRadians);
        double rightZ = -Math.cos(rightYawRadians);

        double moveX = backwardX;
        double moveZ = backwardZ;
        boolean holdLeft = mc.options.keyLeft.isDown();
        boolean holdRight = mc.options.keyRight.isDown();

        if (holdLeft && !holdRight) {
            moveX -= rightX;
            moveZ -= rightZ;
        } else if (holdRight && !holdLeft) {
            moveX += rightX;
            moveZ += rightZ;
        }

        double length = Math.sqrt(moveX * moveX + moveZ * moveZ);
        if (length == 0.0D) {
            return new TravelVector(0.0D, 0.0D, false, false);
        }

        return new TravelVector(moveX / length, moveZ / length, holdLeft, holdRight);
    }

    private boolean isOverEdge(double backwardX, double backwardZ) {
        BlockPos edgeCheckPos = BlockPos.containing(
                mc.player.getX() + backwardX * EDGE_CHECK_OFFSET,
                mc.player.getY() - 1.0D,
                mc.player.getZ() + backwardZ * EDGE_CHECK_OFFSET);
        return mc.level.getBlockState(edgeCheckPos).isAir();
    }

    private void updateSneakState(boolean overEdge) {
        mc.options.keyShift.setDown(overEdge);
    }

    private boolean isExpectedBridgeInputHeld(TravelVector travelVector) {
        boolean holdingBack = mc.options.keyDown.isDown();
        boolean holdingLeft = !travelVector.holdLeft() || mc.options.keyLeft.isDown();
        boolean holdingRight = !travelVector.holdRight() || mc.options.keyRight.isDown();
        return holdingBack && holdingLeft && holdingRight;
    }

    private void releaseSneak() {
        if (mc.options != null) {
            mc.options.keyShift.setDown(false);
        }
    }

    private boolean hasBridgeStarted(TravelVector travelVector) {
        double deltaX = mc.player.getX() - startX;
        double deltaZ = mc.player.getZ() - startZ;
        double travelledBlocks = deltaX * travelVector.x() + deltaZ * travelVector.z();
        return travelledBlocks >= 0.0D;
    }

    private record TravelVector(double x, double z, boolean holdLeft, boolean holdRight) {
    }
}
