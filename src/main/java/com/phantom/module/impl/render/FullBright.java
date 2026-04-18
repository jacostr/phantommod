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

    // Logic moved to LightTextureMixin to avoid vanilla option validation errors.
    
    public FullBright() {
        super("FullBright",
                "Modifies ambient light level to maximum, letting you see perfectly in the dark.\nDetectability: Safe",
                ModuleCategory.RENDER,
                -1);
    }

    @Override
    public void onEnable() {
        if (mc.levelRenderer != null) {
            mc.levelRenderer.allChanged();
        }
    }

    @Override
    public void onDisable() {
        if (mc.levelRenderer != null) {
            mc.levelRenderer.allChanged();
        }
    }
}
