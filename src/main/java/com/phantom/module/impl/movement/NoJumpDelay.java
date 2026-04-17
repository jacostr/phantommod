/* Copyright (c) 2026 PhantomMod. All rights reserved. */
/*
 * NoJumpDelay.java — Removes the vanilla 10-tick jump cooldown (Movement module).
 *
 * Uses LivingEntityJumpDelayMixin + LivingEntityJumpAccessor to zero out the private
 * noJumpDelay field every tick for the local player. Makes bunny-hopping feel instant.
 * Detectability: Subtle — detectable if the server measures jump frequency.
 */
package com.phantom.module.impl.movement;

import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;

public class NoJumpDelay extends Module {
    private static NoJumpDelay instance;

    public NoJumpDelay() {
        super("NoJumpDelay", "Removes the vanilla 10-tick delay between jumps when holding the jump key.\nDetectability: Subtle", ModuleCategory.MOVEMENT, -1);
        instance = this;
    }

    public static boolean isModuleEnabled() {
        return instance != null && instance.isEnabled();
    }
}
