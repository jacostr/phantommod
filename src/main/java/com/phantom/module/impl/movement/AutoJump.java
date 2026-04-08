/*
 * Ghost movement assist: auto-jump when you have forward input on the ground (not while sneaking).
 */
package com.phantom.module.impl.movement;

import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.gui.screens.Screen;
import com.phantom.gui.ModuleSettingsScreen;

public class AutoJump extends Module {

    public AutoJump() {
        super("AutoJump", "Automatically jumps while you are moving.", ModuleCategory.GHOST, -1);
    }

    @Override
    public void onTick() {
        if (mc.player == null || !mc.player.onGround() || mc.player.isShiftKeyDown()) return;

        if (mc.player.input.hasForwardImpulse()) {
            mc.player.jumpFromGround();
        }
    }

    @Override
    public boolean hasConfigurableSettings() {
        return false;
    }

    @Override
    public Screen createSettingsScreen(Screen parent) {
        return new ModuleSettingsScreen(parent, this);
    }
}
