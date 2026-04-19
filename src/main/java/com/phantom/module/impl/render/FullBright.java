/* Copyright (c) 2026 PhantomMod. All rights reserved. */
package com.phantom.module.impl.render;

import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.Minecraft;

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
            Minecraft mc = Minecraft.getInstance();
            Object gammaOpt = mc.options.gamma();
            java.lang.reflect.Method get = gammaOpt.getClass().getMethod("get");
            Object val = get.invoke(gammaOpt);
            if (val instanceof Double) return (Double) val;
            if (val instanceof Integer) return ((Integer) val).doubleValue();
        } catch (Exception ignored) {}
        return DEFAULT_GAMMA;
    }

    private void setGammaValue(double value) {
        try {
            Minecraft mc = Minecraft.getInstance();
            Object gammaOpt = mc.options.gamma();
            java.lang.reflect.Method set = gammaOpt.getClass().getMethod("set", double.class);
            set.invoke(gammaOpt, value);
            mc.options.save();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}