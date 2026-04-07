package com.phantom.module.impl.movement;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import org.lwjgl.glfw.GLFW;

public class SpeedBridge extends Module {
    private static final double EDGE_CHECK_OFFSET = 0.32D;

    // Runtime state captured when the assist is enabled so the bridge direction
    // stays stable.
    private float moveYaw;
    private double startX;
    private double startZ;

    public SpeedBridge() {
        super(
                "Speedbridge Assist",
                "Face away from the void, hold S and your place button on the blocks ahead. Once you reach the edge, it will auto sneak to assist with legit-looking speed bridging.",
                ModuleCategory.MOVEMENT,
                GLFW.GLFW_KEY_X);
    }

    // Capture the starting direction and position so we can measure progress from
    // the
    // exact spot the player enabled the module.
    @Override
    public void onEnable() {
        if (mc.player == null) {
            setEnabled(false);
            return;
        }

        moveYaw = mc.player.getYRot();
        startX = mc.player.getX();
        startZ = mc.player.getZ();
    }

    // Always release the virtual sneak key when the assist turns off so normal
    // crouch
    // behavior is restored immediately.
    @Override
    public void onDisable() {
        releaseSneak();
    }

    // Main assist loop:
    // 1. Make sure the player is in a valid bridging state.
    // 2. Verify the manual movement input matches the selected preset.
    // 3. Start crouching only once the player has moved far enough and is hanging
    // over air.
    @Override
    public void onTick() {
        if (mc.level == null || mc.player == null || mc.options == null)
            return;

        if (!(mc.player.getMainHandItem().getItem() instanceof BlockItem)) {
            releaseSneak();
            return;
        }

        // This module is intentionally assist-only: the player keeps full camera
        // control and
        // still handles the actual movement and placing while the mod only helps with
        // sneak timing.
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

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    public Screen createSettingsScreen(Screen parent) {
        return new ModuleSettingsScreen(parent, this);
    }

    // Convert the stored facing angle into the travel direction this preset
    // expects.
    // Pure backwards is always allowed, and holding A or D adds a sideways
    // component so
    // diagonal manual bridging works without separate presets.
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

    // Sample the block just behind the player's feet in the current bridge
    // direction.
    // If that block is air, the player is hanging over the edge and should crouch.
    private boolean isOverEdge(double backwardX, double backwardZ) {
        BlockPos edgeCheckPos = BlockPos.containing(
                mc.player.getX() + backwardX * EDGE_CHECK_OFFSET,
                mc.player.getY() - 1.0D,
                mc.player.getZ() + backwardZ * EDGE_CHECK_OFFSET);
        return mc.level.getBlockState(edgeCheckPos).isAir();
    }

    // This module only owns crouch timing, so the whole assist effect is just
    // toggling
    // the sneak key based on the edge check.
    private void updateSneakState(boolean overEdge) {
        // Hold crouch while the player is hanging over the next air gap, and relax on
        // solid ground.
        mc.options.keyShift.setDown(overEdge);
    }

    // Accept normal backwards bridging plus either diagonal variant. The player
    // stays in
    // control of movement while the module only manages crouch timing.
    private boolean isExpectedBridgeInputHeld(TravelVector travelVector) {
        boolean holdingBack = mc.options.keyDown.isDown();
        boolean holdingLeft = !travelVector.holdLeft() || mc.options.keyLeft.isDown();
        boolean holdingRight = !travelVector.holdRight() || mc.options.keyRight.isDown();
        return holdingBack && holdingLeft && holdingRight;
    }

    // Helper for clean shutdown and for cases where the module temporarily decides
    // it
    // should not be sneaking.
    private void releaseSneak() {
        if (mc.options != null) {
            mc.options.keyShift.setDown(false);
        }
    }

    // Measure how far the player has travelled along the intended bridge line
    // rather than
    // using raw distance, which keeps diagonal presets accurate.
    private boolean hasBridgeStarted(TravelVector travelVector) {
        double deltaX = mc.player.getX() - startX;
        double deltaZ = mc.player.getZ() - startZ;
        double travelledBlocks = deltaX * travelVector.x() + deltaZ * travelVector.z();
        return travelledBlocks >= 0.0D;
    }

    // Small bundle describing the current expected movement direction and whether
    // the
    // player should also be holding a side key for the detected input style.
    private record TravelVector(double x, double z, boolean holdLeft, boolean holdRight) {
    }
}
