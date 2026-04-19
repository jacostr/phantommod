/* Copyright (c) 2026 PhantomMod. All rights reserved. */
package com.phantom.module.impl.render;

import com.phantom.PhantomMod;
import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import com.phantom.module.impl.player.AntiBot;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Properties;

public class Nametags extends Module {
    private double range = 64.0;
    private double scale = 1.0;
    private boolean showOwnNameTag = true;
    private boolean distanceScaling = true;
    private boolean showHealth = true;
    private boolean showDistance = true;
    private boolean showInvisible = true;
    private boolean showAnimals = true;
    private boolean showMobs = true;
    private boolean showPlayers = true;

    public Nametags() {
        super("Nametags",
                "Renders custom name tags above entities with health and distance.\nDetectability: Safe",
                ModuleCategory.RENDER,
                -1);
    }

    @Override
    public void onRender(WorldRenderContext context) {
        if (mc.player == null || mc.level == null) return;
        if (context.matrices() == null) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        if (camera == null) return;

        Vec3 camPos = camera.position();
        double rangeSq = range * range;

        for (LivingEntity entity : mc.level.getEntitiesOfClass(LivingEntity.class,
                mc.player.getBoundingBox().inflate(range), e -> e instanceof LivingEntity && e.isAlive())) {

            if (!shouldRender(entity)) continue;

            double distSq = mc.player.distanceToSqr(entity);
            if (distSq > rangeSq || distSq < 0.5) continue;

            String name = entity.getName().getString();
            if (name == null || name.isEmpty() || name.equals("null")) continue;

            MutableComponent text = Component.literal(name);

            if (showHealth && entity instanceof LivingEntity living) {
                float health = living.getHealth();
                float maxHealth = living.getMaxHealth();
                if (maxHealth > 0 && health > 0) {
                    ChatFormatting color;
                    float ratio = health / maxHealth;
                    if (ratio > 0.75f) color = ChatFormatting.GREEN;
                    else if (ratio > 0.5f) color = ChatFormatting.YELLOW;
                    else if (ratio > 0.25f) color = ChatFormatting.GOLD;
                    else color = ChatFormatting.RED;
                    text.append(Component.literal(" " + color + (int) Math.ceil(health) + "\u2764"));
                }
            }

            if (showDistance) {
                int meters = Mth.floor(Math.sqrt(distSq));
                text.append(Component.literal(" " + ChatFormatting.GRAY + meters + "m"));
            }

            Vec3 pos = new Vec3(entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ());
            double y = pos.y + entity.getBbHeight() + 0.5;

            renderNametag(context, text, pos.x, y, pos.z, camPos, camera, distSq);
        }
    }

    private void renderNametag(WorldRenderContext context, Component text, double x, double y, double z, Vec3 camPos, Camera camera, double distSq) {
        double dx = x - camPos.x;
        double dy = y - camPos.y;
        double dz = z - camPos.z;

        double rotY = Math.toRadians(camera.yRot() + 180);
        double rotP = Math.toRadians(camera.xRot());

        double cY = Math.cos(rotY);
        double sY = Math.sin(rotY);
        double cP = Math.cos(rotP);
        double sP = Math.sin(rotP);

        double rx = dx * cY + dz * sY;
        double rz = -dx * sY + dz * cY;
        double ry = dy * cP - rz * sP;
        double rz2 = dy * sP + rz * cP;

        if (rz2 <= 0.3) return;

        float fov = mc.options.fov().get().floatValue();
        double fovFactor = Math.tan(Math.toRadians(fov) / 2.0);

        double aspect = (double) mc.getWindow().getGuiScaledWidth() / mc.getWindow().getGuiScaledHeight();
        double projX = (rx / rz2 / fovFactor / aspect) * 0.5 + 0.5;
        double projY = (-ry / rz2 / fovFactor) * 0.5 + 0.5;

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        float screenX = (float) (projX * screenW);
        float screenY = (float) (projY * screenH);

        if (screenX < -50 || screenX > screenW + 50 || screenY < -50 || screenY > screenH + 50) return;

        float tagScale = (float) (0.025 * scale);
        if (distanceScaling) {
            tagScale *= Math.max(1.0f, (float) Math.sqrt(distSq) / 15.0f);
        }

        context.matrices().pushPose();
        context.matrices().translate(screenX, screenY, 0);
        context.matrices().scale(tagScale, tagScale, tagScale);

        net.minecraft.util.FormattedCharSequence seq = text.getVisualOrderText();
        float textWidth = mc.font.width(seq);

        mc.font.drawInBatch(seq, -textWidth / 2.0f, 0.0f, 0xFFFFFFFF, true,
                context.matrices().last().pose(), context.consumers(),
                net.minecraft.client.gui.Font.DisplayMode.POLYGON_OFFSET, 0, 0xFFFFFFFF);

        context.matrices().popPose();
    }

