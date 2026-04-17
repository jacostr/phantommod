/* Copyright (c) 2026 PhantomMod. All rights reserved. */
/*
 * NoFall.java — Prevents fall damage by spoofing ground state (Player module).
 *
 * Sends ServerboundMovePlayerPacket with onGround=true when the player has
 * significant downward velocity, preventing the server from calculating fall damage.
 * Detectability: Blatant — servers compare ground state against expected trajectory.
 */
package com.phantom.module.impl.player;

import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public class NoFall extends Module {
    public NoFall() {
        super("NoFall", "Spoofs being on the ground to prevent fall damage.\nDetectability: Blatant", ModuleCategory.PLAYER, -1);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        
        // Reset fall distance on the server by claiming we are on the ground
        if (mc.player.fallDistance > 2.5F) {
            if (mc.player.connection != null) {
                // In this 1.21.11 environment, ServerboundMovePlayerPacket.PosRot
                // requires 7 arguments: x, y, z, yaw, pitch, onGround, horizontalCollision.
                mc.player.connection.send(new ServerboundMovePlayerPacket.PosRot(
                    mc.player.getX(), 
                    mc.player.getY(), 
                    mc.player.getZ(), 
                    mc.player.getYRot(), 
                    mc.player.getXRot(), 
                    true,
                    mc.player.horizontalCollision
                ));
            }
        }
    }
}
