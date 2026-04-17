/* Copyright (c) 2026 PhantomMod. All rights reserved. */
/*
 * AntiAFK.java — Prevents idle kicks by issuing light movement and look changes.
 *
 * Detectability: Safe to Subtle — conservative settings keep movement natural.
 */
package com.phantom.module.impl.player;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

public class AntiAFK extends Module {
    private double minStartDelaySeconds = 20.0D;
    private double maxStartDelaySeconds = 45.0D;
    private double frequency = 0.35D;
    private boolean keepClose = true;
    private boolean rotation = true;
    private boolean silentAim;
    private double maxYawChange = 12.0D;
    private double maxPitchChange = 6.0D;

    private long idleSince = System.currentTimeMillis();
    private long nextActionAt = Long.MAX_VALUE;
    private Vec3 anchorPos;

    public AntiAFK() {
        super("Anti-AFK",
                "Prevents AFK kicks by issuing occasional movement and optional look changes.\nDetectability: Safe to Subtle",
                ModuleCategory.PLAYER,
                -1);
    }

    @Override
    public void onEnable() {
        if (mc.player != null) {
            anchorPos = mc.player.position();
        }
        idleSince = System.currentTimeMillis();
        nextActionAt = Long.MAX_VALUE;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.options == null || mc.screen != null) {
            return;
        }

        if (hasUserInput()) {
            idleSince = System.currentTimeMillis();
            if (keepClose) {
                anchorPos = mc.player.position();
            }
            nextActionAt = Long.MAX_VALUE;
            return;
        }

        long now = System.currentTimeMillis();
        double chosenDelay = ThreadLocalRandom.current().nextDouble(minStartDelaySeconds, Math.max(minStartDelaySeconds, maxStartDelaySeconds) + 0.0001D);
        if (now - idleSince < (long) (chosenDelay * 1000.0D)) {
            return;
        }

        if (nextActionAt == Long.MAX_VALUE) {
            nextActionAt = now;
        }
        if (now < nextActionAt) {
            return;
        }

