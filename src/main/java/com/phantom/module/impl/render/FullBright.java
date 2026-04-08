/*
 * Render QoL: raises client gamma while enabled; restores your previous brightness when disabled.
 */
package com.phantom.module.impl.render;

import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;

public class FullBright extends Module {

    private Double savedGamma;

    public FullBright() {
        super("FullBright",
                "Maxes brightness (gamma) while on. Turn off to restore your previous brightness setting.",
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
