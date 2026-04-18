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

    public Nametags() {
        super("Nametags",
                "Renders larger custom name tags with optional health and distance info.\nDetectability: Safe",
                ModuleCategory.RENDER,
                -1);
    }

    @Override
    public void onRender(WorldRenderContext context) {
        if (mc.player == null || mc.level == null) {
            return;
        }

        PoseStack matrices = context.matrices();
        if (matrices == null) {
            return;
        }

        // Use Minecraft's own buffer source for text rendering — the context's
        // consumers() may not track/flush font render types correctly, which
        // causes drawInBatch text to silently vanish.
        MultiBufferSource.BufferSource bufferSource =
                mc.renderBuffers().bufferSource();

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.position();
        double rangeSq = range * range;
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

        java.util.List<Player> targetPlayers = new java.util.ArrayList<>(mc.level.players());
        if (mc.player != null && !targetPlayers.contains(mc.player)) {
            targetPlayers.add(mc.player);
        }

        for (Player player : targetPlayers) {
            if (!shouldRender(player)) {
                continue;
            }

            // Use interpolated positions so the nametag tracks the entity smoothly
            double ix = Mth.lerp(partialTick, player.xOld, player.getX());
            double iy = Mth.lerp(partialTick, player.yOld, player.getY());
            double iz = Mth.lerp(partialTick, player.zOld, player.getZ());

            double dx = ix - cameraPos.x;
            double dy = iy + player.getBbHeight() + 0.55 - cameraPos.y;
            double dz = iz - cameraPos.z;
            double distanceSq = dx * dx + dy * dy + dz * dz;
            if (distanceSq > rangeSq) {
                continue;
            }

            MutableComponent text = player.getDisplayName().copy();
            if (showHealth) {
                text.append(buildHealthComponent(player));
            }
            if (showDistance) {
                text.append(Component.literal(" [" + Mth.floor(Math.sqrt(distanceSq)) + "m]").withStyle(ChatFormatting.GRAY));
            }

            renderTag(matrices, bufferSource, camera, text, dx, dy, dz, Math.sqrt(distanceSq));
        }

        // Flush our buffer source — safe because we created it ourselves,
        // not using the shared context consumers.
        bufferSource.endBatch();
    }

    private boolean shouldRender(Player player) {
        if (player == null || !player.isAlive()) {
            return false;
        }
        if (player == mc.player) {
            if (!showOwnNameTag || mc.options.getCameraType().isFirstPerson()) {
                return false;
            }
        }
        if (!showInvisible && player.isInvisible()) {
            return false;
        }
        return !AntiBot.isBot(player);
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
                line,
                textX,
                0.0f,
                0xFFFFFFFF,
                textShadow,
                matrix,
                consumers,
                Font.DisplayMode.SEE_THROUGH,
                backgroundColor,
                LightTexture.FULL_BRIGHT
        );

        matrices.popPose();
    }

    private Component buildHealthComponent(LivingEntity entity) {
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

        return Component.literal(" " + fastOneDecimal(health / 2.0f) + " \u2764").withStyle(color);
    }

    private String fastOneDecimal(float value) {
        int tenths = Math.round(value * 10f);
        int intPart = tenths / 10;
        int fracPart = Math.abs(tenths % 10);
        return fracPart == 0 ? String.valueOf(intPart) : intPart + "." + fracPart;
    }

    public static boolean shouldHideVanillaNametag(net.minecraft.world.entity.Entity entity) {
        if (!(entity instanceof Player)) {
            return false;
        }
        if (PhantomMod.getModuleManager() == null) {
            return false;
        }
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

    public double getRange() { return range; }
    public void setRange(double range) { this.range = Mth.clamp(range, 16.0, 160.0); saveConfig(); }
    public double getScale() { return scale; }
    public void setScale(double scale) { this.scale = Mth.clamp(scale, 0.5, 3.0); saveConfig(); }
    public boolean isShowOwnNameTag() { return showOwnNameTag; }
    public void setShowOwnNameTag(boolean showOwnNameTag) { this.showOwnNameTag = showOwnNameTag; saveConfig(); }
    public boolean isDistanceScaling() { return distanceScaling; }
    public void setDistanceScaling(boolean distanceScaling) { this.distanceScaling = distanceScaling; saveConfig(); }
    public boolean isBackground() { return background; }
    public void setBackground(boolean background) { this.background = background; saveConfig(); }
    public boolean isTextShadow() { return textShadow; }
    public void setTextShadow(boolean textShadow) { this.textShadow = textShadow; saveConfig(); }
    public boolean isShowHealth() { return showHealth; }
    public void setShowHealth(boolean showHealth) { this.showHealth = showHealth; saveConfig(); }
    public boolean isShowDistance() { return showDistance; }
    public void setShowDistance(boolean showDistance) { this.showDistance = showDistance; saveConfig(); }
    public boolean isShowInvisible() { return showInvisible; }
    public void setShowInvisible(boolean showInvisible) { this.showInvisible = showInvisible; saveConfig(); }

    @Override
    public void loadConfig(Properties properties) {
        super.loadConfig(properties);
        try {
            range = Mth.clamp(Double.parseDouble(properties.getProperty("nametags.range", "96.0")), 16.0, 160.0);
        } catch (NumberFormatException ignored) {
        }
        try {
            scale = Mth.clamp(Double.parseDouble(properties.getProperty("nametags.scale", "1.0")), 0.5, 3.0);
        } catch (NumberFormatException ignored) {
        }
        showOwnNameTag = Boolean.parseBoolean(properties.getProperty("nametags.show_own", Boolean.toString(showOwnNameTag)));
        distanceScaling = Boolean.parseBoolean(properties.getProperty("nametags.distance_scaling", Boolean.toString(distanceScaling)));
        background = Boolean.parseBoolean(properties.getProperty("nametags.background", Boolean.toString(background)));
        textShadow = Boolean.parseBoolean(properties.getProperty("nametags.text_shadow", Boolean.toString(textShadow)));
        showHealth = Boolean.parseBoolean(properties.getProperty("nametags.show_health", Boolean.toString(showHealth)));
        showDistance = Boolean.parseBoolean(properties.getProperty("nametags.show_distance", Boolean.toString(showDistance)));
        showInvisible = Boolean.parseBoolean(properties.getProperty("nametags.show_invisible", Boolean.toString(showInvisible)));
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("nametags.range", Double.toString(range));
        properties.setProperty("nametags.scale", Double.toString(scale));
        properties.setProperty("nametags.show_own", Boolean.toString(showOwnNameTag));
        properties.setProperty("nametags.distance_scaling", Boolean.toString(distanceScaling));
        properties.setProperty("nametags.background", Boolean.toString(background));
        properties.setProperty("nametags.text_shadow", Boolean.toString(textShadow));
        properties.setProperty("nametags.show_health", Boolean.toString(showHealth));
        properties.setProperty("nametags.show_distance", Boolean.toString(showDistance));
        properties.setProperty("nametags.show_invisible", Boolean.toString(showInvisible));
    }
}
