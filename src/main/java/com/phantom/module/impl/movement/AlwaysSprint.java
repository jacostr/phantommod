package com.phantom.module.impl.movement;

import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import org.lwjgl.glfw.GLFW;

public class AlwaysSprint extends Module {
    public AlwaysSprint() {
        super("AlwaysSprint", "Keep sprinting automatically.", ModuleCategory.MOVEMENT, GLFW.GLFW_KEY_V);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.player.input == null) return;

        // This is intentionally conservative so it behaves like a held sprint key.
        boolean canSprint =
                mc.player.input.hasForwardImpulse() &&
                !mc.player.horizontalCollision &&
                !mc.player.isShiftKeyDown() &&
                mc.player.getFoodData().getFoodLevel() > 6 &&
                !mc.player.isUsingItem() &&
                !mc.player.isInWater() &&
                !mc.player.isInLava() &&
                !mc.player.getAbilities().flying;

        mc.player.setSprinting(canSprint);
    }   // ← closes onTick()
}       // ← closes AlwaysSprint class
