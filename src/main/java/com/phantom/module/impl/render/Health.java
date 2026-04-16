/*
 * Health.java — Custom health rendering beside entities.
 */
package com.phantom.module.impl.render;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import com.phantom.module.impl.player.AntiBot;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

import java.util.Properties;

public class Health extends Module {

    private boolean throughWalls = true;
    private double barScale = 2.0;
    private boolean showInvisible = true;

    private boolean targetPlayers = true;

    public Health() {
        super("Health", "Displays health bars beside entities.\nDetectability: Safe",
                ModuleCategory.PLAYER, -1);
    }

    @Override
    public void onRender(WorldRenderContext context) {
        if (mc.player == null || mc.level == null) return;

        MultiBufferSource consumers = context.consumers();
        if (consumers == null) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.position();

        if (throughWalls) {
            org.lwjgl.opengl.GL11.glDepthFunc(org.lwjgl.opengl.GL11.GL_ALWAYS);
        }

        try {
            VertexConsumer buffer = consumers.getBuffer(RenderTypes.lines()); 

            for (Player player : mc.level.players()) {
                if (player == mc.player) continue;
                if (!player.isAlive()) continue;
                if (!showInvisible && player.isInvisible()) continue;
                if (AntiBot.isBot(player)) continue;
                if (!targetPlayers) continue;

                renderBillboardedHealthBar(context, buffer, camera, cameraPos, player);
            }
            
            if (consumers instanceof MultiBufferSource.BufferSource bs) {
                bs.endBatch(RenderTypes.lines());
            }
        } finally {
            if (throughWalls) {
                org.lwjgl.opengl.GL11.glDepthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
            }
        }
    }

    private void renderBillboardedHealthBar(WorldRenderContext context, VertexConsumer buffer,
                                            Camera camera, Vec3 cameraPos, LivingEntity entity) {
        double dx = entity.getX() - cameraPos.x;
        double dy = entity.getY() - cameraPos.y;
        double dz = entity.getZ() - cameraPos.z;

        float healthRatio = Math.max(0, Math.min(1, entity.getHealth() / entity.getMaxHealth()));
        float height = entity.getBbHeight();
        
        // Final line width used for the bar
        float activeWidth = (float) barScale;
        float bgWidth = activeWidth + 1.0f;
        
        PoseStack poseStack = context.matrices();
        poseStack.pushPose();
        
        // Translate to the middle of the entity
        poseStack.translate(dx, dy + (height / 2.0), dz);
        
        // Face the camera
        poseStack.mulPose(camera.rotation());
        
        // Offset it slightly to the left of the hitbox
        float xOffset = (entity.getBbWidth() / 2.0f) + 0.15f;
        poseStack.translate(-xOffset, -height / 2.0, 0);

        Matrix4f matrix = poseStack.last().pose();

        int bgColor = 0xFF000000;
        int healthColor = getHealthTintedColor(entity, 0xFF00FF00);

        // Background (Black track)
        drawVerticalLine(matrix, buffer, 0, height, bgColor, bgWidth);
        
        // Health (Colored portion)
        drawVerticalLine(matrix, buffer, 0, height * healthRatio, healthColor, activeWidth);

        poseStack.popPose();
    }

    private void drawVerticalLine(Matrix4f matrix, VertexConsumer buffer, float yStart, float yEnd, int color, float width) {
        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        // Using setNormal and setLineWidth to satisfy the RenderTypes.lines() format
        buffer.addVertex(matrix, 0, yStart, 0).setColor(r, g, b, a).setNormal(0, 1, 0).setLineWidth(width);
        buffer.addVertex(matrix, 0, yEnd, 0).setColor(r, g, b, a).setNormal(0, 1, 0).setLineWidth(width);
    }

    private int getHealthTintedColor(LivingEntity entity, int fallback) {
        float maxHealth = entity.getMaxHealth();
        if (maxHealth <= 0) return fallback;
        float ratio = Math.max(0, Math.min(1, entity.getHealth() / maxHealth));
        int r, g;
        if (ratio > 0.5f) {
            float t = (ratio - 0.5f) * 2;
            r = (int) (255 * (1 - t));
            g = 255;
        } else {
            float t = ratio * 2;
            r = 255;
            g = (int) (255 * t);
        }
        return 0xFF000000 | (r << 16) | (g << 8);
    }

    // --- Settings ---

    @Override public boolean hasConfigurableSettings() { return true; }
    @Override public Screen createSettingsScreen(Screen parent) { return new ModuleSettingsScreen(parent, this); }

    public boolean isThroughWalls()              { return throughWalls; }
    public void setThroughWalls(boolean v)       { throughWalls = v; saveConfig(); }
    public double getBarScale()                  { return barScale; }
    public void setBarScale(double v)            { barScale = Math.max(0.5, Math.min(10.0, v)); saveConfig(); }
    public boolean isShowInvisible()             { return showInvisible; }
    public void setShowInvisible(boolean v)      { showInvisible = v; saveConfig(); }
    public boolean isTargetPlayers()             { return targetPlayers; }
    public void setTargetPlayers(boolean v)      { targetPlayers = v; saveConfig(); }

    @Override
    public void loadConfig(Properties properties) {
        super.loadConfig(properties);
        throughWalls  = Boolean.parseBoolean(properties.getProperty("health.through_walls",   Boolean.toString(throughWalls)));
        showInvisible = Boolean.parseBoolean(properties.getProperty("health.show_invisible",  Boolean.toString(showInvisible)));
        targetPlayers = Boolean.parseBoolean(properties.getProperty("health.target_players",  Boolean.toString(targetPlayers)));
        
        String s = properties.getProperty("health.bar_scale");
        if (s != null) {
            try { barScale = Math.max(0.5, Math.min(10.0, Double.parseDouble(s.trim()))); }
            catch (NumberFormatException ignored) {}
        }
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("health.through_walls",    Boolean.toString(throughWalls));
        properties.setProperty("health.show_invisible",   Boolean.toString(showInvisible));
        properties.setProperty("health.target_players",   Boolean.toString(targetPlayers));
        properties.setProperty("health.bar_scale",        Double.toString(barScale));
    }
}
