/*
 * AutoClicker.java — Repeats left-click attacks while the mouse button is held.
 *
 * Detectability: Moderate to Blatant — higher CPS and flat timing are easy to flag.
 */
package com.phantom.module.impl.combat;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;

import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

public class AutoClicker extends Module {
    private double minCps = 10.0D;
    private double maxCps = 14.0D;
    private boolean onlyWithWeapon = true;
    private boolean requireMouseDown = true;
    private boolean hitEntitiesOnly = true;

    private long lastClickAt;
    private long nextDelayMs = 100L;

    public AutoClicker() {
        super("AutoClicker",
                "Automatically clicks attack while you hold left click with a weapon.\nDetectability: Moderate to Blatant",
                ModuleCategory.COMBAT,
                -1);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.gameMode == null || mc.options == null || mc.screen != null) {
            return;
        }
        if (shouldPauseForBedMining()) {
            return;
        }

        boolean mouseHeld = mc.options.keyAttack.isDown() || (mc.mouseHandler != null && mc.mouseHandler.isLeftPressed());
        if (requireMouseDown && !mouseHeld) {
            return;
        }

        if (onlyWithWeapon && !isHoldingWeapon()) {
            return;
        }

        if (hitEntitiesOnly) {
            if (!(mc.hitResult instanceof EntityHitResult entityHitResult)) {
                return;
            }
            Entity entity = entityHitResult.getEntity();
            if (!(entity instanceof LivingEntity living) || !living.isAlive() || entity == mc.player) {
                return;
            }
        }

        long now = System.currentTimeMillis();
        if (now - lastClickAt < nextDelayMs) {
            return;
        }

        if (mc.hitResult instanceof EntityHitResult entityHitResult) {
            Entity entity = entityHitResult.getEntity();
            if (entity instanceof LivingEntity living && living.isAlive() && entity != mc.player && !isTeammateTarget(entity)) {
                mc.gameMode.attack(mc.player, entity);
                mc.player.swing(InteractionHand.MAIN_HAND);
                lastClickAt = now;
                scheduleNextDelay();
            }
        }
    }

    private void scheduleNextDelay() {
        double cps = ThreadLocalRandom.current().nextDouble(minCps, Math.max(minCps, maxCps) + 0.0001D);
        nextDelayMs = Math.max(1L, Math.round(1000.0D / Math.max(0.1D, cps)));
    }

    private boolean isHoldingWeapon() {
        String id = mc.player.getMainHandItem().getItem().getDescriptionId().toLowerCase(Locale.ROOT);
        return id.contains("sword");
    }

    public double getMinCps() {
        return minCps;
    }

    public void setMinCps(double minCps) {
        this.minCps = Mth.clamp(minCps, 1.0D, 20.0D);
        if (maxCps < this.minCps) {
            maxCps = this.minCps;
        }
        saveConfig();
    }

    public double getMaxCps() {
        return maxCps;
    }

    public void setMaxCps(double maxCps) {
        this.maxCps = Mth.clamp(maxCps, minCps, 20.0D);
        saveConfig();
    }

    public boolean isOnlyWithWeapon() {
        return onlyWithWeapon;
    }

    public void setOnlyWithWeapon(boolean onlyWithWeapon) {
        this.onlyWithWeapon = onlyWithWeapon;
        saveConfig();
    }

    public boolean isRequireMouseDown() {
        return requireMouseDown;
    }

    public void setRequireMouseDown(boolean requireMouseDown) {
        this.requireMouseDown = requireMouseDown;
        saveConfig();
    }

    public boolean isHitEntitiesOnly() {
        return hitEntitiesOnly;
    }

    public void setHitEntitiesOnly(boolean hitEntitiesOnly) {
        this.hitEntitiesOnly = hitEntitiesOnly;
        saveConfig();
    }

    public void applyPresetLegit() {
        setMinCps(8.0);
        setMaxCps(11.0);
        setOnlyWithWeapon(true);
        setRequireMouseDown(true);
        setHitEntitiesOnly(true);
    }

    public void applyPresetNormal() {
        setMinCps(10.0);
        setMaxCps(13.0);
        setOnlyWithWeapon(true);
        setRequireMouseDown(true);
        setHitEntitiesOnly(true);
    }

    public void applyPresetObvious() {
        setMinCps(12.0);
        setMaxCps(16.0);
        setOnlyWithWeapon(true);
        setRequireMouseDown(true);
        setHitEntitiesOnly(true);
    }

    public void applyPresetBlatant() {
        setMinCps(16.0);
        setMaxCps(20.0);
        setOnlyWithWeapon(false);
        setRequireMouseDown(false);
        setHitEntitiesOnly(false);
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
        String min = properties.getProperty("autoclicker.min_cps");
        if (min != null) {
            try { minCps = Mth.clamp(Double.parseDouble(min.trim()), 1.0D, 20.0D); } catch (Exception ignored) {}
        }
        String max = properties.getProperty("autoclicker.max_cps");
        if (max != null) {
            try { maxCps = Mth.clamp(Double.parseDouble(max.trim()), minCps, 20.0D); } catch (Exception ignored) {}
        }
        onlyWithWeapon = Boolean.parseBoolean(properties.getProperty("autoclicker.only_with_weapon", Boolean.toString(onlyWithWeapon)));
        requireMouseDown = Boolean.parseBoolean(properties.getProperty("autoclicker.require_mouse_down", Boolean.toString(requireMouseDown)));
        hitEntitiesOnly = Boolean.parseBoolean(properties.getProperty("autoclicker.hit_entities_only", Boolean.toString(hitEntitiesOnly)));
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("autoclicker.min_cps", Double.toString(minCps));
        properties.setProperty("autoclicker.max_cps", Double.toString(maxCps));
        properties.setProperty("autoclicker.only_with_weapon", Boolean.toString(onlyWithWeapon));
        properties.setProperty("autoclicker.require_mouse_down", Boolean.toString(requireMouseDown));
        properties.setProperty("autoclicker.hit_entities_only", Boolean.toString(hitEntitiesOnly));
    }
}
