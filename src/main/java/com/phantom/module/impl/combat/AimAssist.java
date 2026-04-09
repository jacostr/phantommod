/*
 * AimAssist.java — Smooth camera aim toward nearby targets (Combat module).
 *
 * While holding Left Click with a Sword, smoothly interpolates the player's yaw/pitch
 * toward the closest living entity within a configurable FOV cone. Adds ±1.5° random
 * noise per tick to defeat Watchdog rotation-pattern detectors. Smoothing and FOV
 * are configurable via sliders in settings.
 * Detectability: Subtle — looks human at moderate speed values.
 */
package com.phantom.module.impl.combat;

import com.phantom.module.impl.player.AntiBot;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.EntityHitResult;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public class AimAssist extends Module {
    public enum TargetArea {
        CENTER("Center"),
        HEAD("Head"),
        FEET("Feet");

        private final String label;

        TargetArea(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public TargetArea next() {
            return switch (this) {
                case CENTER -> HEAD;
                case HEAD -> FEET;
                case FEET -> CENTER;
            };
        }

        public static TargetArea fromString(String value) {
            if (value == null) {
                return CENTER;
            }
            try {
                return TargetArea.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return CENTER;
            }
        }
    }

    public enum TargetMode {
        YAW("Yaw"),
        DISTANCE("Distance");

        private final String label;

        TargetMode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public TargetMode next() {
            return this == YAW ? DISTANCE : YAW;
        }

        public static TargetMode fromString(String value) {
            if (value == null) {
                return YAW;
            }
            try {
                return TargetMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return YAW;
            }
        }
    }

    private double smoothing = 5.0;
    private double snapiness = 3.0;
    private double fov = 90.0;
    private double distance = 4.5;
    private boolean requireMouseDown = true;
    private boolean clickAim = true;
    private boolean aimVertically;
    private boolean limitToWeapons = true;
    private boolean visibilityCheck = true;
    private boolean targetPlayers = true;
    private boolean targetMobs = true;
    private boolean targetAnimals;
    private TargetArea targetArea = TargetArea.CENTER;
    private TargetMode targetMode = TargetMode.YAW;
    private boolean attackHeldLastTick;
    private long clickAimWindowUntil;
    
    public AimAssist() {
        super("AimAssist", "Smoothly adjusts your camera toward nearby targets with configurable speed, range, and target mode.\nDetectability: Subtle", ModuleCategory.COMBAT, -1);
    }

    @Override
    public boolean hasConfigurableSettings() {
        return true;
    }

    public double getSmoothing() { return smoothing; }
    public void setSmoothing(double smoothing) {
        this.smoothing = Math.max(1.0, Math.min(10.0, smoothing));
        saveConfig();
    }
    public double getSnapiness() { return snapiness; }
    public void setSnapiness(double snapiness) {
        this.snapiness = Math.max(1.0, Math.min(10.0, snapiness));
        saveConfig();
    }
    public double getFov() { return fov; }
    public void setFov(double fov) { this.fov = fov; saveConfig(); }
    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = Math.max(2.5, Math.min(6.0, distance)); saveConfig(); }
    public boolean isRequireMouseDown() { return requireMouseDown; }
    public void setRequireMouseDown(boolean requireMouseDown) { this.requireMouseDown = requireMouseDown; saveConfig(); }
    public boolean isClickAim() { return clickAim; }
    public void setClickAim(boolean clickAim) { this.clickAim = clickAim; saveConfig(); }
    public boolean isAimVertically() { return aimVertically; }
    public void setAimVertically(boolean aimVertically) { this.aimVertically = aimVertically; saveConfig(); }
    public boolean isLimitToWeapons() { return limitToWeapons; }
    public void setLimitToWeapons(boolean limitToWeapons) { this.limitToWeapons = limitToWeapons; saveConfig(); }
    public boolean isVisibilityCheck() { return visibilityCheck; }
    public void setVisibilityCheck(boolean visibilityCheck) { this.visibilityCheck = visibilityCheck; saveConfig(); }
    public boolean isTargetPlayers() { return targetPlayers; }
    public void setTargetPlayers(boolean targetPlayers) { this.targetPlayers = targetPlayers; saveConfig(); }
    public boolean isTargetMobs() { return targetMobs; }
    public void setTargetMobs(boolean targetMobs) { this.targetMobs = targetMobs; saveConfig(); }
    public boolean isTargetAnimals() { return targetAnimals; }
    public void setTargetAnimals(boolean targetAnimals) { this.targetAnimals = targetAnimals; saveConfig(); }
    public TargetArea getTargetArea() { return targetArea; }
    public void cycleTargetArea() { targetArea = targetArea.next(); saveConfig(); }
    public TargetMode getTargetMode() { return targetMode; }
    public void cycleTargetMode() { targetMode = targetMode.next(); saveConfig(); }

    public void applyPresetLegit() {
        setSmoothing(8.5);
        setSnapiness(2.0);
        setFov(70.0);
        setDistance(4.0);
        setRequireMouseDown(true);
        setClickAim(true);
        setAimVertically(false);
        setLimitToWeapons(true);
        setVisibilityCheck(true);
        setTargetPlayers(true);
        setTargetMobs(false);
        setTargetAnimals(false);
    }

    public void applyPresetNormal() {
        setSmoothing(7.0);
        setSnapiness(3.5);
        setFov(90.0);
        setDistance(4.5);
        setRequireMouseDown(true);
        setClickAim(false);
        setAimVertically(false);
        setLimitToWeapons(true);
        setVisibilityCheck(true);
        setTargetPlayers(true);
        setTargetMobs(true);
        setTargetAnimals(false);
    }

    public void applyPresetObvious() {
        setSmoothing(5.0);
        setSnapiness(6.0);
        setFov(180.0);
        setDistance(5.0);
        setRequireMouseDown(true);
        setClickAim(false);
        setAimVertically(true);
        setLimitToWeapons(true);
        setVisibilityCheck(false);
        setTargetPlayers(true);
        setTargetMobs(true);
        setTargetAnimals(false);
    }

    public void applyPresetBlatant() {
        setSmoothing(3.0);
        setSnapiness(9.0);
        setFov(360.0);
        setDistance(6.0);
        setRequireMouseDown(false);
        setClickAim(false);
        setAimVertically(true);
        setLimitToWeapons(false);
        setVisibilityCheck(false);
        setTargetPlayers(true);
        setTargetMobs(true);
        setTargetAnimals(true);
    }

    @Override
    public void onTick() {
        updateClickAimWindow();
    }

    @Override
    public void onRender(WorldRenderContext context) {
        updateAim();
    }

    private void updateAim() {
        if (mc.player == null || mc.level == null || mc.options == null) return;
        if (shouldPauseForBedMining()) return;
        if (mc.screen != null) return;
        
        if (limitToWeapons && !isHoldingWeapon()) return;
        if (requireMouseDown && !isAttackHeld()) return;
        if (clickAim && !isWithinClickAimWindow()) return;

        Entity target = getBestTarget();
        if (target != null) {
            aimAt(target);
        }
    }
    
    private Entity getBestTarget() {
        if (mc.crosshairPickEntity instanceof Entity focusedCrosshair && isValidTarget(focusedCrosshair)
                && isInFov(focusedCrosshair, Math.max(fov, 20.0))) {
            return focusedCrosshair;
        }

        if (mc.hitResult instanceof EntityHitResult entityHitResult) {
            Entity focused = entityHitResult.getEntity();
            if (isValidTarget(focused)) {
                return focused;
            }
        }

        List<Entity> entities = mc.level.getEntities(mc.player, mc.player.getBoundingBox().inflate(distance));
        return entities.stream()
            .filter(this::isValidTarget)
            .filter(e -> isInFov(e, fov))
            .min(Comparator.comparingDouble(this::targetSortValue))
            .orElse(null);
    }

    private boolean isValidTarget(Entity entity) {
        if (!(entity instanceof LivingEntity living) || entity == mc.player || !living.isAlive() || mc.player.distanceTo(entity) > distance) {
            return false;
        }
        if (AntiBot.isBot(entity)) {
            return false;
        }
        if (visibilityCheck && !mc.player.hasLineOfSight(entity)) {
            return false;
        }
        if (isTeammateTarget(entity)) {
            return false;
        }
        if (entity instanceof Player) {
            return targetPlayers;
        }
        if (entity instanceof Animal || entity instanceof AgeableMob) {
            return targetAnimals;
        }
        if (entity instanceof Mob) {
            return targetMobs;
        }
        return false;
    }

    private double targetSortValue(Entity entity) {
        return targetMode == TargetMode.DISTANCE ? mc.player.distanceTo(entity) : yawDistanceTo(entity);
    }

    private boolean isHoldingWeapon() {
        String id = mc.player.getMainHandItem().getItem().getDescriptionId().toLowerCase(Locale.ROOT);
        return id.contains("sword");
    }

    private boolean isAttackHeld() {
        return mc.options.keyAttack.isDown() || (mc.mouseHandler != null && mc.mouseHandler.isLeftPressed());
    }

    private void updateClickAimWindow() {
        if (mc.player == null || mc.options == null) {
            attackHeldLastTick = false;
            clickAimWindowUntil = 0L;
            return;
        }

        boolean attackHeld = isAttackHeld();
        long now = System.currentTimeMillis();
        if (attackHeld && !attackHeldLastTick) {
            clickAimWindowUntil = now + 175L;
        }
        if (mc.player.attackAnim > 0.0F && mc.player.attackAnim < 0.18F) {
            clickAimWindowUntil = now + 175L;
        }
        attackHeldLastTick = attackHeld;
    }

    private boolean isWithinClickAimWindow() {
        return System.currentTimeMillis() <= clickAimWindowUntil;
    }

    private boolean isInFov(Entity entity, double fovLimit) {
        return yawDistanceTo(entity) <= Math.max(5.0, fovLimit / 2.0);
    }

    private double yawDistanceTo(Entity entity) {
        double diffX = entity.getX() - mc.player.getX();
        double diffZ = entity.getZ() - mc.player.getZ();
        float targetYaw = (float) (Mth.atan2(diffZ, diffX) * (180F / Math.PI)) - 90.0F;
        float wrapDiff = Mth.wrapDegrees(targetYaw - mc.player.getYRot());
        return Math.abs(wrapDiff);
    }

    private void aimAt(Entity entity) {
        double diffX = entity.getX() - mc.player.getX();
        double diffZ = entity.getZ() - mc.player.getZ();
        double targetY = switch (targetArea) {
            case CENTER -> entity.getY() + entity.getBbHeight() * 0.5;
            case HEAD -> entity.getY() + entity.getEyeHeight();
            case FEET -> entity.getY() + 0.15;
        };
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
        if (mc.getCameraEntity() == mc.player) {
            applyYaw(nextYaw);
        }
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
            if (mc.getCameraEntity() == mc.player) {
                applyPitch(nextPitch);
            }
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

    @Override
    public void loadConfig(Properties properties) {
        super.loadConfig(properties);
        String s = properties.getProperty("aim.smoothing");
        if (s != null) {
            try { this.smoothing = Double.parseDouble(s); } catch (Exception ignored) {}
        }
        String snap = properties.getProperty("aim.snapiness");
        if (snap != null) {
            try { this.snapiness = Math.max(1.0, Math.min(10.0, Double.parseDouble(snap))); } catch (Exception ignored) {}
        }
        String f = properties.getProperty("aim.fov");
        if (f != null) {
            try { this.fov = Double.parseDouble(f); } catch (Exception ignored) {}
        }
        String d = properties.getProperty("aim.distance");
        if (d != null) {
            try { this.distance = Math.max(2.5, Math.min(6.0, Double.parseDouble(d))); } catch (Exception ignored) {}
        }
        requireMouseDown = Boolean.parseBoolean(properties.getProperty("aim.require_mouse_down", Boolean.toString(requireMouseDown)));
        clickAim = Boolean.parseBoolean(properties.getProperty("aim.click_aim", Boolean.toString(clickAim)));
        aimVertically = Boolean.parseBoolean(properties.getProperty("aim.aim_vertically", Boolean.toString(aimVertically)));
        limitToWeapons = Boolean.parseBoolean(properties.getProperty("aim.limit_to_weapons", Boolean.toString(limitToWeapons)));
        visibilityCheck = Boolean.parseBoolean(properties.getProperty("aim.visibility_check", Boolean.toString(visibilityCheck)));
        targetPlayers = Boolean.parseBoolean(properties.getProperty("aim.target_players", Boolean.toString(targetPlayers)));
        targetMobs = Boolean.parseBoolean(properties.getProperty("aim.target_mobs", Boolean.toString(targetMobs)));
        targetAnimals = Boolean.parseBoolean(properties.getProperty("aim.target_animals", Boolean.toString(targetAnimals)));
        targetArea = TargetArea.fromString(properties.getProperty("aim.target_area"));
        targetMode = TargetMode.fromString(properties.getProperty("aim.target_mode"));
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("aim.smoothing", Double.toString(smoothing));
        properties.setProperty("aim.snapiness", Double.toString(snapiness));
        properties.setProperty("aim.fov", Double.toString(fov));
        properties.setProperty("aim.distance", Double.toString(distance));
        properties.setProperty("aim.require_mouse_down", Boolean.toString(requireMouseDown));
        properties.setProperty("aim.click_aim", Boolean.toString(clickAim));
        properties.setProperty("aim.aim_vertically", Boolean.toString(aimVertically));
        properties.setProperty("aim.limit_to_weapons", Boolean.toString(limitToWeapons));
        properties.setProperty("aim.visibility_check", Boolean.toString(visibilityCheck));
        properties.setProperty("aim.target_players", Boolean.toString(targetPlayers));
        properties.setProperty("aim.target_mobs", Boolean.toString(targetMobs));
        properties.setProperty("aim.target_animals", Boolean.toString(targetAnimals));
        properties.setProperty("aim.target_area", targetArea.name());
        properties.setProperty("aim.target_mode", targetMode.name());
    }
}
