package com.phantom.mixin;

import com.phantom.module.impl.render.Health;
import com.phantom.module.impl.render.Nametags;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.client.renderer.entity.EntityRenderer.class)
public class EntityRendererMixin {

    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/Entity;D)Z", at = @At("HEAD"), cancellable = true)
    private void phantom$hideVanillaNametags(Entity entity, double distanceToCameraSq, CallbackInfoReturnable<Boolean> cir) {
        if (Nametags.shouldHideVanillaNametag(entity) || Health.shouldHideVanillaNametag(entity)) {
            cir.setReturnValue(false);
        }
    }
}
