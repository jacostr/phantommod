/*
 * Health.java — Custom health rendering beside entities and in player nametags.
 */
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
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

import java.util.Properties;

public class Health extends Module {
    private boolean throughWalls = true;
    private double barScale = 2.0;
    private float tagScale = 1.0f;
    private boolean showInvisible = true;
    private boolean targetPlayers = true;
    private boolean showBar = true;
    private boolean showAboveHeads;
    private boolean nametagHealth;
    private boolean showAbsorption = true;
    private double maxDistance = 64.0;

    public Health() {
        super("Health",
                "Displays opponent health as a side bar, above-head value, or inside the nametag.\nDetectability: Safe",
                ModuleCategory.PLAYER, -1);
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

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.position();
        double rangeSq = maxDistance * maxDistance;

        if (throughWalls) {
            org.lwjgl.opengl.GL11.glDepthFunc(org.lwjgl.opengl.GL11.GL_ALWAYS);
        }

        try {
            VertexConsumer lineBuffer = showBar ? consumers.getBuffer(RenderTypes.lines()) : null;

            for (Player player : mc.level.players()) {
                if (!shouldRender(player, cameraPos, rangeSq)) {
                    continue;
                }

                if (showBar && lineBuffer != null) {
                    renderBillboardedHealthBar(matrices, lineBuffer, camera, cameraPos, player);
                }

                if (showAboveHeads && !nametagHealth) {
                    renderFloatingText(matrices, consumers, camera, cameraPos, player,
                            buildHealthValueComponent(player), 0.25);
                }

                if (nametagHealth) {
                    MutableComponent name = player.getDisplayName().copy();
                    Component suffix = buildNametagHealthComponent(player);
                    if (suffix != null) {
                        name.append(suffix);
                    }
                    renderFloatingText(matrices, consumers, camera, cameraPos, player, name, 0.55);
                }
            }

            if (showBar && consumers instanceof MultiBufferSource.BufferSource bufferSource) {
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

    private void renderFloatingText(PoseStack matrices, MultiBufferSource consumers, Camera camera,
                                    Vec3 cameraPos, LivingEntity entity, Component text, double yOffset) {
        double dx = entity.getX() - cameraPos.x;
        double dy = entity.getY() + entity.getBbHeight() + yOffset - cameraPos.y;
        double dz = entity.getZ() - cameraPos.z;

        matrices.pushPose();
        matrices.translate(dx, dy, dz);
        matrices.mulPose(camera.rotation());

        float scale = 0.025f * tagScale;
        matrices.scale(-scale, -scale, scale);

        FormattedCharSequence line = text.getVisualOrderText();
        float x = -mc.font.width(line) / 2.0f;
        int background = (int) (mc.options.getBackgroundOpacity(0.25f) * 255.0f) << 24;
        Matrix4f matrix = matrices.last().pose();

        mc.font.drawInBatch(
                line,
                x,
                0.0f,
                0xFFFFFFFF,
                false,
                matrix,
                consumers,
                throughWalls ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL,
                background,
                LightTexture.FULL_BRIGHT
        );

        matrices.popPose();
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

    public Component buildNametagHealthComponent(LivingEntity entity) {
        if (!nametagHealth) {
            return null;
        }

        float health = Math.max(0f, entity.getHealth());
        float maxHealth = Math.max(1f, entity.getMaxHealth());
        float ratio = health / maxHealth;

        ChatFormatting color;
        if (ratio > 0.75f) {
            color = ChatFormatting.GREEN;
        } else if (ratio > 0.50f) {
            color = ChatFormatting.YELLOW;
        } else if (ratio > 0.25f) {
            color = ChatFormatting.GOLD;
        } else {
            color = ChatFormatting.RED;
        }

        String valueStr = fastOneDecimal(health / 2.0f);
        MutableComponent text = Component.literal(" ")
                .append(Component.literal(valueStr).withStyle(color))
                .append(Component.literal(" \u2764").withStyle(color));

        if (showAbsorption) {
            float absorption = entity.getAbsorptionAmount();
            if (absorption > 0f) {
                text.append(Component.literal(" "))
                        .append(Component.literal("+" + fastOneDecimal(absorption / 2.0f)).withStyle(ChatFormatting.GOLD))
                        .append(Component.literal(" \u2764").withStyle(ChatFormatting.GOLD));
            }
        }

        return text;
    }

    private Component buildHealthValueComponent(LivingEntity entity) {
        MutableComponent text = Component.literal(fastOneDecimal(entity.getHealth() / 2.0f) + " \u2764")
                .withStyle(getChatColorForHealth(entity));
        if (showAbsorption && entity.getAbsorptionAmount() > 0f) {
            text.append(Component.literal(" +" + fastOneDecimal(entity.getAbsorptionAmount() / 2.0f))
                    .withStyle(ChatFormatting.GOLD));
        }
        return text;
    }

    private ChatFormatting getChatColorForHealth(LivingEntity entity) {
        float ratio = Mth.clamp(entity.getHealth() / Math.max(1.0f, entity.getMaxHealth()), 0.0f, 1.0f);
        if (ratio > 0.75f) {
            return ChatFormatting.GREEN;
        }
        if (ratio > 0.50f) {
            return ChatFormatting.YELLOW;
        }
        if (ratio > 0.25f) {
            return ChatFormatting.GOLD;
        }
        return ChatFormatting.RED;
    }

    private String fastOneDecimal(float value) {
        int tenths = Math.round(value * 10f);
        int intPart = tenths / 10;
        int fracPart = Math.abs(tenths % 10);
        return fracPart == 0 ? String.valueOf(intPart) : intPart + "." + fracPart;
    }

    public static boolean shouldHideVanillaNametag(Entity entity) {
        if (!(entity instanceof Player)) {
            return false;
        }
        if (PhantomMod.getModuleManager() == null) {
            return false;
        }
        Health module = PhantomMod.getModuleManager().getModuleByClass(Health.class);
        return module != null && module.isEnabled() && module.isNametagHealth();
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
    public float getTagScale() { return tagScale; }
    public void setTagScale(float v) { tagScale = Mth.clamp(v, 0.5f, 4.0f); saveConfig(); }
    public boolean isShowInvisible() { return showInvisible; }
    public void setShowInvisible(boolean v) { showInvisible = v; saveConfig(); }
    public boolean isTargetPlayers() { return targetPlayers; }
    public void setTargetPlayers(boolean v) { targetPlayers = v; saveConfig(); }
    public boolean isShowBar() { return showBar; }
    public void setShowBar(boolean v) { showBar = v; saveConfig(); }
    public boolean isShowAboveHeads() { return showAboveHeads; }
    public void setShowAboveHeads(boolean v) { showAboveHeads = v; saveConfig(); }
    public boolean isNametagHealth() { return nametagHealth; }
    public void setNametagHealth(boolean v) { nametagHealth = v; saveConfig(); }
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
        showAboveHeads = Boolean.parseBoolean(properties.getProperty("health.show_above_heads", Boolean.toString(showAboveHeads)));
        nametagHealth = Boolean.parseBoolean(properties.getProperty("health.nametag_health", Boolean.toString(nametagHealth)));
        showAbsorption = Boolean.parseBoolean(properties.getProperty("health.show_absorption", Boolean.toString(showAbsorption)));

        try {
            barScale = Mth.clamp(Double.parseDouble(properties.getProperty("health.bar_scale", Double.toString(barScale))), 0.5, 10.0);
        } catch (NumberFormatException ignored) {
        }
        try {
            tagScale = Mth.clamp(Float.parseFloat(properties.getProperty("health.tag_scale", Float.toString(tagScale))), 0.5f, 4.0f);
        } catch (NumberFormatException ignored) {
        }
        try {
            maxDistance = Mth.clamp(Double.parseDouble(properties.getProperty("health.max_distance", Double.toString(maxDistance))), 16.0, 160.0);
        } catch (NumberFormatException ignored) {
        }
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("health.through_walls", Boolean.toString(throughWalls));
        properties.setProperty("health.show_invisible", Boolean.toString(showInvisible));
        properties.setProperty("health.target_players", Boolean.toString(targetPlayers));
        properties.setProperty("health.show_bar", Boolean.toString(showBar));
        properties.setProperty("health.show_above_heads", Boolean.toString(showAboveHeads));
        properties.setProperty("health.nametag_health", Boolean.toString(nametagHealth));
        properties.setProperty("health.show_absorption", Boolean.toString(showAbsorption));
        properties.setProperty("health.bar_scale", Double.toString(barScale));
        properties.setProperty("health.tag_scale", Float.toString(tagScale));
        properties.setProperty("health.max_distance", Double.toString(maxDistance));
    }
}
