package com.phantom.module.impl.player;

import com.phantom.PhantomMod;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Properties;

public class AntiBot extends Module {
    private final Map<UUID, Integer> firstSeenTicks = new HashMap<>();
    private boolean tabListCheck = true;
    private boolean uuidCheck = true;
    private boolean invisibleUnlistedCheck = true;
    private int joinGraceTicks = 20;

    public AntiBot() {
        super("AntiBot",
                "Filters out bots and NPCs from ESP and combat modules.\n" +
                        "Uses tab-list, UUID, invisibility, and fresh-join heuristics.\n" +
                        "Detectability: Safe",
                ModuleCategory.PLAYER, -1);
    }

    @Override
    public void onTick() {
        if (mc.level == null) {
            firstSeenTicks.clear();
            return;
        }

        for (Player player : mc.level.players()) {
            if (player == mc.player) {
                continue;
            }
            firstSeenTicks.putIfAbsent(player.getUUID(), player.tickCount);
        }

        Iterator<Map.Entry<UUID, Integer>> iterator = firstSeenTicks.entrySet().iterator();
        while (iterator.hasNext()) {
            UUID uuid = iterator.next().getKey();
            if (mc.level.getPlayerByUUID(uuid) == null) {
                iterator.remove();
            }
        }
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
        AntiBot module = PhantomMod.getModuleManager() == null
                ? null
                : PhantomMod.getModuleManager().getModuleByClass(AntiBot.class);
        if (module == null || !module.isEnabled()) {
            return false;
        }

        if (!(entity instanceof Player player))
            return false;

        // Never flag the local player themselves
        if (player == mc.player)
            return false;

        if (!player.isAlive() || player.isSpectator()) {
            return true;
        }

        boolean listedInTab = mc.getConnection() != null
                && mc.getConnection().getPlayerInfo(player.getUUID()) != null;

        if (module.joinGraceTicks > 0) {
            int firstSeenTick = module.firstSeenTicks.getOrDefault(player.getUUID(), player.tickCount);
            if (player.tickCount - firstSeenTick <= module.joinGraceTicks) {
                return true;
            }
        }

        // Check 1: not in the server's tab list
        if (module.tabListCheck && mc.getConnection() != null && !listedInTab) {
            return true;
        }

        // Check 2: UUID is not version 4 (not a real Mojang account UUID)
        UUID uuid = player.getUUID();
        if (module.uuidCheck && uuid.version() != 4) {
            return true;
        }

        if (module.invisibleUnlistedCheck && player.isInvisible() && !listedInTab) {
            return true;
        }

        return false;
    }

    public boolean isTabListCheck() {
        return tabListCheck;
    }

    public void setTabListCheck(boolean tabListCheck) {
        this.tabListCheck = tabListCheck;
        saveConfig();
    }

    public boolean isUuidCheck() {
        return uuidCheck;
    }

    public void setUuidCheck(boolean uuidCheck) {
        this.uuidCheck = uuidCheck;
        saveConfig();
    }

    public boolean isInvisibleUnlistedCheck() {
        return invisibleUnlistedCheck;
    }

    public void setInvisibleUnlistedCheck(boolean invisibleUnlistedCheck) {
        this.invisibleUnlistedCheck = invisibleUnlistedCheck;
        saveConfig();
    }

    public int getJoinGraceTicks() {
        return joinGraceTicks;
    }

    public void setJoinGraceTicks(int joinGraceTicks) {
        this.joinGraceTicks = Math.max(0, Math.min(200, joinGraceTicks));
        saveConfig();
    }

    @Override
    public void loadConfig(Properties properties) {
        super.loadConfig(properties);
        tabListCheck = Boolean.parseBoolean(properties.getProperty("antibot.tab_list", Boolean.toString(tabListCheck)));
        uuidCheck = Boolean.parseBoolean(properties.getProperty("antibot.uuid_check", Boolean.toString(uuidCheck)));
        invisibleUnlistedCheck = Boolean.parseBoolean(
                properties.getProperty("antibot.invisible_unlisted", Boolean.toString(invisibleUnlistedCheck)));
        String grace = properties.getProperty("antibot.join_grace_ticks");
        if (grace != null) {
            try {
                joinGraceTicks = Math.max(0, Math.min(200, Integer.parseInt(grace.trim())));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("antibot.tab_list", Boolean.toString(tabListCheck));
        properties.setProperty("antibot.uuid_check", Boolean.toString(uuidCheck));
        properties.setProperty("antibot.invisible_unlisted", Boolean.toString(invisibleUnlistedCheck));
        properties.setProperty("antibot.join_grace_ticks", Integer.toString(joinGraceTicks));
    }
}
