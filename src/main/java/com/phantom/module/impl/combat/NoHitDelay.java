/*
 * NoHitDelay.java — Reduces or removes the 1.9+ attack cooldown.
 *
 * Resets the player's attack strength timer each tick so attacks land
 * without waiting for the cooldown bar to fill. Configurable chance
 * and delay allow tuning for different servers.
 * Detectability: Blatant — attack speed anomalies are easily flagged.
 */
package com.phantom.module.impl.combat;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.util.Mth;

import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

public class NoHitDelay extends Module {
    public enum Preset {
        VANILLA("Vanilla"),
        HYPIXEL("Hypixel"),
        MINEPLEX("Mineplex"),
        BLATANT("Blatant");

        private final String label;

        Preset(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public Preset next() {
            return switch (this) {
                case VANILLA -> HYPIXEL;
                case HYPIXEL -> MINEPLEX;
                case MINEPLEX -> BLATANT;
                case BLATANT -> VANILLA;
            };
        }

        public static Preset fromString(String value) {
            if (value == null) return BLATANT;
            try {
                return Preset.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return BLATANT;
            }
        }
    }

    private double chance = 1.0D;
    private int delayTicks = 0;
    private Preset preset = Preset.BLATANT;

    private int cooldownTicksRemaining;
    private boolean swingHandled;

    public NoHitDelay() {
        super("NoHitDelay",
                "Reduces or removes the attack cooldown, allowing faster attacks.\nDetectability: Blatant",
                ModuleCategory.COMBAT,
                -1);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.options == null || mc.screen != null) {
            return;
        }

        if (cooldownTicksRemaining > 0) {
            cooldownTicksRemaining--;
        }

        boolean swingActive = mc.player.attackAnim > 0.0F;
        if (!swingActive) {
            swingHandled = false;
            return;
        }

        if (swingHandled || mc.player.attackAnim > 0.18F) {
            return;
        }

        swingHandled = true;
        if (ThreadLocalRandom.current().nextDouble() > chance) {
            return;
        }

        if (cooldownTicksRemaining > 0) {
            return;
        }
        cooldownTicksRemaining = delayTicks;

        if (preset == Preset.VANILLA) {
            return;
        }

        if (preset == Preset.BLATANT || shouldResetForMiss()) {
            mc.player.resetAttackStrengthTicker();
        }
    }

    @Override
    public void onDisable() {
        cooldownTicksRemaining = 0;
        swingHandled = false;
    }

    private boolean shouldResetForMiss() {
        if (!(mc.hitResult instanceof EntityHitResult entityHitResult)) {
            return true;
        }
        return !(entityHitResult.getEntity() instanceof LivingEntity living) || !living.isAlive();
    }

    public double getChance() { return chance; }
    public void setChance(double chance) {
        this.chance = Mth.clamp(chance, 0.0D, 1.0D);
        saveConfig();
    }

    public int getDelayTicks() { return delayTicks; }
    public void setDelayTicks(int delayTicks) {
        this.delayTicks = Mth.clamp(delayTicks, 0, 10);
        saveConfig();
    }

    public Preset getPreset() { return preset; }
    public void cyclePreset() {
        preset = preset.next();
        applyPreset(preset);
        saveConfig();
    }

    public void applyPreset(Preset p) {
        switch (p) {
            case VANILLA -> { chance = 0.3; delayTicks = 5; }
            case HYPIXEL -> { chance = 0.6; delayTicks = 2; }
            case MINEPLEX -> { chance = 0.8; delayTicks = 1; }
            case BLATANT -> { chance = 1.0; delayTicks = 0; }
        }
        this.preset = p;
        saveConfig();
    }

    @Override
    public boolean hasConfigurableSettings() { return true; }

    @Override
    public Screen createSettingsScreen(Screen parent) {
        return new ModuleSettingsScreen(parent, this);
    }

    @Override
    public void loadConfig(Properties properties) {
        super.loadConfig(properties);
        String c = properties.getProperty("nohitdelay.chance");
        if (c != null) {
            try { chance = Mth.clamp(Double.parseDouble(c.trim()), 0.0D, 1.0D); } catch (NumberFormatException ignored) {}
        }
        String d = properties.getProperty("nohitdelay.delay");
        if (d != null) {
            try { delayTicks = Mth.clamp(Integer.parseInt(d.trim()), 0, 10); } catch (NumberFormatException ignored) {}
        }
        preset = Preset.fromString(properties.getProperty("nohitdelay.preset"));
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("nohitdelay.chance", Double.toString(chance));
        properties.setProperty("nohitdelay.delay", Integer.toString(delayTicks));
        properties.setProperty("nohitdelay.preset", preset.name());
    }
}
