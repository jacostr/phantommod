package com.phantom.module.impl.render;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

public class ESP extends Module {
    private static final double WALL_ESP_RANGE = 192.0D;

    // Track which entities we currently forced into the vanilla glowing state so we can
    // clean them up properly when settings change or the module turns off.
    private final Set<Integer> highlightedEntities = new HashSet<>();

    // Config toggles shown in the settings screen.
    private boolean playersEnabled = true;
    private boolean mobsEnabled = true;
    private boolean animalsEnabled = true;

    public ESP() {
        super("ESP", "Draws simple boxes around nearby entities.", ModuleCategory.RENDER, GLFW.GLFW_KEY_Y);
    }

    // Tick pass:
    // - decide which entities match the current filters
    // - enable vanilla glow on those entities so they remain visible through walls
    // - remove glow from anything that no longer matches
    @Override
    public void onTick() {
        if (mc.level == null || mc.player == null) {
            return;
        }

        Set<Integer> nextHighlighted = new HashSet<>();
        for (Entity entity : mc.level.getEntitiesOfClass(
                Entity.class,
                mc.player.getBoundingBox().inflate(WALL_ESP_RANGE),
                this::shouldHighlight
        )) {
            if (!shouldHighlight(entity)) {
                continue;
            }

            // Vanilla glowing keeps the target visible through walls while the world render pass
            // adds a local box, which gives the module a clearer ESP feel.
            entity.setGlowingTag(true);
            nextHighlighted.add(entity.getId());
        }

        for (Integer entityId : highlightedEntities) {
            if (nextHighlighted.contains(entityId)) {
                continue;
            }

            Entity entity = mc.level.getEntity(entityId);
            if (entity != null) {
                entity.setGlowingTag(false);
            }
        }

        highlightedEntities.clear();
        highlightedEntities.addAll(nextHighlighted);
    }

    // Render pass:
    // draw a simple line box for local visibility while the vanilla glow handles the
    // through-walls part of the effect.
    @Override
    public void onRender(WorldRenderContext context) {
        if (mc.level == null || mc.player == null) {
            return;
        }

        Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();
        var vertexConsumer = context.consumers().getBuffer(RenderTypes.lines());

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!shouldHighlight(entity)) {
                continue;
            }

            AABB box = entity.getBoundingBox().inflate(0.05D);

            // Render the entity box relative to the active camera so it stays stable in world space.
            ShapeRenderer.renderShape(
                    context.matrices(),
                    vertexConsumer,
                    Shapes.create(box),
                    -cameraPos.x,
                    -cameraPos.y,
                    -cameraPos.z,
                    getColor(entity),
                    1.0F
            );
        }
    }

    @Override
    public void onDisable() {
        clearHighlights();
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    public Screen createSettingsScreen(Screen parent) {
        return new ModuleSettingsScreen(parent, this);
    }

    public boolean isPlayersEnabled() {
        return playersEnabled;
    }

    public void setPlayersEnabled(boolean playersEnabled) {
        this.playersEnabled = playersEnabled;
    }

    public boolean isMobsEnabled() {
        return mobsEnabled;
    }

    public void setMobsEnabled(boolean mobsEnabled) {
        this.mobsEnabled = mobsEnabled;
    }

    public boolean isAnimalsEnabled() {
        return animalsEnabled;
    }

    public void setAnimalsEnabled(boolean animalsEnabled) {
        this.animalsEnabled = animalsEnabled;
    }

    // Central filter used by both the glow pass and the box render pass so the visual
    // behavior stays consistent across both parts of the ESP.
    private boolean shouldHighlight(Entity entity) {
        if (entity == mc.player) {
            return false;
        }

        if (entity instanceof LivingEntity livingEntity && !livingEntity.isAlive()) {
            return false;
        }

        if (playersEnabled && entity instanceof AbstractClientPlayer) {
            return true;
        }

        if (animalsEnabled && (entity instanceof Animal || entity instanceof AgeableMob)) {
            return true;
        }

        return mobsEnabled && entity instanceof Mob;
    }

    // Simple color palette so each target type reads differently at a glance.
    private int getColor(Entity entity) {
        if (entity instanceof AbstractClientPlayer) {
            return 0xFF55FFFF;
        }

        if (entity instanceof Animal || entity instanceof AgeableMob) {
            return 0xFF55FF55;
        }

        if (entity instanceof Mob) {
            return 0xFFFF5555;
        }

        return 0xFFFFFF55;
    }

    // Clear every glow tag this module applied. This matters because glowing is stored on
    // the entity itself, so leaving it behind would make the ESP look "stuck" after disable.
    private void clearHighlights() {
        if (mc.level == null) {
            highlightedEntities.clear();
            return;
        }

        for (Integer entityId : highlightedEntities) {
            Entity entity = mc.level.getEntity(entityId);
            if (entity != null) {
                entity.setGlowingTag(false);
            }
        }

        highlightedEntities.clear();
    }
}
