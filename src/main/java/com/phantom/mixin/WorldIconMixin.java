package com.phantom.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to fix a common vanilla bug/error where the game crashes or logs errors 
 * if a world icon is not exactly 64x64. This prevents the "IllegalArgumentException: 
 * Icon must be 64x64" from appearing in the logs and potentially breaking the world list.
 */
import com.mojang.blaze3d.platform.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.screen.world.WorldIcon")
public class WorldIconMixin {

    @Inject(
        method = {"verifyIcon", "method_52199"},
        at = @At("HEAD"),
        cancellable = true,
        remap = false,
        require = 0
    )
    private static void phantom$bypassIconCheck(CallbackInfo ci) {
        ci.cancel();
    }
}
