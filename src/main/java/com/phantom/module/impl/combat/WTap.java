/* Copyright (c) 2026 PhantomMod. All rights reserved. */
/*
 * WTap.java — Briefly releases sprint after an attack to refresh sprint knockback.
 *
 * On eligible hits, the module forces a short unsprint window and then re-sprints.
 * Detectability: Moderate — repetitive timing patterns can still be profiled.
 */
package com.phantom.module.impl.combat;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.Entity;

import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

public class WTap extends Module {
    private double chance = 1.0D;
    private int triggerDelayMs = 70;
    private int releaseDelayMs = 50;
    private int repressDelayMs = 0;
    private int cooldownMs = 250;
    private boolean selectHits = true;
    private boolean playersOnly = true;

    private long scheduledTapAt = -1L;
    private long tapStartedAt = -1L;
    private long lastTapAt = -1L;

    public WTap() {
        super("WTap",
                "Briefly drops sprint after attacks, then re-applies it to refresh sprint knockback.\nDetectability: Moderate",
                ModuleCategory.MOVEMENT,
                -1);
    }

    public void onAttack(Entity target) {
        if (mc.player == null || target == null || !mc.player.isSprinting()) {
            return;
        }

        if (com.phantom.module.impl.player.AntiBot.isBot(target)) {
            return;
        }

        if (playersOnly && !(target instanceof net.minecraft.world.entity.player.Player)) {
            return;
        }

        if (selectHits && mc.player.getAttackStrengthScale(0.0F) < 0.9F) {
            return;
        }

        long now = System.currentTimeMillis();
        if (lastTapAt >= 0L && now - lastTapAt < cooldownMs) {
            return;
        }

        if (ThreadLocalRandom.current().nextDouble() > chance) {
            return;
        }

        scheduledTapAt = now + triggerDelayMs;
    }

    @Override
    public void onTick() {
        if (mc.player == null) {
            scheduledTapAt = -1L;
            tapStartedAt = -1L;
            return;
        }
        if (scheduledTapAt >= 0L && System.currentTimeMillis() >= scheduledTapAt) {
            tapStartedAt = System.currentTimeMillis();
            scheduledTapAt = -1L;
            lastTapAt = tapStartedAt;
        }

        if (tapStartedAt < 0L) {
            return;
        }

        if (shouldPauseForBedMining() || !mc.player.isAlive()) {
            scheduledTapAt = -1L;
            tapStartedAt = -1L;
            return;
        }

        long elapsed = System.currentTimeMillis() - tapStartedAt;
        if (elapsed < releaseDelayMs + repressDelayMs) {
            mc.player.setSprinting(false);
            return;
        }

        if (canResumeSprint()) {
            mc.player.setSprinting(true);
        }
        tapStartedAt = -1L;
    }

    @Override
    public void onDisable() {
        scheduledTapAt = -1L;
        tapStartedAt = -1L;
        lastTapAt = -1L;
    }

    public boolean isTapActive() {
        return scheduledTapAt >= 0L || tapStartedAt >= 0L;
    }

    private boolean canResumeSprint() {
        return mc.player.input != null
                && mc.player.input.hasForwardImpulse()
                && !mc.player.horizontalCollision
                && !mc.player.isShiftKeyDown()
                && mc.player.getFoodData().getFoodLevel() > 6
                && !mc.player.isUsingItem()
                && !mc.player.isInWater()
                && !mc.player.isInLava()
                && !mc.player.getAbilities().flying;
    }

    public double getChance() {
        return chance;
    }

    public void setChance(double chance) {
        this.chance = Math.max(0.0D, Math.min(1.0D, chance));
        saveConfig();
    }

    public int getReleaseDelayMs() {
        return releaseDelayMs;
    }

    public int getTriggerDelayMs() {
        return triggerDelayMs;
    }

    public void setTriggerDelayMs(int triggerDelayMs) {
        this.triggerDelayMs = Math.max(0, Math.min(250, triggerDelayMs));
        saveConfig();
    }

    public void setReleaseDelayMs(int releaseDelayMs) {
        this.releaseDelayMs = Math.max(0, Math.min(250, releaseDelayMs));
        saveConfig();
    }

