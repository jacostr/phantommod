package com.phantom.module.impl.combat;

import com.phantom.PhantomMod;
import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import com.phantom.module.impl.player.AntiBot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Random;

/**
 * BowAimbot — Advanced projectile assistance for Bows.
 * 
 * Includes:
 * - FOV-priority targeting (feels like Aim Assist).
 * - Iterative physics simulation for perfect gravity/drag compensation.
 * - Legit smoothing with micro-jitter to bypass anti-cheats like Watchdog.
 * - Filters for Hypixel bots, spectators, and teammates.
 * - "Only Full Draw" mode for realistic behavior.
 */
public class BowAimbot extends Module {
    private double fov = 30.0;
    private double smoothing = 5.0;
    private double maxDistance = 60.0;
    private boolean predictMovement = true;
    private boolean verticalCorrection = true;
    private boolean playersOnly = true;
    private boolean visibilityCheck = true;
    private boolean requireMouseDown = true;
    private boolean teamCheck = true;
    private boolean onlyFullDraw = true;
    private boolean legitJitter = true;
    private AimbotPreset preset = AimbotPreset.MEDIUM;

    public enum AimbotPreset {
        CLOSE("Close", 60.0, 7.0, 25.0),
        MEDIUM("Medium", 30.0, 5.0, 60.0),
        FAR("Far", 15.0, 3.0, 100.0);

        private final String name;
        private final double fov;
        private final double smoothing;
        private final double distance;

        AimbotPreset(String name, double fov, double smoothing, double distance) {
            this.name = name;
            this.fov = fov;
            this.smoothing = smoothing;
            this.distance = distance;
        }

        public String getName() { return name; }
        public double getFov() { return fov; }
        public double getSmoothing() { return smoothing; }
        public double getDistance() { return distance; }
        public AimbotPreset next() { return values()[(ordinal() + 1) % values().length]; }
    }

    private final Random random = new Random();

    public BowAimbot() {
        super("BowAimbot", "High-accuracy projectile aim assistant.\nDetectability: Legit", 
              ModuleCategory.COMBAT, GLFW.GLFW_KEY_V);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) return;
        if (mc.screen != null) return;
        if (!isHoldingBow()) return;
        if (requireMouseDown && !isRightClickDown()) return;
        if (!mc.player.isUsingItem()) return;
        if (onlyFullDraw && mc.player.getTicksUsingItem() < 20) return;

