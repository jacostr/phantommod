/* Copyright (c) 2026 PhantomMod. All rights reserved. */
package com.phantom.mixin;

import com.phantom.PhantomMod;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(method = "handleEntityEvent", at = @At("HEAD"))
    private void onEntityEvent(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        PhantomMod.getModuleManager().onEntityEvent(packet);
    }
}