    public int getRepressDelayMs() {
        return repressDelayMs;
    }

    public void setRepressDelayMs(int repressDelayMs) {
        this.repressDelayMs = Math.max(0, Math.min(250, repressDelayMs));
        saveConfig();
    }

    public int getCooldownMs() {
        return cooldownMs;
    }

    public void setCooldownMs(int cooldownMs) {
        this.cooldownMs = Math.max(0, Math.min(1000, cooldownMs));
        saveConfig();
    }

    public boolean isSelectHits() {
        return selectHits;
    }

    public void setSelectHits(boolean selectHits) {
        this.selectHits = selectHits;
        saveConfig();
    }

    public boolean isPlayersOnly() {
        return playersOnly;
    }

    public void setPlayersOnly(boolean playersOnly) {
        this.playersOnly = playersOnly;
        saveConfig();
    }

    public void applyPresetLegit() {
        setChance(0.35);
        setTriggerDelayMs(100);
        setReleaseDelayMs(75);
        setRepressDelayMs(15);
        setCooldownMs(325);
        setSelectHits(true);
        setPlayersOnly(true);
    }

    public void applyPresetNormal() {
        setChance(0.60);
        setTriggerDelayMs(75);
        setReleaseDelayMs(60);
        setRepressDelayMs(8);
        setCooldownMs(250);
        setSelectHits(true);
        setPlayersOnly(true);
    }

    public void applyPresetObvious() {
        setChance(0.80);
        setTriggerDelayMs(40);
        setReleaseDelayMs(45);
        setRepressDelayMs(0);
        setCooldownMs(175);
        setSelectHits(true);
        setPlayersOnly(true);
    }

    public void applyPresetBlatant() {
        setChance(1.0);
        setTriggerDelayMs(0);
        setReleaseDelayMs(20);
        setRepressDelayMs(0);
        setCooldownMs(100);
        setSelectHits(false);
        setPlayersOnly(false);
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
        String chanceValue = properties.getProperty("wtap.chance");
        if (chanceValue != null) {
            try {
                chance = Math.max(0.0D, Math.min(1.0D, Double.parseDouble(chanceValue.trim())));
            } catch (NumberFormatException ignored) {
            }
        }

        String releaseValue = properties.getProperty("wtap.release_delay_ms");
        if (releaseValue != null) {
            try {
                releaseDelayMs = Math.max(0, Math.min(250, Integer.parseInt(releaseValue.trim())));
            } catch (NumberFormatException ignored) {
            }
        }

        String repressValue = properties.getProperty("wtap.repress_delay_ms");
        if (repressValue != null) {
            try {
                repressDelayMs = Math.max(0, Math.min(250, Integer.parseInt(repressValue.trim())));
            } catch (NumberFormatException ignored) {
            }
        }

        String triggerValue = properties.getProperty("wtap.trigger_delay_ms");
        if (triggerValue != null) {
            try {
                triggerDelayMs = Math.max(0, Math.min(250, Integer.parseInt(triggerValue.trim())));
            } catch (NumberFormatException ignored) {
            }
        }

        String cooldownValue = properties.getProperty("wtap.cooldown_ms");
        if (cooldownValue != null) {
            try {
                cooldownMs = Math.max(0, Math.min(1000, Integer.parseInt(cooldownValue.trim())));
            } catch (NumberFormatException ignored) {
            }
        }

        selectHits = Boolean.parseBoolean(properties.getProperty("wtap.select_hits", Boolean.toString(selectHits)));
        playersOnly = Boolean.parseBoolean(properties.getProperty("wtap.players_only", Boolean.toString(playersOnly)));
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("wtap.chance", Double.toString(chance));
        properties.setProperty("wtap.trigger_delay_ms", Integer.toString(triggerDelayMs));
        properties.setProperty("wtap.release_delay_ms", Integer.toString(releaseDelayMs));
        properties.setProperty("wtap.repress_delay_ms", Integer.toString(repressDelayMs));
        properties.setProperty("wtap.cooldown_ms", Integer.toString(cooldownMs));
        properties.setProperty("wtap.select_hits", Boolean.toString(selectHits));
        properties.setProperty("wtap.players_only", Boolean.toString(playersOnly));
    }
}