        performAfkAction();
        long interval = (long) Mth.clamp(3000.0D - (frequency * 2200.0D), 250.0D, 3000.0D);
        nextActionAt = now + interval;
    }

    private void performAfkAction() {
        if (keepClose && anchorPos != null && mc.player.position().distanceTo(anchorPos) > 2.5D) {
            return;
        }

        if (mc.player.onGround()) {
            mc.player.jumpFromGround();
        } else {
            Vec3 motion = mc.player.getDeltaMovement();
            double x = (ThreadLocalRandom.current().nextDouble() - 0.5D) * 0.08D;
            double z = (ThreadLocalRandom.current().nextDouble() - 0.5D) * 0.08D;
            mc.player.setDeltaMovement(motion.x + x, motion.y, motion.z + z);
        }

        if (rotation) {
            float yawDelta = (float) ((ThreadLocalRandom.current().nextDouble() - 0.5D) * 2.0D * maxYawChange);
            float pitchDelta = (float) ((ThreadLocalRandom.current().nextDouble() - 0.5D) * 2.0D * maxPitchChange);
            mc.player.setYRot(mc.player.getYRot() + yawDelta);
            if (!silentAim) {
                mc.player.setXRot(Mth.clamp(mc.player.getXRot() + pitchDelta, -90.0F, 90.0F));
            }
        }
    }

    private boolean hasUserInput() {
        return mc.options.keyUp.isDown()
                || mc.options.keyDown.isDown()
                || mc.options.keyLeft.isDown()
                || mc.options.keyRight.isDown()
                || mc.options.keyJump.isDown()
                || mc.options.keyShift.isDown()
                || mc.options.keyAttack.isDown()
                || mc.options.keyUse.isDown();
    }

    public double getMinStartDelaySeconds() {
        return minStartDelaySeconds;
    }

    public void setMinStartDelaySeconds(double minStartDelaySeconds) {
        this.minStartDelaySeconds = Mth.clamp(minStartDelaySeconds, 5.0D, 300.0D);
        if (maxStartDelaySeconds < this.minStartDelaySeconds) {
            maxStartDelaySeconds = this.minStartDelaySeconds;
        }
        saveConfig();
    }

    public double getMaxStartDelaySeconds() {
        return maxStartDelaySeconds;
    }

    public void setMaxStartDelaySeconds(double maxStartDelaySeconds) {
        this.maxStartDelaySeconds = Mth.clamp(maxStartDelaySeconds, minStartDelaySeconds, 300.0D);
        saveConfig();
    }

    public double getFrequency() {
        return frequency;
    }

    public void setFrequency(double frequency) {
        this.frequency = Mth.clamp(frequency, 0.1D, 1.0D);
        saveConfig();
    }

    public boolean isKeepClose() {
        return keepClose;
    }

    public void setKeepClose(boolean keepClose) {
        this.keepClose = keepClose;
        saveConfig();
    }

    public boolean isRotation() {
        return rotation;
    }

    public void setRotation(boolean rotation) {
        this.rotation = rotation;
        saveConfig();
    }

    public boolean isSilentAim() {
        return silentAim;
    }

    public void setSilentAim(boolean silentAim) {
        this.silentAim = silentAim;
        saveConfig();
    }

    public double getMaxYawChange() {
        return maxYawChange;
    }

    public void setMaxYawChange(double maxYawChange) {
        this.maxYawChange = Mth.clamp(maxYawChange, 1.0D, 90.0D);
        saveConfig();
    }

    public double getMaxPitchChange() {
        return maxPitchChange;
    }

    public void setMaxPitchChange(double maxPitchChange) {
        this.maxPitchChange = Mth.clamp(maxPitchChange, 1.0D, 90.0D);
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
        String minValue = properties.getProperty("antiafk.min_start_delay_seconds");
        if (minValue != null) {
            try {
                minStartDelaySeconds = Mth.clamp(Double.parseDouble(minValue.trim()), 5.0D, 300.0D);
            } catch (NumberFormatException ignored) {
            }
        }
        String maxValue = properties.getProperty("antiafk.max_start_delay_seconds");
        if (maxValue != null) {
            try {
                maxStartDelaySeconds = Mth.clamp(Double.parseDouble(maxValue.trim()), minStartDelaySeconds, 300.0D);
            } catch (NumberFormatException ignored) {
            }
        }
        String frequencyValue = properties.getProperty("antiafk.frequency");
        if (frequencyValue != null) {
            try {
                frequency = Mth.clamp(Double.parseDouble(frequencyValue.trim()), 0.1D, 1.0D);
            } catch (NumberFormatException ignored) {
            }
        }
        keepClose = Boolean.parseBoolean(properties.getProperty("antiafk.keep_close", Boolean.toString(keepClose)));
        rotation = Boolean.parseBoolean(properties.getProperty("antiafk.rotation", Boolean.toString(rotation)));
        silentAim = Boolean.parseBoolean(properties.getProperty("antiafk.silent_aim", Boolean.toString(silentAim)));
        String yawValue = properties.getProperty("antiafk.max_yaw_change");
        if (yawValue != null) {
            try {
                maxYawChange = Mth.clamp(Double.parseDouble(yawValue.trim()), 1.0D, 90.0D);
            } catch (NumberFormatException ignored) {
            }
        }
        String pitchValue = properties.getProperty("antiafk.max_pitch_change");
        if (pitchValue != null) {
            try {
                maxPitchChange = Mth.clamp(Double.parseDouble(pitchValue.trim()), 1.0D, 90.0D);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("antiafk.min_start_delay_seconds", Double.toString(minStartDelaySeconds));
        properties.setProperty("antiafk.max_start_delay_seconds", Double.toString(maxStartDelaySeconds));
        properties.setProperty("antiafk.frequency", Double.toString(frequency));
        properties.setProperty("antiafk.keep_close", Boolean.toString(keepClose));
        properties.setProperty("antiafk.rotation", Boolean.toString(rotation));
        properties.setProperty("antiafk.silent_aim", Boolean.toString(silentAim));
        properties.setProperty("antiafk.max_yaw_change", Double.toString(maxYawChange));
        properties.setProperty("antiafk.max_pitch_change", Double.toString(maxPitchChange));
    }
}
