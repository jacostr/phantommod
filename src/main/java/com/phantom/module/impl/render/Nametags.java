package com.phantom.module.impl.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.phantom.PhantomMod;
import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import com.phantom.module.impl.player.AntiBot;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Properties;

public class Nametags extends Module {
    private double scale = 1.0;
    private double maxDistance = 96.0;
    private boolean throughWalls = true;
    private boolean showDistance;
    private boolean showInvisible;
    private boolean showSelf;

    public Nametags() {
        super("Nametags",
                "Renders custom player nametags and can merge health into the name text like Hypixel.\nDetectability: Safe",
                ModuleCategory.PLAYER,
                -1);
    }

    @Override
    public void onRender(WorldRenderContext context) {
        if (mc.player == null || mc.level == null) {
            return;
        }

        PoseStack matrices = context.matrices();
        MultiBufferSource consumers = context.consumers();
        if (matrices == null || consumers == null) {
            return;
        }

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.position();
        double rangeSq = maxDistance * maxDistance;

        for (Player player : mc.level.players()) {
            if (!shouldRender(player)) {
                continue;
            }

            double dx = player.getX() - cameraPos.x;
            double dy = player.getY() + player.getBbHeight() + 0.55 - cameraPos.y;
            double dz = player.getZ() - cameraPos.z;
            double distanceSq = dx * dx + dy * dy + dz * dz;
            if (distanceSq > rangeSq) {
                continue;
            }

            MutableComponent displayName = player.getDisplayName().copy();
            HealthBar healthBar = getHealthBarModule();
            if (healthBar != null && healthBar.isEnabled()) {
                Component suffix = healthBar.buildNametagHealthComponent(player);
                if (suffix != null) {
                    displayName.append(suffix);
                }
            }

            if (showDistance) {
                int distance = Mth.floor(Math.sqrt(distanceSq));
                displayName.append(Component.literal(" [" + distance + "m]"));
            }

            renderTag(matrices, consumers, camera, displayName, dx, dy, dz);
        }
    }

    private boolean shouldRender(Player player) {
        if (player == null || !player.isAlive()) {
            return false;
        }
        if (player == mc.player && !showSelf) {
            return false;
        }
        if (!showInvisible && player.isInvisible()) {
            return false;
        }
        return !AntiBot.isBot(player);
    }

    private void renderTag(PoseStack matrices, MultiBufferSource consumers, Camera camera,
                           Component text, double dx, double dy, double dz) {
        matrices.pushPose();
        matrices.translate(dx, dy, dz);
        matrices.mulPose(camera.rotation());

        float scaledSize = (float) (0.025f * scale);
        matrices.scale(-scaledSize, -scaledSize, scaledSize);

        FormattedCharSequence line = text.getVisualOrderText();
        int width = mc.font.width(line);
        float textX = -width / 2.0f;
        int background = (int) (mc.options.getBackgroundOpacity(0.25f) * 255.0f) << 24;
        Matrix4f matrix = matrices.last().pose();

        mc.font.drawInBatch(
                line,
                textX,
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

    private HealthBar getHealthBarModule() {
        return PhantomMod.getModuleManager() == null
                ? null
                : PhantomMod.getModuleManager().getModuleByClass(HealthBar.class);
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = Mth.clamp(scale, 0.5, 3.0);
        saveConfig();
    }

    public double getMaxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(double maxDistance) {
        this.maxDistance = Mth.clamp(maxDistance, 16.0, 160.0);
        saveConfig();
    }

    public boolean isThroughWalls() {
        return throughWalls;
    }

    public void setThroughWalls(boolean throughWalls) {
        this.throughWalls = throughWalls;
        saveConfig();
    }

    public boolean isShowDistance() {
        return showDistance;
    }

    public void setShowDistance(boolean showDistance) {
        this.showDistance = showDistance;
        saveConfig();
    }

    public boolean isShowInvisible() {
        return showInvisible;
    }

    public void setShowInvisible(boolean showInvisible) {
        this.showInvisible = showInvisible;
        saveConfig();
    }

    public boolean isShowSelf() {
        return showSelf;
    }

    public void setShowSelf(boolean showSelf) {
        this.showSelf = showSelf;
        saveConfig();
    }

    @Override
    public boolean hasConfigurableSettings() {
        return true;
    }

    @Override
    public Screen createSettingsScreen(Screen parent) {
        return new ModuleSettingsScreen(parent, this);
    }

    @Override
    public void loadConfig(Properties properties) {
        super.loadConfig(properties);
        try {
            scale = Mth.clamp(Double.parseDouble(properties.getProperty("nametags.scale", "1.0")), 0.5, 3.0);
        } catch (NumberFormatException ignored) {
        }
        try {
            maxDistance = Mth.clamp(Double.parseDouble(properties.getProperty("nametags.max_distance", "96.0")), 16.0, 160.0);
        } catch (NumberFormatException ignored) {
        }
        throughWalls = Boolean.parseBoolean(properties.getProperty("nametags.through_walls", Boolean.toString(throughWalls)));
        showDistance = Boolean.parseBoolean(properties.getProperty("nametags.show_distance", Boolean.toString(showDistance)));
        showInvisible = Boolean.parseBoolean(properties.getProperty("nametags.show_invisible", Boolean.toString(showInvisible)));
        showSelf = Boolean.parseBoolean(properties.getProperty("nametags.show_self", Boolean.toString(showSelf)));
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("nametags.scale", Double.toString(scale));
        properties.setProperty("nametags.max_distance", Double.toString(maxDistance));
        properties.setProperty("nametags.through_walls", Boolean.toString(throughWalls));
        properties.setProperty("nametags.show_distance", Boolean.toString(showDistance));
        properties.setProperty("nametags.show_invisible", Boolean.toString(showInvisible));
        properties.setProperty("nametags.show_self", Boolean.toString(showSelf));
    }
}
