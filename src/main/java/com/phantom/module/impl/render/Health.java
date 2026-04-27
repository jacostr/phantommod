/* Copyright (c) 2026 PhantomMod. All rights reserved. */
/*
 * Health.java — Custom health rendering as a vertical bar beside entities.
 */
package com.phantom.module.impl.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import com.phantom.module.impl.player.AntiBot;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

import java.util.Properties;

public class Health extends Module {
    private boolean throughWalls = true;
    private double barScale = 2.0;
    private boolean showInvisible = true;
    private boolean targetPlayers = true;
    private boolean showBar = true;
    private boolean showAbsorption = true;
    private double maxDistance = 64.0;

    public Health() {
        super("Health",
                "Displays opponent health as a vertical side bar next to their name.\nDetectability: Safe",
                ModuleCategory.RENDER, -1);
    }

    @Override
    public void onRender(WorldRenderContext context) {
        if (mc.player == null || mc.level == null || !targetPlayers) {
            return;
        }

        MultiBufferSource consumers = context.consumers();
        PoseStack matrices = context.matrices();
        if (consumers == null || matrices == null) {
            return;
        }

        if (!showBar) {
            return;
        }

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.position();
        double rangeSq = maxDistance * maxDistance;

        if (throughWalls) {
            org.lwjgl.opengl.GL11.glDepthFunc(org.lwjgl.opengl.GL11.GL_ALWAYS);
        }

        try {
            VertexConsumer lineBuffer = consumers.getBuffer(RenderTypes.lines());

            for (Player player : mc.level.players()) {
                if (!shouldRender(player, cameraPos, rangeSq)) {
                    continue;
                }

                renderBillboardedHealthBar(matrices, lineBuffer, camera, cameraPos, player);
            }

            if (consumers instanceof MultiBufferSource.BufferSource bufferSource) {
                bufferSource.endBatch(RenderTypes.lines());
            }
        } finally {
            if (throughWalls) {
                org.lwjgl.opengl.GL11.glDepthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
            }
        }
    }

    private boolean shouldRender(Player player, Vec3 cameraPos, double rangeSq) {
        if (player == mc.player || !player.isAlive()) {
            return false;
        }
        if (!showInvisible && player.isInvisible()) {
            return false;
        }
        if (AntiBot.isBot(player)) {
            return false;
        }

        double dx = player.getX() - cameraPos.x;
        double dy = player.getY() + player.getBbHeight() - cameraPos.y;
        double dz = player.getZ() - cameraPos.z;
        return dx * dx + dy * dy + dz * dz <= rangeSq;
    }

    private void renderBillboardedHealthBar(PoseStack poseStack, VertexConsumer buffer,
                                            Camera camera, Vec3 cameraPos, LivingEntity entity) {
        double dx = entity.getX() - cameraPos.x;
        double dy = entity.getY() - cameraPos.y;
        double dz = entity.getZ() - cameraPos.z;

        float maxHealth = Math.max(1.0f, entity.getMaxHealth());
        float healthRatio = Mth.clamp(entity.getHealth() / maxHealth, 0.0f, 1.0f);
        float height = entity.getBbHeight();

        float activeWidth = (float) barScale;
        float bgWidth = activeWidth + 1.0f;

        poseStack.pushPose();
        poseStack.translate(dx, dy + (height / 2.0), dz);
        poseStack.mulPose(camera.rotation());

        float xOffset = (entity.getBbWidth() / 2.0f) + 0.15f;
        poseStack.translate(-xOffset, -height / 2.0, 0);

        Matrix4f matrix = poseStack.last().pose();
        int bgColor = 0xFF000000;
        int healthColor = getHealthTintedColor(entity, 0xFF00FF00);

        drawVerticalLine(matrix, buffer, 0, height, bgColor, bgWidth);
        drawVerticalLine(matrix, buffer, 0, height * healthRatio, healthColor, activeWidth);

        poseStack.popPose();
    }

