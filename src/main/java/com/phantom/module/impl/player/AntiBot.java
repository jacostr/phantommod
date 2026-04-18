/* Copyright (c) 2026 PhantomMod. All rights reserved. */
package com.phantom.module.impl.player;

import com.phantom.PhantomMod;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Properties;

public class AntiBot extends Module {
    private final Map<UUID, Integer> firstSeenTicks = new HashMap<>();
    private final Map<UUID, Long> lastPlayerInfoUpdate = new HashMap<>();

    private boolean tabListCheck = true;
    private boolean uuidCheck = false;
    private boolean invisibleUnlistedCheck = true;
    private boolean nameLengthCheck = true;
    private boolean pingCheck = false;
    private int joinGraceTicks = 20;
    private int minNameLength = 2;
    private int maxNameLength = 16;

    public AntiBot() {
        super("AntiBot",
                "Filters out bots and NPCs from ESP and combat modules.\n" +
                        "Uses tab-list, UUID, invisibility, name length, ping, and gamemode heuristics.\n" +
                        "Detectability: Safe",
                ModuleCategory.PLAYER, -1);
    }

    @Override
    public void onTick() {
        if (mc.level == null) {
            firstSeenTicks.clear();
            lastPlayerInfoUpdate.clear();
            return;
        }

        for (Player player : mc.level.players()) {
            if (player == mc.player) continue;
            firstSeenTicks.putIfAbsent(player.getUUID(), player.tickCount);

            PlayerInfo info = mc.getConnection() != null ? mc.getConnection().getPlayerInfo(player.getUUID()) : null;
            if (info != null) {
                lastPlayerInfoUpdate.put(player.getUUID(), System.currentTimeMillis());
            }
        }

        Iterator<Map.Entry<UUID, Integer>> iterator = firstSeenTicks.entrySet().iterator();
        while (iterator.hasNext()) {
            UUID uuid = iterator.next().getKey();
            if (mc.level.getPlayerByUUID(uuid) == null) {
                iterator.remove();
                lastPlayerInfoUpdate.remove(uuid);
            }
        }
    }

    public static boolean isBot(Entity entity) {
        AntiBot module = PhantomMod.getModuleManager() == null
                ? null
                : PhantomMod.getModuleManager().getModuleByClass(AntiBot.class);
        if (module == null || !module.isEnabled()) {
            return false;
        }

        if (!(entity instanceof Player player))
            return false;

        if (player == mc.player)
            return false;

        if (!player.isAlive() || player.isSpectator()) {
            return true;
        }

        if (module.joinGraceTicks > 0) {
            int firstSeenTick = module.firstSeenTicks.getOrDefault(player.getUUID(), player.tickCount);
            if (player.tickCount - firstSeenTick <= module.joinGraceTicks) {
                return true;
            }
        }

        boolean listedInTab = mc.getConnection() != null
                && mc.getConnection().getPlayerInfo(player.getUUID()) != null;

        if (module.tabListCheck && !listedInTab) {
            return true;
        }

        UUID uuid = player.getUUID();
        if (module.uuidCheck && uuid.version() != 4) {
            return true;
        }

        if (module.invisibleUnlistedCheck && player.isInvisible() && !listedInTab) {
            return true;
        }

        if (module.nameLengthCheck) {
            Component displayName = player.getDisplayName();
            String name = displayName != null ? displayName.getString() : "";
            if (name.length() < module.minNameLength || name.length() > module.maxNameLength) {
                return true;
            }
        }

        if (module.pingCheck && listedInTab) {
            PlayerInfo info = mc.getConnection().getPlayerInfo(player.getUUID());
            if (info != null && info.getLatency() == 0) {
                return true;
            }
        }

        return false;
    }

    public boolean isTabListCheck() { return tabListCheck; }
    public void setTabListCheck(boolean v) { tabListCheck = v; saveConfig(); }
    public boolean isUuidCheck() { return uuidCheck; }
    public void setUuidCheck(boolean v) { uuidCheck = v; saveConfig(); }
    public boolean isInvisibleUnlistedCheck() { return invisibleUnlistedCheck; }
    public void setInvisibleUnlistedCheck(boolean v) { invisibleUnlistedCheck = v; saveConfig(); }
    public boolean isNameLengthCheck() { return nameLengthCheck; }
    public void setNameLengthCheck(boolean v) { nameLengthCheck = v; saveConfig(); }
    public boolean isPingCheck() { return pingCheck; }
    public void setPingCheck(boolean v) { pingCheck = v; saveConfig(); }
    public int getJoinGraceTicks() { return joinGraceTicks; }
    public void setJoinGraceTicks(int v) { joinGraceTicks = Math.max(0, Math.min(200, v)); saveConfig(); }

    @Override
    public void loadConfig(Properties properties) {
        super.loadConfig(properties);
        tabListCheck = Boolean.parseBoolean(properties.getProperty("antibot.tab_list", Boolean.toString(tabListCheck)));
        uuidCheck = Boolean.parseBoolean(properties.getProperty("antibot.uuid_check", Boolean.toString(uuidCheck)));
        invisibleUnlistedCheck = Boolean.parseBoolean(properties.getProperty("antibot.invisible_unlisted", Boolean.toString(invisibleUnlistedCheck)));
        nameLengthCheck = Boolean.parseBoolean(properties.getProperty("antibot.name_length", Boolean.toString(nameLengthCheck)));
        pingCheck = Boolean.parseBoolean(properties.getProperty("antibot.ping_check", Boolean.toString(pingCheck)));
        try { joinGraceTicks = Integer.parseInt(properties.getProperty("antibot.join_grace_ticks", "20")); } catch (Exception ignored) {}
        try { minNameLength = Integer.parseInt(properties.getProperty("antibot.min_name_length", "2")); } catch (Exception ignored) {}
        try { maxNameLength = Integer.parseInt(properties.getProperty("antibot.max_name_length", "16")); } catch (Exception ignored) {}
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("antibot.tab_list", Boolean.toString(tabListCheck));
        properties.setProperty("antibot.uuid_check", Boolean.toString(uuidCheck));
        properties.setProperty("antibot.invisible_unlisted", Boolean.toString(invisibleUnlistedCheck));
        properties.setProperty("antibot.name_length", Boolean.toString(nameLengthCheck));
        properties.setProperty("antibot.ping_check", Boolean.toString(pingCheck));
        properties.setProperty("antibot.join_grace_ticks", Integer.toString(joinGraceTicks));
        properties.setProperty("antibot.min_name_length", Integer.toString(minNameLength));
        properties.setProperty("antibot.max_name_length", Integer.toString(maxNameLength));
    }
}