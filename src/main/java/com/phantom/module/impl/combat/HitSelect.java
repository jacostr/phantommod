/*
 * HitSelect.java — Interrupts attacks to gain timing advantages.
 *
 * Detectability: Moderate — consistent interruption patterns can still be profiled.
 */
package com.phantom.module.impl.combat;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.Entity;

import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

public class HitSelect extends Module {
    public enum Mode {
        PAUSE("Pause"),
        ACTIVE("Active");

        private final String label;

        Mode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public Mode next() {
            return this == PAUSE ? ACTIVE : PAUSE;
        }

        public static Mode fromString(String value) {
            if (value == null) {
                return PAUSE;
            }
            try {
                return Mode.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return PAUSE;
            }
        }
    }

    public enum Preference {
        KB_REDUCTION("KB reduction"),
        CRITICAL_HITS("Critical hits");

        private final String label;

        Preference(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public Preference next() {
            return this == KB_REDUCTION ? CRITICAL_HITS : KB_REDUCTION;
        }

        public static Preference fromString(String value) {
            if (value == null) {
                return KB_REDUCTION;
            }
            try {
                return Preference.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return KB_REDUCTION;
            }
        }
    }

    private double chance = 0.6D;
    private Mode mode = Mode.PAUSE;
    private Preference preference = Preference.KB_REDUCTION;

    private int pauseTicksRemaining;
    private int lastHurtTime;

    public HitSelect() {
        super("HitSelect",
                "Interrupts attacks to gain combat advantages like reduced knockback or easier crit timing. \n lower chance + PAUSE mode is significantly safer on hypixel.\nDetectability: Moderate",
                ModuleCategory.COMBAT,
                -1);
    }

    @Override
    public void onTick() {
        if (mc.player == null) {
            pauseTicksRemaining = 0;
            return;
        }
        if (shouldPauseForBedMining()) {
            pauseTicksRemaining = 0;
            lastHurtTime = mc.player.hurtTime;
            return;
        }

        if (pauseTicksRemaining > 0) {
            pauseTicksRemaining--;
        }

        if (mc.player.hurtTime > lastHurtTime && ThreadLocalRandom.current().nextDouble() < chance) {
            pauseTicksRemaining = computePauseTicks();
        }
        lastHurtTime = mc.player.hurtTime;
    }

    @Override
    public void onDisable() {
        pauseTicksRemaining = 0;
        lastHurtTime = 0;
    }

    public boolean shouldCancelAttack(Entity target) {
        if (mc.player == null || target == null) {
            return false;
        }

        if (mode == Mode.PAUSE) {
            return pauseTicksRemaining > 0;
        }

        if (pauseTicksRemaining <= 0) {
            return false;
        }

        return switch (preference) {
            // KB_REDUCTION: wait for the target's hurt animation to expire before hitting
            // again so our next hit sends full knockback. Cancel while target is still
            // in their invincibility window (hurtTime > 0).
            case KB_REDUCTION -> {
                if (target instanceof net.minecraft.world.entity.LivingEntity living) {
                    yield living.hurtTime > 0;
                }
                yield false;
            }
            // CRITICAL_HITS: cancel while on the ground — wait until we are falling
            // (not on ground) so the next hit registers as a critical.
            case CRITICAL_HITS -> mc.player.onGround();
        };
    }

    private int computePauseTicks() {
        // PAUSE mode: stop attacking entirely for N ticks after being hit.
        // ACTIVE mode: shorter window — just enough to re-check the condition each
        // tick.
        // KB_REDUCTION needs fewer ticks (target hurtTime is 10, we just wait for it).
        // CRITICAL_HITS needs slightly more to reliably catch a falling window.
        return switch (mode) {
            case PAUSE -> preference == Preference.KB_REDUCTION ? 4 : 6;
            case ACTIVE -> preference == Preference.KB_REDUCTION ? 2 : 4;
        };
    }

    public double getChance() {
        return chance;
    }

    public void setChance(double chance) {
        this.chance = Math.max(0.0D, Math.min(1.0D, chance));
        saveConfig();
    }

    public Mode getMode() {
        return mode;
    }

    public void cycleMode() {
        mode = mode.next();
        saveConfig();
    }

    public Preference getPreference() {
        return preference;
    }

    public void cyclePreference() {
        preference = preference.next();
        saveConfig();
    }

    public void applyPresetLegit() {
        setChance(0.35);
        mode = Mode.PAUSE;
        preference = Preference.KB_REDUCTION;
        saveConfig();
    }

    public void applyPresetNormal() {
        setChance(0.55);
        mode = Mode.PAUSE;
        preference = Preference.CRITICAL_HITS;
        saveConfig();
    }

    public void applyPresetObvious() {
        setChance(0.80);
        mode = Mode.ACTIVE;
        preference = Preference.KB_REDUCTION;
        saveConfig();
    }

    public void applyPresetBlatant() {
        setChance(1.0);
        mode = Mode.ACTIVE;
        preference = Preference.CRITICAL_HITS;
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
        String chanceValue = properties.getProperty("hitselect.chance");
        if (chanceValue != null) {
            try {
                chance = Math.max(0.0D, Math.min(1.0D, Double.parseDouble(chanceValue.trim())));
            } catch (NumberFormatException ignored) {
            }
        }
        mode = Mode.fromString(properties.getProperty("hitselect.mode"));
        preference = Preference.fromString(properties.getProperty("hitselect.preference"));
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("hitselect.chance", Double.toString(chance));
        properties.setProperty("hitselect.mode", mode.name());
        properties.setProperty("hitselect.preference", preference.name());
    }
}