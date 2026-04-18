/* Copyright (c) 2026 PhantomMod. All rights reserved. */
package com.phantom.module.impl.player;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.gui.screens.Screen;

import java.util.Properties;

/**
 * AutoGG — Automatically sends "gg" (or a custom message) after a game ends.
 * 
 * Detects common game-end strings in chat packets (Winner, Game Over, etc.)
 * and dispatches a chat message after a configurable delay.
 * Detectability: Safe — sending "gg" is very common behavior.
 */
public class AutoGG extends Module {
    private String message = "gg";
    private int delayMs = 1000;
    private long lastSentAt = 0;

    public AutoGG() {
        super("AutoGG", "Automatically sends 'gg' after a game ends.\nDetectability: Safe", ModuleCategory.PLAYER, -1);
    }

    @Override
    public void onChat(String chatMessage) {
        if (!isEnabled()) return;

        String msg = chatMessage.toLowerCase();
        if (isGameEndMessage(msg)) {
            // Rate limit to avoid double-sending in quickly terminating multi-round games
            long now = System.currentTimeMillis();
            if (now - lastSentAt < 5000) return; 

            lastSentAt = now;

            new Thread(() -> {
                try {
                    Thread.sleep(delayMs);
                    mc.execute(() -> {
                        if (mc.player != null && mc.player.connection != null) {
                            mc.player.connection.sendChat(message);
                        }
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    private boolean isGameEndMessage(String msg) {
        return msg.contains("winner!") || 
               msg.contains("1st place:") || 
               msg.contains("top players:") || 
               msg.contains("game over!") ||
               msg.contains("reward summary") ||
               msg.contains("you won!") ||
               msg.contains("victory!") ||
               msg.contains("game ending");
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
        saveConfig();
    }

    public int getDelayMs() {
        return delayMs;
    }

    public void setDelayMs(int delayMs) {
        this.delayMs = Math.max(0, Math.min(5000, delayMs));
        saveConfig();
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
    public void loadConfig(Properties properties) {
        super.loadConfig(properties);
        String savedMsg = properties.getProperty("autogg.message");
        if (savedMsg != null) {
            this.message = savedMsg;
        }
        String savedDelay = properties.getProperty("autogg.delay_ms");
        if (savedDelay != null) {
            try {
                this.delayMs = Integer.parseInt(savedDelay.trim());
            } catch (NumberFormatException ignored) {}
        }
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("autogg.message", message);
        properties.setProperty("autogg.delay_ms", Integer.toString(delayMs));
    }
}
