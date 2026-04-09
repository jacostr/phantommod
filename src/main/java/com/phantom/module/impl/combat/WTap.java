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
    private int releaseDelayMs = 50;
    private int repressDelayMs = 0;
    private boolean selectHits = true;

    private long tapStartedAt = -1L;

    public WTap() {
        super("WTap",
                "Briefly drops sprint after attacks, then re-applies it to refresh sprint knockback.\nDetectability: Moderate",
                ModuleCategory.COMBAT,
                -1);
    }

    public void onAttack(Entity target) {
        if (mc.player == null || target == null || !mc.player.isSprinting()) {
            return;
        }

        if (selectHits && mc.player.getAttackStrengthScale(0.0F) < 0.9F) {
            return;
        }

        if (ThreadLocalRandom.current().nextDouble() > chance) {
            return;
        }

        tapStartedAt = System.currentTimeMillis();
    }

    @Override
    public void onTick() {
        if (mc.player == null || tapStartedAt < 0L) {
            return;
        }
        if (shouldPauseForBedMining()) {
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
        tapStartedAt = -1L;
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

    public boolean isSelectHits() {
        return selectHits;
    }

    public void setSelectHits(boolean selectHits) {
        this.selectHits = selectHits;
        saveConfig();
    }

    public void applyPresetLegit() {
        setChance(0.35);
        setReleaseDelayMs(75);
        setRepressDelayMs(15);
        setSelectHits(true);
    }

    public void applyPresetNormal() {
        setChance(0.60);
        setReleaseDelayMs(60);
        setRepressDelayMs(8);
        setSelectHits(true);
    }

    public void applyPresetObvious() {
        setChance(0.80);
        setReleaseDelayMs(45);
        setRepressDelayMs(0);
        setSelectHits(true);
    }

    public void applyPresetBlatant() {
        setChance(1.0);
        setReleaseDelayMs(20);
        setRepressDelayMs(0);
        setSelectHits(false);
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

        selectHits = Boolean.parseBoolean(properties.getProperty("wtap.select_hits", Boolean.toString(selectHits)));
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("wtap.chance", Double.toString(chance));
        properties.setProperty("wtap.release_delay_ms", Integer.toString(releaseDelayMs));
        properties.setProperty("wtap.repress_delay_ms", Integer.toString(repressDelayMs));
        properties.setProperty("wtap.select_hits", Boolean.toString(selectHits));
    }
}
