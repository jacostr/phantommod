/* Copyright (c) 2026 PhantomMod. All rights reserved. */
package com.phantom.mixin;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundSetEntityMotionPacket.class)
public interface ClientboundSetEntityMotionPacketAccessor {
    @Accessor("movement")
    Vec3 phantom$getMovement();

    @Accessor("movement")
    void phantom$setMovement(Vec3 movement);
}
