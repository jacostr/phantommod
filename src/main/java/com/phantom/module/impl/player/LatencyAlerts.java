package com.phantom.module.impl.player;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.gui.NotificationManager;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.Properties;

public class LatencyAlerts extends Module {
    private int highPingMs = 180;
    private int spikeIncreaseMs = 60;
    private int alertCooldownSeconds = 10;

    private long lastAlertAt;
    private int lastPing = -1;

    public LatencyAlerts() {
        super("LatencyAlerts",
                "Shows HUD notifications when your ping gets high or spikes sharply.\nDetectability: Safe",
                ModuleCategory.PLAYER, -1);
    }

    @Override
    public boolean hasConfigurableSettings() {
        return true;
    }

    @Override
    public Screen createSettingsScreen(Screen parent) {
        return new ModuleSettingsScreen(parent, this);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.getConnection() == null) {
            lastPing = -1;
            return;
        }

        PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
        if (info == null) {
            return;
        }

        int ping = Math.max(0, info.getLatency());
        boolean high = ping >= highPingMs;
        boolean spike = lastPing >= 0 && ping - lastPing >= spikeIncreaseMs;
        long now = System.currentTimeMillis();

        if ((high || spike) && now - lastAlertAt >= alertCooldownSeconds * 1000L) {
            NotificationManager.push("Ping " + ping + "ms");
            lastAlertAt = now;
        }

        lastPing = ping;
    }

    public int getHighPingMs() {
        return highPingMs;
    }

    public void setHighPingMs(int highPingMs) {
        this.highPingMs = Math.max(50, Math.min(1000, highPingMs));
        saveConfig();
    }

    public int getSpikeIncreaseMs() {
        return spikeIncreaseMs;
    }

    public void setSpikeIncreaseMs(int spikeIncreaseMs) {
        this.spikeIncreaseMs = Math.max(10, Math.min(500, spikeIncreaseMs));
        saveConfig();
    }

    public int getAlertCooldownSeconds() {
        return alertCooldownSeconds;
    }

    public void setAlertCooldownSeconds(int alertCooldownSeconds) {
        this.alertCooldownSeconds = Math.max(1, Math.min(60, alertCooldownSeconds));
        saveConfig();
    }

    @Override
    public void loadConfig(Properties properties) {
        super.loadConfig(properties);
        String highPingValue = properties.getProperty("latency_alerts.high_ping_ms");
        if (highPingValue != null) {
            try {
                highPingMs = Math.max(50, Math.min(1000, Integer.parseInt(highPingValue.trim())));
            } catch (NumberFormatException ignored) {
            }
        }
        String spikeValue = properties.getProperty("latency_alerts.spike_increase_ms");
        if (spikeValue != null) {
            try {
                spikeIncreaseMs = Math.max(10, Math.min(500, Integer.parseInt(spikeValue.trim())));
            } catch (NumberFormatException ignored) {
            }
        }
        String cooldownValue = properties.getProperty("latency_alerts.cooldown_seconds");
        if (cooldownValue != null) {
            try {
                alertCooldownSeconds = Math.max(1, Math.min(60, Integer.parseInt(cooldownValue.trim())));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("latency_alerts.high_ping_ms", Integer.toString(highPingMs));
        properties.setProperty("latency_alerts.spike_increase_ms", Integer.toString(spikeIncreaseMs));
        properties.setProperty("latency_alerts.cooldown_seconds", Integer.toString(alertCooldownSeconds));
    }
}
