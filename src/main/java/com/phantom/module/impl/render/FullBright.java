package com.phantom.module.impl.render;

import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.lwjgl.glfw.GLFW;

public class FullBright extends Module {
    public FullBright() {
        super("FullBright", "Maxes out brightness for visibility.", ModuleCategory.RENDER, GLFW.GLFW_KEY_B);
    }

    // Apply immediately on enable so the player does not need to wait for the next tick.
    @Override
    public void onEnable() {
        applyNightVision();
    }

    // Remove the effect cleanly when the module turns off.
    @Override
    public void onDisable() {
        if (mc.player != null) {
            mc.player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    // Refresh the effect each tick so the timer never visibly runs out while the module
    // stays enabled.
    @Override
    public void onTick() {
        applyNightVision();
    }

    // Modern versions clamp brightness settings pretty hard, so a hidden night vision
    // refresh is the most stable "fullbright" style for this client.
    private void applyNightVision() {
        if (mc.player != null) {
            // 1.21.11 clamps brightness, so a silent night vision refresh is more reliable.
            mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, false, false, false));
        }
    }
}
