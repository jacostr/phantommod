package com.phantom.mixin;

import com.phantom.PhantomMod;
import com.phantom.module.impl.render.FullBright;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(LightTexture.class)
public class LightTextureMixin {

    @ModifyConstant(
        method = "updateLightTexture",
        constant = @Constant(floatValue = 0.0f, ordinal = 0)
    )
    private float phantom$forceFullbright(float zeroConstant) {
        if (PhantomMod.getModuleManager() != null) {
            FullBright fb = PhantomMod.getModuleManager().getModuleByClass(FullBright.class);
            if (fb != null && fb.isEnabled()) {
                return 1.0f; // Force night vision brightness level instead of 0.0
            }
        }
        return zeroConstant;
    }
}
