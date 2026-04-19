/* Copyright (c) 2026 PhantomMod. All rights reserved. */
package com.phantom.module.impl.render;

import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.Minecraft;

public class FullBright extends Module {

    private double previousGamma = 0.5;

    public FullBright() {
        super("FullBright",
                "Removes darkness for full vision.\nDetectability: Safe",
                ModuleCategory.RENDER,
                -1);
    }

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) {
            // Save the player's current gamma before overriding
            previousGamma = mc.options.gamma().get();
            mc.options.gamma().set(16.0);
            mc.options.save();
        }
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) {
            // Restore the gamma to what it was before the module was enabled
            mc.options.gamma().set(previousGamma);
            mc.options.save();
        }
    }
}