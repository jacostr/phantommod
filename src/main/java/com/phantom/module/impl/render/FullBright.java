/* Copyright (c) 2026 PhantomMod. All rights reserved. */
/*
 * FullBright.java — Maximum ambient light level for night vision (Player module).
 *
 * Saves the current gamma value on enable, sets it to 1.0 (maximum), and restores
 * the original value on disable. Re-applies gamma every tick in case the user opens
 * vanilla Options and the value gets reset.
 * Detectability: Safe — gamma is a client-only graphics option.
 */
package com.phantom.module.impl.render;

import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;

public class FullBright extends Module {

    private Double savedGamma;

    public FullBright() {
        // Moved to Player tab — it's a visual QoL feature, not combat automation.
        // No hotkey by default; assign one in settings if you toggle this often.
        super("FullBright",
                "Modifies ambient light level to maximum, letting you see perfectly in the dark.\nDetectability: Safe",
                ModuleCategory.RENDER,
                -1);
    }

    @Override
    public void onEnable() {
        applyGamma();
    }

    @Override
    public void onDisable() {
        restoreGamma();
    }

    @Override
    public void onTick() {
        if (!isEnabled() || mc.options == null) {
            return;
        }
        // Keep gamma applied if the user opened options and changed something mid-session.
        var opt = mc.options.gamma();
        if (savedGamma != null && opt.get() < 0.999) {
            opt.set(1.0);
        }
    }

    private void applyGamma() {
        if (mc.options == null) {
            return;
        }
        var opt = mc.options.gamma();
        if (savedGamma == null) {
            savedGamma = opt.get();
        }
        opt.set(1.0);
    }

    private void restoreGamma() {
        if (mc.options != null && savedGamma != null) {
            mc.options.gamma().set(savedGamma);
        }
        savedGamma = null;
    }
}
