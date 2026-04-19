/* Copyright (c) 2026 PhantomMod. All rights reserved. */
package com.phantom.module.impl.render;

import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import com.phantom.util.Logger;


import java.util.List;

public class FullBright extends Module {

    private static final double DEFAULT_GAMMA = 0.5;
    private static final double MAX_GAMMA = 1.0;
    private double savedGamma = DEFAULT_GAMMA;
    private boolean wasEnabled = false;

    public FullBright() {
        super("FullBright",
                "Removes darkness for full vision.\nDetectability: Safe",
                ModuleCategory.RENDER,
                -1);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;

        if (isEnabled()) {
            if (!wasEnabled) {
                wasEnabled = true;
                savedGamma = getGammaValue();
                setGammaValue(MAX_GAMMA);
            }
        } else {
            if (wasEnabled) {
                wasEnabled = false;
                setGammaValue(savedGamma > 0 ? savedGamma : DEFAULT_GAMMA);
            }
        }
    }

    @Override
    public void onDisable() {
        wasEnabled = false;
        setGammaValue(savedGamma > 0 ? savedGamma : DEFAULT_GAMMA);
    }

    private double getGammaValue() {
        try {
            return mc.options.gamma().get();
        } catch (Exception e) {
            Logger.error("FullBright: Failed to read gamma", e);
        }
        return DEFAULT_GAMMA;
    }

    private void setGammaValue(double value) {
        try {
            mc.options.gamma().set(value);
            mc.options.save();
        } catch (Exception e) {
            Logger.error("FullBright: Failed to set gamma to " + value, e);
        }
    }
}