/* Copyright (c) 2026 PhantomMod. All rights reserved. */
package com.phantom.module.impl.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.phantom.PhantomMod;
import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import com.phantom.module.impl.player.AntiBot;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Properties;

public class Nametags extends Module {
    private double range = 96.0;
    private double scale = 1.0;
    private boolean showOwnNameTag;
    private boolean distanceScaling = true;
    private boolean background = true;
    private boolean textShadow = true;
    private boolean showHealth = true;
    private boolean showDistance;
    private boolean showInvisible = true;
    private boolean showAnimals = true;
    private boolean showMobs = true;

    public Nametags() {
        super("Nametags",
                "Renders larger custom name tags with optional health and distance info.\nDetectability: Safe",
                ModuleCategory.RENDER,
                -1);
    }

    @Override
    public void onRender(WorldRenderContext context) {
        if (mc.player == null || mc.level == null)
            return;

        PoseStack matrices = context.matrices();
        if (matrices == null)
            return;

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.position();
        double rangeSq = range * range;
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

        // Use AABB query so mobs/animals behind the camera are not frustum-culled.
        // Always manually add the local player for third-person rendering.
        java.util.List<LivingEntity> entities = new java.util.ArrayList<>(
                mc.level.getEntitiesOfClass(LivingEntity.class,
                        mc.player.getBoundingBox().inflate(range)));
        if (!entities.contains(mc.player)) {
            entities.add(mc.player);
        }

        for (LivingEntity living : entities) {
            if (!shouldRender(living))
                continue;

            // xOld/yOld/zOld are unreliable for the local player — use current pos directly
            double ix, iy, iz;
            if (living == mc.player) {
                ix = living.getX();
                iy = living.getY();
                iz = living.getZ();
            } else {
                ix = Mth.lerp(partialTick, living.xOld, living.getX());
                iy = Mth.lerp(partialTick, living.yOld, living.getY());
                iz = Mth.lerp(partialTick, living.zOld, living.getZ());
            }

            double dx = ix - cameraPos.x;
            double dy = iy + living.getBbHeight() + 0.55 - cameraPos.y;
            double dz = iz - cameraPos.z;
            double distanceSq = dx * dx + dy * dy + dz * dz;
            if (distanceSq > rangeSq)
                continue;

            MutableComponent text = living.getDisplayName().copy();
            if (showHealth) {
                text.append(buildHealthComponent(living));
            }
            if (showDistance) {
                text.append(Component.literal(" [" + Mth.floor(Math.sqrt(distanceSq)) + "m]")
                        .withStyle(ChatFormatting.GRAY));
            }

            renderTag(matrices, bufferSource, camera, text, dx, dy, dz, Math.sqrt(distanceSq));
        }

        bufferSource.endBatch();
    }

    private boolean shouldRender(LivingEntity entity) {
        if (entity == null || !entity.isAlive())
            return false;

        // Own nametag — only visible in third-person, skip all other checks
        if (entity == mc.player) {
            return showOwnNameTag && !mc.options.getCameraType().isFirstPerson();
        }

        if (!showInvisible && entity.isInvisible())
            return false;

        // Other players (filter bots)
        if (entity instanceof Player player) {
            return !AntiBot.isBot(player);
        }

        // Animals & ambient creatures.
        // WaterAnimal does not exist in this MC version — AbstractFish extends Animal
        // anyway.
        if (entity instanceof net.minecraft.world.entity.animal.Animal
                || entity instanceof net.minecraft.world.entity.ambient.AmbientCreature) {
            return showAnimals;
        }

        // Everything else with AI (zombies, skeletons, golems, etc.)
        // Monster extends Mob so one check covers both.
        if (entity instanceof net.minecraft.world.entity.Mob) {
            return showMobs;
        }

        return false;
    }

    private void renderTag(PoseStack matrices, MultiBufferSource consumers, Camera camera, Component text,
            double dx, double dy, double dz, double distance) {
        matrices.pushPose();
        matrices.translate(dx, dy, dz);
        matrices.mulPose(camera.rotation());

        float worldScale = 0.025f * (float) scale;
        if (distanceScaling) {
            worldScale *= (float) Math.max(1.0, distance / 8.0);
        }
        matrices.scale(-worldScale, -worldScale, worldScale);

        FormattedCharSequence line = text.getVisualOrderText();
        float textX = -mc.font.width(line) / 2.0f;
        int backgroundColor = background
                ? (int) (mc.options.getBackgroundOpacity(0.25f) * 255.0f) << 24
                : 0;
        Matrix4f matrix = matrices.last().pose();

        mc.font.drawInBatch(
                line, textX, 0.0f, 0xFFFFFFFF, textShadow, matrix, consumers,
                Font.DisplayMode.SEE_THROUGH, backgroundColor, LightTexture.FULL_BRIGHT);

        matrices.popPose();
    }

    private Component buildHealthComponent(LivingEntity entity) {
        float health = Math.max(0f, entity.getHealth());
        float maxHealth = Math.max(1f, entity.getMaxHealth());
        float ratio = health / maxHealth;

        ChatFormatting color;
        if (ratio > 0.75f)
            color = ChatFormatting.GREEN;
        else if (ratio > 0.50f)
            color = ChatFormatting.YELLOW;
        else if (ratio > 0.25f)
            color = ChatFormatting.GOLD;
        else
            color = ChatFormatting.RED;

        return Component.literal(" " + fastOneDecimal(health / 2.0f) + " \u2764").withStyle(color);
    }

