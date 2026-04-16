/*
 * HealthBar.java — Renders a health indicator near the crosshair and above entities.
 */
package com.phantom.module.impl.render;

import com.phantom.PhantomMod;
import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;

import java.util.Locale;
import java.util.Properties;

public class HealthBar extends Module {
    public enum ColorMode {
        RED("Red"),
        GRADIENT("Gradient");

        private final String label;
        ColorMode(String label) { this.label = label; }
        public String getLabel() { return label; }
        public ColorMode next() { return this == RED ? GRADIENT : RED; }
        public static ColorMode fromString(String v) {
            if (v == null) return GRADIENT;
            try { return ColorMode.valueOf(v.trim().toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException e) { return GRADIENT; }
        }
    }

    private int offsetX = 0;
    private int offsetY = 12;
    private float tagScale = 1.0f;   // multiplier on the world-space tag size (0.5 – 4.0)
    private boolean showAbsorption = true;
    private boolean opponentHealthTags = false;
    private boolean nametagHealth = false;  // appends HP to player nametags like Hypixel
    private boolean showSelf = true;
    private ColorMode colorMode = ColorMode.GRADIENT;

    public HealthBar() {
        super("HealthBar",
                "Displays your health near the crosshair and opponents' health above their heads.\nDetectability: Safe",
                ModuleCategory.PLAYER,
                -1);
    }

    // ── HUD: self health near crosshair ───────────────────────────────────────

    @Override
    public void onHudRender(GuiGraphics graphics) {
        if (!showSelf || mc.player == null || mc.options.hideGui) return;

        float health = mc.player.getHealth();
        float maxHealth = mc.player.getMaxHealth();
        float absorption = mc.player.getAbsorptionAmount();

        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2;

        String healthText = String.format("%.1f", health / 2.0f) + " \u2764";
        if (showAbsorption && absorption > 0) {
            healthText += " +" + String.format("%.1f", absorption / 2.0f);
        }

        int color = getHealthColor(health, maxHealth);
        int textWidth = mc.font.width(healthText);
        graphics.drawString(mc.font, Component.literal(healthText),
                centerX + offsetX - textWidth / 2,
                centerY + offsetY,
                color, true);
    }

    // ── World: opponent health tags above their heads ─────────────────────────

    @Override
    public void onRender(WorldRenderContext context) {
        if (!opponentHealthTags || mc.level == null || mc.player == null) return;
        if (nametagHealth && isCustomNametagModuleActive()) return;

        // FIX: null-guard matrices and consumers before doing any work
        PoseStack matrices = context.matrices();
        MultiBufferSource consumers = context.consumers();
        if (matrices == null || consumers == null) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.position();
        double rangeSq = 64.0 * 64.0; // FIX: use double to avoid int overflow

        for (LivingEntity entity : mc.level.getEntitiesOfClass(
                LivingEntity.class,
                mc.player.getBoundingBox().inflate(64.0))) {

            if (entity == mc.player || !entity.isAlive() || entity.isInvisible()) continue;

            double dx = entity.getX() - cameraPos.x;
            // FIX: tag floats above the entity's head — use getBbHeight() + 0.25 for tight placement
            double dy = entity.getY() + entity.getBbHeight() + 0.25 - cameraPos.y;
            double dz = entity.getZ() - cameraPos.z;

            // FIX: correct squared-distance comparison using double
            if (dx * dx + dy * dy + dz * dz > rangeSq) continue;

            float health = entity.getHealth();
            float maxHealth = entity.getMaxHealth();
            float absorption = entity.getAbsorptionAmount();

            String text = String.format("%.1f", health / 2.0f) + " \u2764";
            if (showAbsorption && absorption > 0) {
                text += " +" + String.format("%.1f", absorption / 2.0f);
            }

            int color = getHealthColor(health, maxHealth);
            renderTag(matrices, consumers, camera, text, color, dx, dy, dz, tagScale);
        }
    }

    /**
     * Returns a formatted health string for a living entity, styled exactly like
     * Hypixel's nametag health — colour-coded value, heart symbol, and golden
     * absorption suffix. Returns null if nametagHealth is disabled.
     *
     * Call this from your Nametags renderer to append health to the display name.
     */
    public String buildNametagHealthSuffix(LivingEntity entity) {
        Component component = buildNametagHealthComponent(entity);
        return component == null ? null : component.getString();
    }

    public Component buildNametagHealthComponent(LivingEntity entity) {
        if (!nametagHealth) return null;

        float health    = Math.max(0f, entity.getHealth());
        float maxHealth = Math.max(1f, entity.getMaxHealth());
        float ratio     = health / maxHealth;

        // Colour ladder matching Hypixel: green → yellow → orange → red
        ChatFormatting color;
        if      (ratio > 0.75f) color = ChatFormatting.GREEN;
        else if (ratio > 0.50f) color = ChatFormatting.YELLOW;
        else if (ratio > 0.25f) color = ChatFormatting.GOLD;
        else                    color = ChatFormatting.RED;

        String valueStr = fastOneDecimal(health / 2.0f);
        MutableComponent text = Component.literal(" ")
                .append(Component.literal(valueStr).withStyle(color))
                .append(Component.literal(" \u2764").withStyle(color));

        if (showAbsorption) {
            float abs = entity.getAbsorptionAmount();
            if (abs > 0f) {
                text.append(Component.literal(" "))
                        .append(Component.literal("+" + fastOneDecimal(abs / 2.0f)).withStyle(ChatFormatting.GOLD))
                        .append(Component.literal(" \u2764").withStyle(ChatFormatting.GOLD));
            }
        }

        return text;
    }

