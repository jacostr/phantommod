/* Copyright (c) 2026 PhantomMod. All rights reserved. */
package com.phantom.mixin;

import com.phantom.PhantomMod;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import com.phantom.mixin.ClientboundSetEntityMotionPacketAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(method = "handleEntityEvent", at = @At("HEAD"))
    private void onEntityEvent(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        com.phantom.PhantomMod.getModuleManager().onEntityEvent(packet);
    }

    @Inject(method = "handleSetEntityMotion", at = @At("HEAD"))
    private void onSetEntityMotion(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) {
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;
            if (packet.getId() != mc.player.getId()) return;

            com.phantom.module.impl.combat.Velocity velocityModule = com.phantom.PhantomMod.getModuleManager().getModuleByClass(com.phantom.module.impl.combat.Velocity.class);
            if (velocityModule != null && velocityModule.isEnabled() && velocityModule.shouldApplyVelocity()) {
                ClientboundSetEntityMotionPacketAccessor accessor = (ClientboundSetEntityMotionPacketAccessor) packet;
                net.minecraft.world.phys.Vec3 current = accessor.phantom$getMovement();
                
                if (current != null) {
                    net.minecraft.world.phys.Vec3 scaled = new net.minecraft.world.phys.Vec3(
                            current.x * velocityModule.getHorizontalPercent(),
                            current.y * velocityModule.getVerticalPercent(),
                            current.z * velocityModule.getHorizontalPercent()
                    );
                    accessor.phantom$setMovement(scaled);
                }
            }
        } catch (Exception e) {
            com.phantom.util.Logger.error("Velocity: Error processing motion packet", e);
        }
    }
}
