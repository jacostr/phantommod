/*
 * MultiPlayerGameModeMixin.java — Hooks player attacks for the Criticals module.
 *
 * Injects at HEAD of MultiPlayerGameMode.attack(). Before the attack packet is sent,
 * fires 4 position packets that simulate a tiny vertical bounce (y+0.0625, y, y+0.0125, y).
 * The server interprets this as the player being airborne, registering the hit as a critical.
 * A randomised chance slider controls how often this fires to reduce anti-cheat pattern flags.
 */
package com.phantom.mixin;

import com.phantom.PhantomMod;
import com.phantom.module.impl.combat.BlockHit;
import com.phantom.module.impl.combat.Criticals;
import com.phantom.module.impl.combat.HitSelect;
import com.phantom.module.impl.combat.WTap;
import com.phantom.module.impl.render.ReachDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
    private final Random rand = new Random();

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onAttack(Player player, Entity target, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        HitSelect hitSelect = (HitSelect) PhantomMod.getModuleManager().getModuleByName("HitSelect");
        Criticals critModule = (Criticals) PhantomMod.getModuleManager().getModuleByName("Criticals");
        BlockHit blockHit = (BlockHit) PhantomMod.getModuleManager().getModuleByName("BlockHit");
        WTap wTapModule = (WTap) PhantomMod.getModuleManager().getModuleByName("WTap");
        ReachDisplay reachDisplay = (ReachDisplay) PhantomMod.getModuleManager().getModuleByName("Reach Display");

        if (hitSelect != null && hitSelect.isEnabled() && hitSelect.shouldCancelAttack(target)) {
            ci.cancel();
            return;
        }
        
        if (critModule != null && critModule.isEnabled() && mc.player != null && mc.player.onGround() && mc.player.fallDistance <= 0.0f) {
            // Apply randomized chance
            if (rand.nextDouble() < critModule.getChance()) {
                double x = mc.player.getX();
                double y = mc.player.getY();
                double z = mc.player.getZ();
                
                // Send standard NCP criticals packet sequence
                if (mc.getConnection() != null) {
                    mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(x, y + 0.0625101D, z, false, false));
                    mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(x, y, z, false, false));
                    mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(x, y + 0.0125D, z, false, false));
                    mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(x, y, z, false, false));
                }
            }
        }

        if (wTapModule != null && wTapModule.isEnabled()) {
            wTapModule.onAttack(target);
        }

        if (blockHit != null && blockHit.isEnabled()) {
            blockHit.onAttack(target);
        }

        if (reachDisplay != null && reachDisplay.isEnabled()) {
            reachDisplay.recordHit(target);
        }
    }
}