    private String fastOneDecimal(float value) {
        int tenths   = Math.round(value * 10f);
        int intPart  = tenths / 10;
        int fracPart = Math.abs(tenths % 10);
        return fracPart == 0 ? String.valueOf(intPart) : intPart + "." + fracPart;
    }

    /**
     * Renders a floating text tag at the given world-space offset from the camera.
     * Uses FULL_BRIGHT lighting so the tag is always readable regardless of shadow.
     */
    private void renderTag(PoseStack matrices, MultiBufferSource consumers,
                           Camera camera, String text, int color,
                           double dx, double dy, double dz, float scale) {
        matrices.pushPose();
        matrices.translate(dx, dy, dz);

        // FIX: billboard the tag toward the camera
        matrices.mulPose(camera.rotation());

        // Scale: negative Y flips text right-side up in world space.
        // tagScale lets the user grow the tag beyond the default 0.025 base size.
        float s = 0.025f * scale;
        matrices.scale(-s, -s, s);

        Matrix4f matrix = matrices.last().pose();
        float bgOpacity = mc.options.getBackgroundOpacity(0.25f);
        int bgColor = (int)(bgOpacity * 255.0f) << 24;
        float textX = (float)(-mc.font.width(text) / 2);

        // FIX: use LightTexture.FULL_BRIGHT so tag is visible in dark areas
        mc.font.drawInBatch(
                text, textX, 0f, color,
                false, matrix, consumers,
                net.minecraft.client.gui.Font.DisplayMode.SEE_THROUGH, // renders through blocks
                bgColor,
                LightTexture.FULL_BRIGHT
        );

        matrices.popPose();
    }

    private int getHealthColor(float health, float maxHealth) {
        if (colorMode == ColorMode.RED) return 0xFFFF5555;

        float ratio = Mth.clamp(health / Math.max(1.0f, maxHealth), 0f, 1f);
        int r, g;
        if (ratio > 0.5f) {
            float t = (ratio - 0.5f) * 2.0f;
            r = (int)(255 * (1.0f - t));
            g = 255;
        } else {
            float t = ratio * 2.0f;
            r = 255;
            g = (int)(255 * t);
        }
        return 0xFF000000 | (r << 16) | (g << 8);
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public int getOffsetX() { return offsetX; }
    public void setOffsetX(int v) { offsetX = Mth.clamp(v, -200, 200); saveConfig(); }
    public int getOffsetY() { return offsetY; }
    public void setOffsetY(int v) { offsetY = Mth.clamp(v, -200, 200); saveConfig(); }
    public float getTagScale() { return tagScale; }
    public void setTagScale(float v) { tagScale = Mth.clamp(v, 0.5f, 4.0f); saveConfig(); }
    public boolean isShowAbsorption() { return showAbsorption; }
    public void setShowAbsorption(boolean v) { showAbsorption = v; saveConfig(); }
    public boolean isOpponentHealthTags() { return opponentHealthTags; }
    public void setOpponentHealthTags(boolean v) { opponentHealthTags = v; saveConfig(); }
    public boolean isNametagHealth() { return nametagHealth; }
    public void setNametagHealth(boolean v) { nametagHealth = v; saveConfig(); }
    public boolean isShowSelf() { return showSelf; }
    public void setShowSelf(boolean v) { showSelf = v; saveConfig(); }
    public ColorMode getColorMode() { return colorMode; }
    public void cycleColorMode() { colorMode = colorMode.next(); saveConfig(); }

    private boolean isCustomNametagModuleActive() {
        if (PhantomMod.getModuleManager() == null) {
            return false;
        }
        Nametags nametags = PhantomMod.getModuleManager().getModuleByClass(Nametags.class);
        return nametags != null && nametags.isEnabled();
    }

    @Override public boolean hasConfigurableSettings() { return true; }
    @Override public Screen createSettingsScreen(Screen parent) { return new ModuleSettingsScreen(parent, this); }

    // ── Config ────────────────────────────────────────────────────────────────

    @Override
    public void loadConfig(Properties p) {
        super.loadConfig(p);
        try { offsetX   = Integer.parseInt(p.getProperty("healthbar.offset_x", "0")); }  catch (Exception ignored) {}
        try { offsetY   = Integer.parseInt(p.getProperty("healthbar.offset_y", "12")); } catch (Exception ignored) {}
        try { tagScale  = Mth.clamp(Float.parseFloat(p.getProperty("healthbar.tag_scale", "1.0")), 0.5f, 4.0f); } catch (Exception ignored) {}
        showAbsorption     = Boolean.parseBoolean(p.getProperty("healthbar.absorption",       Boolean.toString(showAbsorption)));
        opponentHealthTags = Boolean.parseBoolean(p.getProperty("healthbar.opponent_health",  Boolean.toString(opponentHealthTags)));
        nametagHealth      = Boolean.parseBoolean(p.getProperty("healthbar.nametag_health",   Boolean.toString(nametagHealth)));
        showSelf           = Boolean.parseBoolean(p.getProperty("healthbar.show_self",        "true"));
        colorMode          = ColorMode.fromString(p.getProperty("healthbar.color_mode"));
    }

    @Override
    public void saveConfig(Properties p) {
        super.saveConfig(p);
        p.setProperty("healthbar.offset_x",       Integer.toString(offsetX));
        p.setProperty("healthbar.offset_y",        Integer.toString(offsetY));
        p.setProperty("healthbar.tag_scale",       Float.toString(tagScale));
        p.setProperty("healthbar.absorption",      Boolean.toString(showAbsorption));
        p.setProperty("healthbar.opponent_health", Boolean.toString(opponentHealthTags));
        p.setProperty("healthbar.nametag_health",  Boolean.toString(nametagHealth));
        p.setProperty("healthbar.show_self",       Boolean.toString(showSelf));
        p.setProperty("healthbar.color_mode",      colorMode.name());
    }
}
