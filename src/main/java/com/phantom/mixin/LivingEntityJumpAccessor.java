package com.phantom.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityJumpAccessor {
    /** Mojang name in 1.21.11 (was incorrectly jumpCooldown — that field does not exist on LivingEntity). */
    @Accessor("noJumpDelay")
    void phantom$setNoJumpDelay(int ticks);
}
