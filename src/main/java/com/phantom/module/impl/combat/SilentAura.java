/* Copyright (c) 2026 PhantomMod. All rights reserved. */
/*
 * SilentAura.java — Automatically attacks nearby entities and smoothly pulls the camera.
 *
 * Finds the best target within range, smoothly pulls the client camera to face them,
 * and attacks using gameMode.attack().
 * Detectability: Subtle at high smoothing, Blatant at low smoothing.
 */
package com.phantom.module.impl.combat;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import com.phantom.module.impl.player.AntiBot;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

public class SilentAura extends Module {
    public enum TargetMode {
        DISTANCE("Distance"),
        HEALTH("Health"),
        YAW("Yaw");

        private final String label;
        TargetMode(String label) { this.label = label; }
        public String getLabel() { return label; }
        public TargetMode next() {
            return switch (this) {
                case DISTANCE -> HEALTH;
                case HEALTH -> YAW;
                case YAW -> DISTANCE;
            };
        }
        public static TargetMode fromString(String v) {
            if (v == null) return DISTANCE;
            try { return TargetMode.valueOf(v.trim().toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException e) { return DISTANCE; }
        }
    }

    private double minCps = 8.0;
    private double maxCps = 12.0;
    private double smoothing = 5.0;
    private double snapiness = 3.0;
    private double maxAngle = 180.0;
    private double attackRange = 4.0;
    private TargetMode targetMode = TargetMode.DISTANCE;
    private boolean requireMouseDown = false;
    private boolean aimVertically = false;
    private boolean breakBlocksPause = true;
    private boolean limitToWeapons = true;
    private boolean targetPlayers = true;
    private boolean targetMobs = true;
    private boolean targetAnimals = false;
    private boolean hypixelMode = false;

    private long lastAttackAt;
    private long nextDelayMs = 100L;
    private LivingEntity currentTarget;

    public SilentAura() {
        super("SilentAura",
                "Automatically attacks nearby entities while smoothly aiming at them.\n" +
                "Detectability: Subtle to Blatant depending on smoothing.",
                ModuleCategory.COMBAT,
                -1);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.gameMode == null || mc.level == null || mc.screen != null) {
            currentTarget = null;
            return;
        }
        if (requireMouseDown && (mc.options == null || !mc.options.keyAttack.isDown())) {
            currentTarget = null;
            return;
        }
        if (breakBlocksPause && shouldPauseForBedMining()) {
            currentTarget = null;
            return;
        }
        if (breakBlocksPause && mc.gameMode.isDestroying()) {
            currentTarget = null;
            return;
        }
        if (limitToWeapons && !isHoldingWeapon()) {
            currentTarget = null;
            return;
        }

        currentTarget = findTarget();
        if (currentTarget == null) return;

        long now = System.currentTimeMillis();
        if (now - lastAttackAt < nextDelayMs) return;

        boolean canAttack = getAngleToEntity(currentTarget) < 35.0;

        if (hypixelMode) {
            canAttack = (mc.crosshairPickEntity == currentTarget) && (mc.player.distanceTo(currentTarget) <= 3.0);
        }

        if (canAttack) {
            mc.gameMode.attack(mc.player, currentTarget);
            mc.player.swing(InteractionHand.MAIN_HAND);
            lastAttackAt = now;
            scheduleNextDelay();
        }
    }

    @Override
    public void onRender(WorldRenderContext context) {
        if (currentTarget != null && mc.player != null) {
            aimAt(currentTarget);
        }
    }

    private LivingEntity findTarget() {
        if (mc.crosshairPickEntity instanceof LivingEntity focusedCrosshair && isValidTarget(focusedCrosshair)
                && getAngleToEntity(focusedCrosshair) <= Math.max(maxAngle, 20.0)) {
            return focusedCrosshair;
        }

        if (mc.hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof LivingEntity focused) {
             if (isValidTarget(focused)) {
                return focused;
            }
        }
        
        List<Entity> nearby = mc.level.getEntitiesOfClass(Entity.class,
                mc.player.getBoundingBox().inflate(attackRange),
                this::isValidTarget);

        if (nearby.isEmpty()) return null;

        Comparator<Entity> comp = switch (targetMode) {
            case DISTANCE -> Comparator.comparingDouble(e -> mc.player.distanceToSqr(e));
            case HEALTH -> Comparator.comparingDouble(e -> e instanceof LivingEntity le ? le.getHealth() : Float.MAX_VALUE);
            case YAW -> Comparator.comparingDouble(e -> getAngleToEntity(e));
        };

        Entity best = nearby.stream().min(comp).orElse(null);
        return best instanceof LivingEntity le ? le : null;
    }

    private boolean isValidTarget(Entity entity) {
        if (entity == mc.player) return false;
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) return false;
        if (AntiBot.isBot(entity)) return false;
        if (isTeammateTarget(entity)) return false;
        if (mc.player.distanceTo(entity) > attackRange) return false;

        double angle = getAngleToEntity(entity);
        if (angle > maxAngle) return false;

