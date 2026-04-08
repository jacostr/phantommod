/*
 * LivingEntityJumpDelayMixin.java — Removes the vanilla jump cooldown for NoJumpDelay.
 *
 * Injects at the TAIL of LivingEntity.tick(). If the NoJumpDelay module is enabled and
 * the entity is the local player, sets noJumpDelay to 0 via the LivingEntityJumpAccessor.
 * This makes bunny-hopping feel instant instead of waiting 10 ticks between jumps.
 */
package com.phantom.mixin;

import com.phantom.module.impl.movement.NoJumpDelay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityJumpDelayMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void phantom$clearJumpCooldown(CallbackInfo ci) {
        if (!NoJumpDelay.isModuleEnabled()) {
            return;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof LocalPlayer)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != self) {
            return;
        }
        ((LivingEntityJumpAccessor) self).phantom$setNoJumpDelay(0);
    }
}
