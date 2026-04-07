package com.phantom.module.impl.render;

import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.lwjgl.glfw.GLFW;

public class FullBright extends Module {

    // Refresh when this many ticks remain so there's no visible flicker between cycles.
    private static final int REFRESH_THRESHOLD = 40;
    // Duration to apply — long enough that refreshes are rare.
    private static final int EFFECT_DURATION = 800;

    public FullBright() {
        super("FullBright", "Applies hidden night vision so dark areas stay bright until you turn it off.", ModuleCategory.RENDER, GLFW.GLFW_KEY_B);
    }

    @Override
    public void onEnable() {
        // Apply immediately so the player sees the effect right away without waiting a tick.
        applyNightVision();
    }

    @Override
    public void onDisable() {
        if (mc.player == null) return;
        mc.player.removeEffect(MobEffects.NIGHT_VISION);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;

        // Only refresh when the effect is about to expire, not every single tick.
        MobEffectInstance current = mc.player.getEffect(MobEffects.NIGHT_VISION);
        if (current == null || current.getDuration() <= REFRESH_THRESHOLD) {
            applyNightVision();
        }
    }

    public boolean isActuallyActive() {
        return mc.player != null && mc.player.hasEffect(MobEffects.NIGHT_VISION);
    }

    private void applyNightVision() {
        if (mc.player == null) return;
        mc.player.addEffect(new MobEffectInstance(
                MobEffects.NIGHT_VISION,
                EFFECT_DURATION,
                0,
                false, // ambient — hides particles
                false, // showParticles
                false  // showIcon — keeps the HUD clean
        ));
    }
}