        if (entity instanceof Player && targetPlayers) return true;
        if (entity instanceof Animal && targetAnimals) return true;
        if (entity instanceof Mob && targetMobs) return true;
        return false;
    }

    private double getAngleToEntity(Entity entity) {
        double dx = entity.getX() - mc.player.getX();
        double dz = entity.getZ() - mc.player.getZ();
        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float diff = Mth.wrapDegrees(targetYaw - mc.player.getYRot());
        return Math.abs(diff);
    }

    private void aimAt(Entity entity) {
        double diffX = entity.getX() - mc.player.getX();
        double diffZ = entity.getZ() - mc.player.getZ();
        double targetY = entity.getY() + entity.getBbHeight() * 0.5;
        double diffY = targetY - (mc.player.getY() + mc.player.getEyeHeight());

        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);
        
        float targetYaw = (float) (Mth.atan2(diffZ, diffX) * (180F / Math.PI)) - 90.0F;
        float targetPitch = (float) -(Mth.atan2(diffY, dist) * (180F / Math.PI));
        
        float currentYaw = mc.player.getYRot();
        float currentPitch = mc.player.getXRot();
        
        float yawDiff = Mth.wrapDegrees(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;
        float smoothFactor = (float) ((smoothing - 1.0) / 9.0);
        float snapFactor = (float) ((snapiness - 1.0) / 9.0);

        float divisor = Mth.lerp(smoothFactor, 6.0F, 16.0F) - (snapFactor * 3.0F);
        divisor = Math.max(1.75F, divisor);
        float maxYawStep = Mth.lerp(snapFactor, 1.0F, 12.0F) * Mth.lerp(smoothFactor, 1.15F, 0.8F);
        float yawStep = Mth.clamp(yawDiff / divisor, -maxYawStep, maxYawStep);
        float nextYaw = currentYaw + yawStep;

        float snapThreshold = Mth.lerp(snapFactor, 0.08F, 0.85F) * Mth.lerp(smoothFactor, 0.8F, 1.1F);
        if (Math.abs(yawDiff) <= snapThreshold) {
            nextYaw = targetYaw;
        }

        applyYaw(nextYaw);
        
        if (aimVertically) {
            float pitchDivisor = Mth.lerp(smoothFactor, 6.5F, 16.5F) - (snapFactor * 2.5F);
            pitchDivisor = Math.max(1.8F, pitchDivisor);
            float maxPitchStep = Mth.lerp(snapFactor, 0.8F, 8.0F) * Mth.lerp(smoothFactor, 1.1F, 0.82F);
            float pitchStep = Mth.clamp(pitchDiff / pitchDivisor, -maxPitchStep, maxPitchStep);
            float nextPitch = Mth.clamp(currentPitch + pitchStep, -90.0F, 90.0F);
            if (Math.abs(pitchDiff) <= snapThreshold) {
                nextPitch = Mth.clamp(targetPitch, -90.0F, 90.0F);
            }
            applyPitch(nextPitch);
        }
    }

    private void applyYaw(float yaw) {
        mc.player.setYRot(yaw);
        mc.player.yRotO = yaw;
        mc.player.setYHeadRot(yaw);
        mc.player.yHeadRotO = yaw;
        mc.player.setYBodyRot(yaw);
        mc.player.yBodyRotO = yaw;
    }

    private void applyPitch(float pitch) {
        mc.player.setXRot(pitch);
        mc.player.xRotO = pitch;
    }

    private void scheduleNextDelay() {
        double cps = ThreadLocalRandom.current().nextDouble(minCps, Math.max(minCps, maxCps) + 0.001);
        nextDelayMs = Math.max(1L, Math.round(1000.0 / Math.max(0.1, cps)));
    }

    private boolean isHoldingWeapon() {
        String id = mc.player.getMainHandItem().getItem().getDescriptionId().toLowerCase(Locale.ROOT);
        return id.contains("sword") || id.contains("_axe") || id.contains("mace") || id.contains("trident");
    }

    // Getters/Setters
    public double getMinCps() { return minCps; }
    public void setMinCps(double v) { minCps = Mth.clamp(v, 1.0, 20.0); if (maxCps < minCps) maxCps = minCps; saveConfig(); }
    public double getMaxCps() { return maxCps; }
    public void setMaxCps(double v) { maxCps = Mth.clamp(v, minCps, 20.0); saveConfig(); }
    public double getSmoothing() { return smoothing; }
    public void setSmoothing(double v) { smoothing = Mth.clamp(v, 1.0, 10.0); saveConfig(); }
    public double getSnapiness() { return snapiness; }
    public void setSnapiness(double v) { snapiness = Mth.clamp(v, 1.0, 10.0); saveConfig(); }
    public double getMaxAngle() { return maxAngle; }
    public void setMaxAngle(double v) { maxAngle = Mth.clamp(v, 10.0, 360.0); saveConfig(); }
    public double getAttackRange() { return attackRange; }
    public void setAttackRange(double v) { attackRange = Mth.clamp(v, 2.0, 6.0); saveConfig(); }
    public TargetMode getTargetMode() { return targetMode; }
    public void cycleTargetMode() { targetMode = targetMode.next(); saveConfig(); }
    public boolean isRequireMouseDown() { return requireMouseDown; }
    public void setRequireMouseDown(boolean v) { requireMouseDown = v; saveConfig(); }
    public boolean isAimVertically() { return aimVertically; }
    public void setAimVertically(boolean v) { aimVertically = v; saveConfig(); }
    public boolean isBreakBlocksPause() { return breakBlocksPause; }
    public void setBreakBlocksPause(boolean v) { breakBlocksPause = v; saveConfig(); }
    public boolean isLimitToWeapons() { return limitToWeapons; }
    public void setLimitToWeapons(boolean v) { limitToWeapons = v; saveConfig(); }
    public boolean isTargetPlayers() { return targetPlayers; }
    public void setTargetPlayers(boolean v) { targetPlayers = v; saveConfig(); }
    public boolean isTargetMobs() { return targetMobs; }
    public void setTargetMobs(boolean v) { targetMobs = v; saveConfig(); }
    public boolean isTargetAnimals() { return targetAnimals; }
    public void setTargetAnimals(boolean v) { targetAnimals = v; saveConfig(); }
    public boolean isHypixelMode() { return hypixelMode; }
    public void setHypixelMode(boolean v) { hypixelMode = v; saveConfig(); }

    @Override public boolean hasConfigurableSettings() { return true; }
    @Override public Screen createSettingsScreen(Screen parent) { return new ModuleSettingsScreen(parent, this); }

    @Override
    public void loadConfig(Properties p) {
        super.loadConfig(p);
        try { minCps = Mth.clamp(Double.parseDouble(p.getProperty("silentaura.min_cps", "8.0")), 1.0, 20.0); } catch (Exception ignored) {}
        try { maxCps = Mth.clamp(Double.parseDouble(p.getProperty("silentaura.max_cps", "12.0")), minCps, 20.0); } catch (Exception ignored) {}
        try { smoothing = Mth.clamp(Double.parseDouble(p.getProperty("silentaura.smoothing", "5.0")), 1.0, 10.0); } catch (Exception ignored) {}
        try { snapiness = Mth.clamp(Double.parseDouble(p.getProperty("silentaura.snapiness", "3.0")), 1.0, 10.0); } catch (Exception ignored) {}
        try { maxAngle = Mth.clamp(Double.parseDouble(p.getProperty("silentaura.max_angle", "180.0")), 10.0, 360.0); } catch (Exception ignored) {}
        try { attackRange = Mth.clamp(Double.parseDouble(p.getProperty("silentaura.range", "4.0")), 2.0, 6.0); } catch (Exception ignored) {}
        targetMode = TargetMode.fromString(p.getProperty("silentaura.target_mode"));
        requireMouseDown = Boolean.parseBoolean(p.getProperty("silentaura.require_mouse", Boolean.toString(requireMouseDown)));
        aimVertically = Boolean.parseBoolean(p.getProperty("silentaura.aim_vertically", Boolean.toString(aimVertically)));
        breakBlocksPause = Boolean.parseBoolean(p.getProperty("silentaura.break_pause", Boolean.toString(breakBlocksPause)));
        limitToWeapons = Boolean.parseBoolean(p.getProperty("silentaura.limit_weapons", Boolean.toString(limitToWeapons)));
        targetPlayers = Boolean.parseBoolean(p.getProperty("silentaura.target_players", Boolean.toString(targetPlayers)));
        targetMobs = Boolean.parseBoolean(p.getProperty("silentaura.target_mobs", Boolean.toString(targetMobs)));
        targetAnimals = Boolean.parseBoolean(p.getProperty("silentaura.target_animals", Boolean.toString(targetAnimals)));
        hypixelMode = Boolean.parseBoolean(p.getProperty("silentaura.hypixel_mode", Boolean.toString(hypixelMode)));
    }

    @Override
    public void saveConfig(Properties p) {
        super.saveConfig(p);
        p.setProperty("silentaura.min_cps", Double.toString(minCps));
        p.setProperty("silentaura.max_cps", Double.toString(maxCps));
        p.setProperty("silentaura.smoothing", Double.toString(smoothing));
        p.setProperty("silentaura.snapiness", Double.toString(snapiness));
        p.setProperty("silentaura.max_angle", Double.toString(maxAngle));
        p.setProperty("silentaura.range", Double.toString(attackRange));
        p.setProperty("silentaura.target_mode", targetMode.name());
        p.setProperty("silentaura.require_mouse", Boolean.toString(requireMouseDown));
        p.setProperty("silentaura.aim_vertically", Boolean.toString(aimVertically));
        p.setProperty("silentaura.break_pause", Boolean.toString(breakBlocksPause));
        p.setProperty("silentaura.limit_weapons", Boolean.toString(limitToWeapons));
        p.setProperty("silentaura.target_players", Boolean.toString(targetPlayers));
        p.setProperty("silentaura.target_mobs", Boolean.toString(targetMobs));
        p.setProperty("silentaura.target_animals", Boolean.toString(targetAnimals));
        p.setProperty("silentaura.hypixel_mode", Boolean.toString(hypixelMode));
    }
}