    private boolean shouldRender(LivingEntity entity) {
        if (entity == null || !entity.isAlive()) return false;

        if (entity == mc.player) return showOwnNameTag;
        if (!showInvisible && entity.isInvisible()) return false;

        if (entity instanceof Player player) {
            return showPlayers && !AntiBot.isBot(player);
        }

        if (entity instanceof net.minecraft.world.entity.animal.Animal
                || entity instanceof net.minecraft.world.entity.ambient.AmbientCreature) {
            return showAnimals;
        }

        if (entity instanceof net.minecraft.world.entity.Mob) {
            return showMobs;
        }

        return false;
    }

    public static boolean shouldHideVanillaNametag(net.minecraft.world.entity.Entity entity) {
        if (!(entity instanceof Player)) return false;
        if (PhantomMod.getModuleManager() == null) return false;
        Nametags module = PhantomMod.getModuleManager().getModuleByClass(Nametags.class);
        return module != null && module.isEnabled();
    }

    @Override public boolean hasConfigurableSettings() { return true; }
    @Override public Screen createSettingsScreen(Screen parent) { return new ModuleSettingsScreen(parent, this); }

    public double getRange() { return range; }
    public void setRange(double v) { range = Mth.clamp(v, 16.0, 160.0); saveConfig(); }
    public double getScale() { return scale; }
    public void setScale(double v) { scale = Mth.clamp(v, 0.5, 3.0); saveConfig(); }
    public boolean isShowOwnNameTag() { return showOwnNameTag; }
    public void setShowOwnNameTag(boolean v) { showOwnNameTag = v; saveConfig(); }
    public boolean isDistanceScaling() { return distanceScaling; }
    public void setDistanceScaling(boolean v) { distanceScaling = v; saveConfig(); }
    public boolean isShowHealth() { return showHealth; }
    public void setShowHealth(boolean v) { showHealth = v; saveConfig(); }
    public boolean isShowDistance() { return showDistance; }
    public void setShowDistance(boolean v) { showDistance = v; saveConfig(); }
    public boolean isShowInvisible() { return showInvisible; }
    public void setShowInvisible(boolean v) { showInvisible = v; saveConfig(); }
    public boolean isShowAnimals() { return showAnimals; }
    public void setShowAnimals(boolean v) { showAnimals = v; saveConfig(); }
    public boolean isShowMobs() { return showMobs; }
    public void setShowMobs(boolean v) { showMobs = v; saveConfig(); }
    public boolean isShowPlayers() { return showPlayers; }
    public void setShowPlayers(boolean v) { showPlayers = v; saveConfig(); }

    @Override
    public void loadConfig(Properties props) {
        super.loadConfig(props);
        try { range = Mth.clamp(Double.parseDouble(props.getProperty("nametags.range", "64.0")), 16.0, 160.0); } catch (Exception ignored) {}
        try { scale = Mth.clamp(Double.parseDouble(props.getProperty("nametags.scale", "1.0")), 0.5, 3.0); } catch (Exception ignored) {}
        showOwnNameTag = Boolean.parseBoolean(props.getProperty("nametags.show_own", "true"));
        distanceScaling = Boolean.parseBoolean(props.getProperty("nametags.distance_scaling", "true"));
        showHealth = Boolean.parseBoolean(props.getProperty("nametags.show_health", "true"));
        showDistance = Boolean.parseBoolean(props.getProperty("nametags.show_distance", "true"));
        showInvisible = Boolean.parseBoolean(props.getProperty("nametags.show_invisible", "true"));
        showAnimals = Boolean.parseBoolean(props.getProperty("nametags.show_animals", "true"));
        showMobs = Boolean.parseBoolean(props.getProperty("nametags.show_mobs", "true"));
        showPlayers = Boolean.parseBoolean(props.getProperty("nametags.show_players", "true"));
    }

    @Override
    public void saveConfig(Properties props) {
        super.saveConfig(props);
        props.setProperty("nametags.range", Double.toString(range));
        props.setProperty("nametags.scale", Double.toString(scale));
        props.setProperty("nametags.show_own", Boolean.toString(showOwnNameTag));
        props.setProperty("nametags.distance_scaling", Boolean.toString(distanceScaling));
        props.setProperty("nametags.show_health", Boolean.toString(showHealth));
        props.setProperty("nametags.show_distance", Boolean.toString(showDistance));
        props.setProperty("nametags.show_invisible", Boolean.toString(showInvisible));
        props.setProperty("nametags.show_animals", Boolean.toString(showAnimals));
        props.setProperty("nametags.show_mobs", Boolean.toString(showMobs));
        props.setProperty("nametags.show_players", Boolean.toString(showPlayers));
    }
}