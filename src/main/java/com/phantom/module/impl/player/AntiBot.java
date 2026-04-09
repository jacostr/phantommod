package com.phantom.module.impl.player;

import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AntiBot extends Module {
    private static final Set<UUID> botUuids = new HashSet<>();

    public AntiBot() {
        super("AntiBot", "Filters out Hypixel Watchdog bots and NPCs.\nDetectability: Safe", 
                ModuleCategory.PLAYER, -1);
    }

    @Override
    public void onTick() {
        if (mc.level == null) {
            botUuids.clear();
            return;
        }

        // Periodic cleanup or logic if needed. 
        // For now, we use the isBot method called from other modules.
    }

    public static boolean isBot(Entity entity) {
        if (!(entity instanceof Player player)) return false;
        
        // Hypixel Watchdog Bot Checks:
        // 1. Check if they are in the tab list (Watchdog bots usually aren't)
        if (mc.getConnection() != null && mc.getConnection().getPlayerInfo(player.getUUID()) == null) {
            return true;
        }

        // 2. Custom name check (Watchdog bots often have random strings or very specific patterns)
        String name = player.getName().getString();
        if (name.contains("Watchdog") || name.isEmpty()) {
            return true;
        }

        // 3. ID Check (Hypixel NPCs often have high entity IDs)
        if (player.getId() >= 1000000) {
            return true;
        }

        return false;
    }

    @Override
    public void onDisable() {
        botUuids.clear();
    }
}
