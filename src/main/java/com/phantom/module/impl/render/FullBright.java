/* Copyright (c) 2026 PhantomMod. All rights reserved. */
package com.phantom.module.impl.render;

import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.Minecraft;

public class FullBright extends Module {

    public FullBright() {
        super("FullBright",
                "Removes darkness for full vision.\nDetectability: Safe",
                ModuleCategory.RENDER,
                -1);
    }

    @Override
    public void onEnable() {
        setGamma(100.0);
    }

    @Override
    public void onDisable() {
        setGamma(0.5);
    }

    private void setGamma(double value) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.options != null) {
                var gammaOption = mc.options.gamma();
                var field = gammaOption.getClass().getDeclaredField("value");
                field.setAccessible(true);
                field.set(gammaOption, value);
                mc.options.save();
            }
        } catch (Exception e) {
            // Fallback - try normal setter
            try {
                Minecraft mc = Minecraft.getInstance();
                if (mc.options != null) {
                    mc.options.gamma().set(Math.min(2.0, Math.max(0.0, value)));
                    mc.options.save();
                }
            } catch (Exception ignored) {}
        }
    }
}