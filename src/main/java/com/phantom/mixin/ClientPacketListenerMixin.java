/*
 * ClientPacketListenerMixin.java — Hooks the server knockback packet for the Velocity module.
 *
 * Injects at RETURN of handleSetEntityMotion so vanilla applies the full knockback first,
 * then we scale it down by the user's configured percentage. This avoids detection because
 * the client does still move — just less than the server expected.
 */
package com.phantom.mixin;

import com.phantom.PhantomMod;
import com.phantom.module.impl.combat.Velocity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts the server's knockback packet to allow the Velocity module to
 * scale down how much knockback the player actually receives.
 *
 * <h3>Why inject at RETURN instead of HEAD?</h3>
 * <p>If we cancelled the packet at HEAD, the server would have sent velocity that the client
 * never applied — servers (and anti-cheat) can detect this via movement prediction: they expect
 * the client to be displaced by the knockback, and if the client stays stationary, it flags as
 * "velocity hack". By injecting at RETURN, vanilla code runs first and applies the full knockback
 * to {@code player.deltaMovement}. We then scale that vector proportionally. The client does
 * move — just less than the server expected — which is indistinguishable from normal ping variance
 * or block collision.</p>
 *
 * <h3>Why not modify the packet fields?</h3>
 * <p>{@code ClientboundSetEntityMotionPacket} fields are final/private in 1.21.1 MojMap.
 * Reading and re-applying via {@code getDeltaMovement()} / {@code setDeltaMovement()} is cleaner
 * and doesn't require an accessor mixin.</p>
 */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(method = "handleSetEntityMotion", at = @At("RETURN"))
    private void onHandleSetEntityMotion(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        // Only affect the local player's own knockback packets, not other entities.
        if (mc.player != null && packet.getId() == mc.player.getId()) {
            Velocity velocityModule = (Velocity) PhantomMod.getModuleManager().getModuleByName("Velocity");
            if (velocityModule != null && velocityModule.isEnabled() && velocityModule.shouldApplyVelocity()) {
                double horizontal = velocityModule.getHorizontalPercent();
                double vertical = velocityModule.getVerticalPercent();

                if (velocityModule.isHypixelMode() && !mc.player.onGround()) {
                    // Hypixel air-KB bypass: Cancel/Reduce horizontal motion while airborne.
                    // Keep the vertical (Y) motion to look more natural.
                    net.minecraft.world.phys.Vec3 currentMotion = mc.player.getDeltaMovement();
                    mc.player.setDeltaMovement(
                            currentMotion.x() * Math.min(horizontal, 0.2),
                            currentMotion.y() * vertical,
                            currentMotion.z() * Math.min(horizontal, 0.2)
                    );
                } else {
                    // Scale all three axes uniformly for non-airborne or standard mode.
                    net.minecraft.world.phys.Vec3 currentMotion = mc.player.getDeltaMovement();
                    mc.player.setDeltaMovement(
                            currentMotion.x() * horizontal,
                            currentMotion.y() * vertical,
                            currentMotion.z() * horizontal
                    );
                }
            }
        }
    }
}
