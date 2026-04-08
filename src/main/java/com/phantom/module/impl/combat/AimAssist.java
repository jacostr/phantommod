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

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.gui.widget.PhantomSlider;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Properties;

public class AimAssist extends Module {
    private double smoothing = 5.0; // Higher = slower/smoother camera
    private double fov = 90.0;     // Field of view in which to assist
    
    public AimAssist() {
        super("AimAssist", "Smoothly adjusts your camera towards targets while you hold Left Click with a Sword. Bypasses Watchdog heuristics.\nDetectability: Subtle", ModuleCategory.COMBAT, -1);
    }

    @Override
    public boolean hasConfigurableSettings() {
        return true;
    }

    public double getSmoothing() { return smoothing; }
    public void setSmoothing(double smoothing) { this.smoothing = smoothing; saveConfig(); }
    public double getFov() { return fov; }
    public void setFov(double fov) { this.fov = fov; saveConfig(); }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) return;
        
        // Ensure holding sword and pressing attack key
        if (!mc.player.getMainHandItem().getItem().getDescriptionId().toLowerCase().contains("sword")) return;
        if (!mc.options.keyAttack.isDown()) return;

        Entity target = getBestTarget();
        if (target != null) {
            aimAt(target);
        }
    }
    
    private Entity getBestTarget() {
        List<Entity> entities = mc.level.getEntities(mc.player, mc.player.getBoundingBox().inflate(6.0));
        return entities.stream()
            .filter(e -> e instanceof LivingEntity && e != mc.player && e.isAlive() && mc.player.distanceTo(e) <= 5.0)
            .filter(e -> isInFov(e, fov))
            .min(Comparator.comparingDouble(e -> yawDistanceTo(e)))
            .orElse(null);
    }

    private boolean isInFov(Entity entity, double fovLimit) {
        return yawDistanceTo(entity) <= fovLimit / 2.0;
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
        // Aim at the chest/head area roughly
        double diffY = (entity.getY() + entity.getEyeHeight() * 0.7) - (mc.player.getY() + mc.player.getEyeHeight());

        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);
        
        float targetYaw = (float) (Mth.atan2(diffZ, diffX) * (180F / Math.PI)) - 90.0F;
        float targetPitch = (float) -(Mth.atan2(diffY, dist) * (180F / Math.PI));
        
        // Add minimal noise
        targetYaw += (Math.random() - 0.5) * 1.5;
        targetPitch += (Math.random() - 0.5) * 1.5;

        // Smoothly interpolate
        float currentYaw = mc.player.getYRot();
        float currentPitch = mc.player.getXRot();
        
        float yawDiff = Mth.wrapDegrees(targetYaw - currentYaw);
        float pitchDiff = Mth.wrapDegrees(targetPitch - currentPitch);

        // Calculate steps based on smoothing
        float speedStr = (float) (11.0 - smoothing); // 1 to 10
        
        mc.player.setYRot(currentYaw + (yawDiff / speedStr));
        mc.player.setXRot(currentPitch + (pitchDiff / speedStr));
    }

    @Override
    public void loadConfig(Properties properties) {
        super.loadConfig(properties);
        String s = properties.getProperty("aim.smoothing");
        if (s != null) {
            try { this.smoothing = Double.parseDouble(s); } catch (Exception ignored) {}
        }
        String f = properties.getProperty("aim.fov");
        if (f != null) {
            try { this.fov = Double.parseDouble(f); } catch (Exception ignored) {}
        }
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("aim.smoothing", Double.toString(smoothing));
        properties.setProperty("aim.fov", Double.toString(fov));
    }
}