    private void drawVerticalLine(Matrix4f matrix, VertexConsumer buffer, float yStart, float yEnd, int color, float width) {
        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        buffer.addVertex(matrix, 0, yStart, 0).setColor(r, g, b, a).setNormal(0, 1, 0).setLineWidth(width);
        buffer.addVertex(matrix, 0, yEnd, 0).setColor(r, g, b, a).setNormal(0, 1, 0).setLineWidth(width);
    }

    private int getHealthTintedColor(LivingEntity entity, int fallback) {
        float maxHealth = entity.getMaxHealth();
        if (maxHealth <= 0) {
            return fallback;
        }
        float ratio = Mth.clamp(entity.getHealth() / maxHealth, 0.0f, 1.0f);
        int r;
        int g;
        if (ratio > 0.5f) {
            float t = (ratio - 0.5f) * 2.0f;
            r = (int) (255 * (1.0f - t));
            g = 255;
        } else {
            float t = ratio * 2.0f;
            r = 255;
            g = (int) (255 * t);
        }
        return 0xFF000000 | (r << 16) | (g << 8);
    }

    @Override
    public boolean hasConfigurableSettings() {
        return true;
    }

    @Override
    public Screen createSettingsScreen(Screen parent) {
        return new ModuleSettingsScreen(parent, this);
    }

    public boolean isThroughWalls() { return throughWalls; }
    public void setThroughWalls(boolean v) { throughWalls = v; saveConfig(); }
    public double getBarScale() { return barScale; }
    public void setBarScale(double v) { barScale = Mth.clamp(v, 0.5, 10.0); saveConfig(); }
    public boolean isShowInvisible() { return showInvisible; }
    public void setShowInvisible(boolean v) { showInvisible = v; saveConfig(); }
    public boolean isTargetPlayers() { return targetPlayers; }
    public void setTargetPlayers(boolean v) { targetPlayers = v; saveConfig(); }
    public boolean isShowBar() { return showBar; }
    public void setShowBar(boolean v) { showBar = v; saveConfig(); }
    public boolean isShowAbsorption() { return showAbsorption; }
    public void setShowAbsorption(boolean v) { showAbsorption = v; saveConfig(); }
    public double getMaxDistance() { return maxDistance; }
    public void setMaxDistance(double v) { maxDistance = Mth.clamp(v, 16.0, 160.0); saveConfig(); }

    @Override
    public void loadConfig(Properties properties) {
        super.loadConfig(properties);
        throughWalls = Boolean.parseBoolean(properties.getProperty("health.through_walls", Boolean.toString(throughWalls)));
        showInvisible = Boolean.parseBoolean(properties.getProperty("health.show_invisible", Boolean.toString(showInvisible)));
        targetPlayers = Boolean.parseBoolean(properties.getProperty("health.target_players", Boolean.toString(targetPlayers)));
        showBar = Boolean.parseBoolean(properties.getProperty("health.show_bar", Boolean.toString(showBar)));
        showAbsorption = Boolean.parseBoolean(properties.getProperty("health.show_absorption", Boolean.toString(showAbsorption)));

        try {
            barScale = Mth.clamp(Double.parseDouble(properties.getProperty("health.bar_scale", Double.toString(barScale))), 0.5, 10.0);
        } catch (NumberFormatException ignored) {}
        try {
            maxDistance = Mth.clamp(Double.parseDouble(properties.getProperty("health.max_distance", Double.toString(maxDistance))), 16.0, 160.0);
        } catch (NumberFormatException ignored) {}
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("health.through_walls", Boolean.toString(throughWalls));
        properties.setProperty("health.show_invisible", Boolean.toString(showInvisible));
        properties.setProperty("health.target_players", Boolean.toString(targetPlayers));
        properties.setProperty("health.show_bar", Boolean.toString(showBar));
        properties.setProperty("health.show_absorption", Boolean.toString(showAbsorption));
        properties.setProperty("health.bar_scale", Double.toString(barScale));
        properties.setProperty("health.max_distance", Double.toString(maxDistance));
    }
}