        Entity target = getBestTarget();
        if (target != null) {
            aimAt(target);
        }
    }

    private boolean isHoldingBow() {
        return mc.player.getMainHandItem().getItem() instanceof BowItem || 
               mc.player.getOffhandItem().getItem() instanceof BowItem;
    }

    private boolean isRightClickDown() {
        return mc.options.keyUse.isDown() || (mc.mouseHandler != null && mc.mouseHandler.isRightPressed());
    }

    /**
     * Finds the target that is closest to the player's crosshair (FOV priority).
     */
    private Entity getBestTarget() {
        List<Entity> entities = mc.level.getEntities(mc.player, mc.player.getBoundingBox().inflate(maxDistance));
        return entities.stream()
            .filter(this::isValidTarget)
            .min(Comparator.comparingDouble(e -> {
                double fovOffset = getFovOffset(e);
                double distance = mc.player.distanceTo(e);
                // Weighted score: FOV offset is primary, distance is secondary
                return (fovOffset * 5.0) + distance;
            }))
            .filter(e -> getFovOffset(e) <= fov / 2.0)
            .orElse(null);
    }

    private double getFovOffset(Entity entity) {
        Vec3 targetPos = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
        double diffX = targetPos.x - mc.player.getX();
        double diffZ = targetPos.z - mc.player.getZ();
        float yaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0D);
        float wrapYaw = Mth.wrapDegrees(yaw - mc.player.getYRot());
        
        double diffY = targetPos.y - (mc.player.getY() + mc.player.getEyeHeight());
        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, dist));
        float wrapPitch = Mth.wrapDegrees(pitch - mc.player.getXRot());
        
        return Math.sqrt(wrapYaw * wrapYaw + wrapPitch * wrapPitch);
    }

    private boolean isValidTarget(Entity entity) {
        if (!(entity instanceof LivingEntity living) || entity == mc.player || !living.isAlive()) return false;
        if (entity.isSpectator()) return false;
        if (playersOnly && !(entity instanceof Player)) return false;
        if (teamCheck && isTeammateTarget(entity)) return false;
        if (AntiBot.isBot(entity)) return false;
        if (visibilityCheck && !mc.player.hasLineOfSight(entity)) return false;
        return true;
    }

    private void aimAt(Entity target) {
        float velocity = getProjectileVelocity();
        if (velocity < 0.1F) return;

        Vec3 targetPos = target.position().add(0, target.getBbHeight() * 0.5, 0);
        double diffX = targetPos.x - mc.player.getX();
        double diffZ = targetPos.z - mc.player.getZ();
        double hDist = Math.sqrt(diffX * diffX + diffZ * diffZ);
        double diffY = targetPos.y - (mc.player.getY() + mc.player.getEyeHeight());

        // Pass 1: Find how long it takes to reach current target location
        SimulationResult initial = simulateForBestPitch(hDist, diffY, velocity);
        
        // Prediction logic: Adjust target position based on exact flight ticks
        if (predictMovement) {
            Vec3 motion = target.getDeltaMovement();
            targetPos = targetPos.add(motion.x * initial.ticks, motion.y * initial.ticks, motion.z * initial.ticks);
            
            // Re-calculate diffs for new predicted position
            diffX = targetPos.x - mc.player.getX();
            diffZ = targetPos.z - mc.player.getZ();
            hDist = Math.sqrt(diffX * diffX + diffZ * diffZ);
            diffY = targetPos.y - (mc.player.getY() + mc.player.getEyeHeight());
        }

        // Pass 2: Find the final perfect pitch for the predicted position
        SimulationResult finalResult = simulateForBestPitch(hDist, diffY, velocity);

        float targetYaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0D);
        float targetPitch = (float) finalResult.pitch;

        if (legitJitter) {
            targetYaw += (random.nextFloat() - 0.5f) * 0.75f;
            targetPitch += (random.nextFloat() - 0.5f) * 0.75f;
        }

        smoothAim(targetYaw, Mth.clamp(targetPitch, -90.0f, 90.0f));
    }

    private SimulationResult simulateForBestPitch(double hDist, double diffY, float velocity) {
        double initialPitch = -Math.toDegrees(Math.atan2(diffY, hDist));
        if (!verticalCorrection) return new SimulationResult(initialPitch, 0, (int)(hDist / (velocity * 0.99)));

        double gravity = 0.05;
        double drag = 0.99;
        
        // 10 Iterations for "Sniper" level convergence
        double currentPitch = initialPitch;
        int ticks = 0;
        
        for (int i = 0; i < 10; i++) {
            SimulationResult res = simulateProjectile(hDist, currentPitch, velocity, gravity, drag);
            double error = diffY - res.finalY;
            currentPitch -= Math.toDegrees(Math.atan2(error, hDist)) * 0.6; // Slightly more aggressive convergence
            ticks = res.ticks;
        }
        
        return new SimulationResult(currentPitch, 0, ticks);
    }

    private SimulationResult simulateProjectile(double hDist, double pitch, float velocity, double gravity, double drag) {
        double vX = Math.cos(Math.toRadians(pitch)) * velocity;
        double vY = -Math.sin(Math.toRadians(pitch)) * velocity;
        double x = 0;
        double y = 0;
        int ticks = 0;
        
        while (x < hDist && ticks < 200) {
            x += vX;
            y += vY;
            vX *= drag;
            vY *= drag;
            vY -= gravity;
            ticks++;
        }
        return new SimulationResult(0, y, ticks);
    }

    private static class SimulationResult {
        final double pitch;
        final double finalY;
        final int ticks;

        SimulationResult(double pitch, double finalY, int ticks) {
            this.pitch = pitch;
            this.finalY = finalY;
            this.ticks = ticks;
        }
    }

    private float getProjectileVelocity() {
        int useCount = mc.player.getTicksUsingItem();
        float f = (float)useCount / 20.0F;
        f = (f * f + f * 2.0F) / 3.0F;
        if (f > 1.0F) f = 1.0F;
        return f * 3.0F;
    }

    private void smoothAim(float targetYaw, float targetPitch) {
        float currentYaw = mc.player.getYRot();
        float currentPitch = mc.player.getXRot();

        float diffYaw = Mth.wrapDegrees(targetYaw - currentYaw);
        float diffPitch = targetPitch - currentPitch;

        double clampedSmoothing = Math.max(1.0, smoothing);
        float stepYaw = (float) (diffYaw / clampedSmoothing);
        float stepPitch = (float) (diffPitch / clampedSmoothing);

        mc.player.setYRot(currentYaw + stepYaw);
        mc.player.setYHeadRot(mc.player.getYRot());
        mc.player.setXRot(Mth.clamp(currentPitch + stepPitch, -90.0f, 90.0f));
    }

    @Override
    public boolean hasConfigurableSettings() { return true; }

    @Override
    public Screen createSettingsScreen(Screen parent) { return new ModuleSettingsScreen(parent, this); }

    @Override
    public void loadConfig(Properties props) {
        super.loadConfig(props);
        try {
            preset = AimbotPreset.valueOf(props.getProperty("bowaim.preset", "MEDIUM"));
        } catch (IllegalArgumentException ignored) {
            preset = AimbotPreset.MEDIUM;
        }
        fov = parseDouble(props, "bowaim.fov", preset.getFov(), 1.0, 360.0);
        smoothing = parseDouble(props, "bowaim.smoothing", preset.getSmoothing(), 1.0, 10.0);
        maxDistance = parseDouble(props, "bowaim.maxdist", preset.getDistance(), 10.0, 100.0);
        predictMovement = Boolean.parseBoolean(props.getProperty("bowaim.predict", "true"));
        verticalCorrection = Boolean.parseBoolean(props.getProperty("bowaim.vertical", "true"));
        playersOnly = Boolean.parseBoolean(props.getProperty("bowaim.players", "true"));
        visibilityCheck = Boolean.parseBoolean(props.getProperty("bowaim.visibility", "true"));
        requireMouseDown = Boolean.parseBoolean(props.getProperty("bowaim.mousedown", "true"));
        teamCheck = Boolean.parseBoolean(props.getProperty("bowaim.team", "true"));
        onlyFullDraw = Boolean.parseBoolean(props.getProperty("bowaim.fulldraw", "true"));
        legitJitter = Boolean.parseBoolean(props.getProperty("bowaim.jitter", "true"));
    }

    @Override
    public void saveConfig(Properties props) {
        super.saveConfig(props);
        props.setProperty("bowaim.preset", preset.name());
        props.setProperty("bowaim.fov", Double.toString(fov));
        props.setProperty("bowaim.smoothing", Double.toString(smoothing));
        props.setProperty("bowaim.maxdist", Double.toString(maxDistance));
        props.setProperty("bowaim.predict", Boolean.toString(predictMovement));
        props.setProperty("bowaim.vertical", Boolean.toString(verticalCorrection));
        props.setProperty("bowaim.players", Boolean.toString(playersOnly));
        props.setProperty("bowaim.visibility", Boolean.toString(visibilityCheck));
        props.setProperty("bowaim.mousedown", Boolean.toString(requireMouseDown));
        props.setProperty("bowaim.team", Boolean.toString(teamCheck));
        props.setProperty("bowaim.fulldraw", Boolean.toString(onlyFullDraw));
        props.setProperty("bowaim.jitter", Boolean.toString(legitJitter));
    }

    public double getFov() { return fov; }
    public void setFov(double v) { fov = Mth.clamp(v, 1.0, 360.0); saveConfig(); }
    public double getSmoothing() { return smoothing; }
    public void setSmoothing(double v) { smoothing = Mth.clamp(v, 1.0, 10.0); saveConfig(); }
    public double getMaxDistance() { return maxDistance; }
    public void setMaxDistance(double v) { maxDistance = Mth.clamp(v, 10.0, 100.0); saveConfig(); }
    public boolean isPredictMovement() { return predictMovement; }
    public void setPredictMovement(boolean v) { predictMovement = v; saveConfig(); }
    public boolean isVerticalCorrection() { return verticalCorrection; }
    public void setVerticalCorrection(boolean v) { verticalCorrection = v; saveConfig(); }
    public boolean isPlayersOnly() { return playersOnly; }
    public void setPlayersOnly(boolean v) { playersOnly = v; saveConfig(); }
    public boolean isVisibilityCheck() { return visibilityCheck; }
    public void setVisibilityCheck(boolean v) { visibilityCheck = v; saveConfig(); }
    public boolean isRequireMouseDown() { return requireMouseDown; }
    public void setRequireMouseDown(boolean v) { requireMouseDown = v; saveConfig(); }
    public boolean isTeamCheck() { return teamCheck; }
    public void setTeamCheck(boolean v) { teamCheck = v; saveConfig(); }
    public boolean isOnlyFullDraw() { return onlyFullDraw; }
    public void setOnlyFullDraw(boolean v) { onlyFullDraw = v; saveConfig(); }
    public boolean isLegitJitter() { return legitJitter; }
    public void setLegitJitter(boolean v) { legitJitter = v; saveConfig(); }

    public AimbotPreset getPreset() { return preset; }
    public void cyclePreset() {
        this.preset = this.preset.next();
        this.fov = preset.getFov();
        this.smoothing = preset.getSmoothing();
        this.maxDistance = preset.getDistance();
        saveConfig();
    }

    private double parseDouble(Properties props, String key, double fallback, double min, double max) {
        try {
            return Mth.clamp(Double.parseDouble(props.getProperty(key, Double.toString(fallback))), min, max);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