    private String fastOneDecimal(float value) {
        int tenths = Math.round(value * 10f);
        int intPart = tenths / 10;
        int fracPart = Math.abs(tenths % 10);
        return fracPart == 0 ? String.valueOf(intPart) : intPart + "." + fracPart;
    }

    public static boolean shouldHideVanillaNametag(net.minecraft.world.entity.Entity entity) {
        if (!(entity instanceof Player))
            return false;
        if (PhantomMod.getModuleManager() == null)
            return false;
        Nametags module = PhantomMod.getModuleManager().getModuleByClass(Nametags.class);
        return module != null && module.isEnabled();
    }

    @Override
    public boolean hasConfigurableSettings() {
        return true;
    }

    @Override
    public Screen createSettingsScreen(Screen parent) {
        return new ModuleSettingsScreen(parent, this);
    }

    public double getRange() {
        return range;
    }

    public void setRange(double v) {
        this.range = Mth.clamp(v, 16.0, 160.0);
        saveConfig();
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double v) {
        this.scale = Mth.clamp(v, 0.5, 3.0);
        saveConfig();
    }

    public boolean isShowOwnNameTag() {
        return showOwnNameTag;
    }

    public void setShowOwnNameTag(boolean v) {
        this.showOwnNameTag = v;
        saveConfig();
    }

    public boolean isDistanceScaling() {
        return distanceScaling;
    }

    public void setDistanceScaling(boolean v) {
        this.distanceScaling = v;
        saveConfig();
    }

    public boolean isBackground() {
        return background;
    }

    public void setBackground(boolean v) {
        this.background = v;
        saveConfig();
    }

    public boolean isTextShadow() {
        return textShadow;
    }

    public void setTextShadow(boolean v) {
        this.textShadow = v;
        saveConfig();
    }

    public boolean isShowHealth() {
        return showHealth;
    }

    public void setShowHealth(boolean v) {
        this.showHealth = v;
        saveConfig();
    }

    public boolean isShowDistance() {
        return showDistance;
    }

    public void setShowDistance(boolean v) {
        this.showDistance = v;
        saveConfig();
    }

    public boolean isShowInvisible() {
        return showInvisible;
    }

    public void setShowInvisible(boolean v) {
        this.showInvisible = v;
        saveConfig();
    }

    public boolean isShowAnimals() {
        return showAnimals;
    }

    public void setShowAnimals(boolean v) {
        this.showAnimals = v;
        saveConfig();
    }

    public boolean isShowMobs() {
        return showMobs;
    }

    public void setShowMobs(boolean v) {
        this.showMobs = v;
        saveConfig();
    }

    @Override
    public void loadConfig(Properties props) {
        super.loadConfig(props);
        try {
            range = Mth.clamp(Double.parseDouble(props.getProperty("nametags.range", "96.0")), 16.0, 160.0);
        } catch (NumberFormatException ignored) {
        }
        try {
            scale = Mth.clamp(Double.parseDouble(props.getProperty("nametags.scale", "1.0")), 0.5, 3.0);
        } catch (NumberFormatException ignored) {
        }
        showOwnNameTag = Boolean.parseBoolean(props.getProperty("nametags.show_own", Boolean.toString(showOwnNameTag)));
        distanceScaling = Boolean
                .parseBoolean(props.getProperty("nametags.distance_scaling", Boolean.toString(distanceScaling)));
        background = Boolean.parseBoolean(props.getProperty("nametags.background", Boolean.toString(background)));
        textShadow = Boolean.parseBoolean(props.getProperty("nametags.text_shadow", Boolean.toString(textShadow)));
        showHealth = Boolean.parseBoolean(props.getProperty("nametags.show_health", Boolean.toString(showHealth)));
        showDistance = Boolean
                .parseBoolean(props.getProperty("nametags.show_distance", Boolean.toString(showDistance)));
        showInvisible = Boolean
                .parseBoolean(props.getProperty("nametags.show_invisible", Boolean.toString(showInvisible)));
        showAnimals = Boolean.parseBoolean(props.getProperty("nametags.show_animals", Boolean.toString(showAnimals)));
        showMobs = Boolean.parseBoolean(props.getProperty("nametags.show_mobs", Boolean.toString(showMobs)));
    }

    @Override
    public void saveConfig(Properties props) {
        super.saveConfig(props);
        props.setProperty("nametags.range", Double.toString(range));
        props.setProperty("nametags.scale", Double.toString(scale));
        props.setProperty("nametags.show_own", Boolean.toString(showOwnNameTag));
        props.setProperty("nametags.distance_scaling", Boolean.toString(distanceScaling));
        props.setProperty("nametags.background", Boolean.toString(background));
        props.setProperty("nametags.text_shadow", Boolean.toString(textShadow));
        props.setProperty("nametags.show_health", Boolean.toString(showHealth));
        props.setProperty("nametags.show_distance", Boolean.toString(showDistance));
        props.setProperty("nametags.show_invisible", Boolean.toString(showInvisible));
        props.setProperty("nametags.show_animals", Boolean.toString(showAnimals));
        props.setProperty("nametags.show_mobs", Boolean.toString(showMobs));
    }
}