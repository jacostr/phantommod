package com.phantom.module.impl.player;

import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class AntiBot extends Module {

    public AntiBot() {
        super("AntiBot",
                "Filters out bots and NPCs from ESP and combat modules.\n" +
                        "Uses tab-list presence and UUID version checks.\n" +
                        "Detectability: Safe",
                ModuleCategory.PLAYER, -1);
    }

    /**
     * Returns true if the entity is considered a bot or NPC and should be
     * ignored by targeting modules (ESP, KillAura, AimAssist, etc.).
     *
     * Two checks, in order of reliability:
     *
     * 1. Tab-list check — on Hypixel (and most servers), every real connected
     * player has a PlayerInfo entry. Bots/NPCs injected server-side almost
     * never do. This alone catches the vast majority of cases.
     *
     * 2. UUID version check — real Minecraft accounts use version-4 (random)
     * UUIDs. Hypixel NPCs and many bot frameworks use version-2 or version-3
     * UUIDs. Any non-v4 UUID is a strong signal the entity is not a real player.
     */
    public static boolean isBot(Entity entity) {
        if (!(entity instanceof Player player))
            return false;

        // Never flag the local player themselves
        if (player == mc.player)
            return false;

        // Check 1: not in the server's tab list
        if (mc.getConnection() != null
                && mc.getConnection().getPlayerInfo(player.getUUID()) == null) {
            return true;
        }

        // Check 2: UUID is not version 4 (not a real Mojang account UUID)
        UUID uuid = player.getUUID();
        if (uuid.version() != 4) {
            return true;
        }

        return false;
    }
}