/*
 * AlwaysSprint.java — Automatically keeps the player sprinting (Movement module).
 *
 * Sets mc.player.setSprinting(true) every tick when moving forward and sprint is
 * normally allowed (not too hungry, not in liquid, etc.). Eliminates the need to
 * double-tap or hold a sprint key.
 * Detectability: Safe — vanilla sprint behaviour, just automated.
 */
package com.phantom.module.impl.movement;

import com.phantom.PhantomMod;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import com.phantom.module.impl.combat.WTap;

public class AlwaysSprint extends Module {
    public AlwaysSprint() {
        super("AlwaysSprint",
                "Keeps player sprinting automatically when moving forward.\nDetectability: Safe/Subtle",
                ModuleCategory.MOVEMENT, -1);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.player.input == null)
            return;

        WTap wTap = PhantomMod.getModuleManager().getModuleByClass(WTap.class);
        if (wTap != null && wTap.isEnabled() && wTap.isTapActive()) {
            mc.player.setSprinting(false);
            return;
        }

        // This is intentionally conservative so it behaves like a held sprint key.
        boolean canSprint = mc.player.input.hasForwardImpulse() &&
                !mc.player.horizontalCollision &&
                !mc.player.isShiftKeyDown() &&
                mc.player.getFoodData().getFoodLevel() > 6 &&
                !mc.player.isUsingItem() &&
                !mc.player.isInWater() &&
                !mc.player.isInLava() &&
                !mc.player.getAbilities().flying;

        mc.player.setSprinting(canSprint);
    }
}
