/* Copyright (c) 2026 PhantomMod. All rights reserved. */
package com.phantom.module.impl.movement;

import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class SafeWalk extends Module {
    private static final double EDGE_CHECK_OFFSET = 0.32D;
    private boolean sneakingFromModule;

    public SafeWalk() {
        super("SafeWalk", 
              "Prevents you from walking off the edge of blocks.\nDetectability: Safe", 
              ModuleCategory.MOVEMENT, 
              -1);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null || mc.options == null) return;
        if (!mc.player.onGround()) {
            releaseSneak();
            return;
        }

        mc.options.keyShift.setDown(isAtEdge());
        sneakingFromModule = mc.options.keyShift.isDown();
    }

    @Override
    public void onDisable() {
        releaseSneak();
    }

    private boolean isAtEdge() {
        Vec3 movement = mc.player.getDeltaMovement();
        double moveX = movement.x;
        double moveZ = movement.z;

        if ((moveX * moveX + moveZ * moveZ) < 1.0E-4) {
            double yawRadians = Math.toRadians(mc.player.getYRot());
            moveX = -Math.sin(yawRadians);
            moveZ = Math.cos(yawRadians);
        }

        double length = Math.sqrt(moveX * moveX + moveZ * moveZ);
        if (length <= 0.0) {
            return false;
        }

        BlockPos edgeCheckPos = BlockPos.containing(
                mc.player.getX() + (moveX / length) * EDGE_CHECK_OFFSET,
                mc.player.getY() - 1.0D,
                mc.player.getZ() + (moveZ / length) * EDGE_CHECK_OFFSET
        );
        return mc.level.getBlockState(edgeCheckPos).isAir();
    }

    private void releaseSneak() {
        if (sneakingFromModule && mc.options != null) {
            mc.options.keyShift.setDown(false);
        }
        sneakingFromModule = false;
    }
}
