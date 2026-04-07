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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ESP extends Module {
    private static final double WALL_ESP_RANGE = 192.0D;

    private final Set<Integer> highlightedEntities = new HashSet<>();

    private boolean playersEnabled = true;
    private boolean mobsEnabled = true;
    private boolean animalsEnabled = true;

    public ESP() {
        super("ESP", "Highlights nearby players, mobs, and animals with boxes that stay visible through blocks.", ModuleCategory.RENDER, GLFW.GLFW_KEY_Y);
    }

    @Override
    public void onTick() {
        if (mc.level == null || mc.player == null) {
            clearHighlights();
            return;
        }

        // Collect matching entities once, reuse the list to avoid double-querying.
        List<Entity> targets = mc.level.getEntitiesOfClass(
                Entity.class,
                mc.player.getBoundingBox().inflate(WALL_ESP_RANGE),
                this::shouldHighlight
        );

        Set<Integer> nextHighlighted = new HashSet<>();
        for (Entity entity : targets) {
            entity.setGlowingTag(true);
            nextHighlighted.add(entity.getId());
        }

        // Strip glow from entities that no longer match the filter.
        for (Integer entityId : highlightedEntities) {
            if (!nextHighlighted.contains(entityId)) {
                Entity entity = mc.level.getEntity(entityId);
                if (entity != null) {
                    entity.setGlowingTag(false);
                }
            }
        }

        highlightedEntities.clear();
        highlightedEntities.addAll(nextHighlighted);
    }

    @Override
    public void onRender(WorldRenderContext context) {
        if (mc.level == null || mc.player == null) {
            return;
        }

        Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();

        // FIXED: use LINES render type — it's always drawn regardless of depth,
        // so boxes appear through walls without needing any depth-test tricks.
        var vertexConsumer = context.consumers().getBuffer(RenderTypes.lines());

        // FIXED: query by range instead of entitiesForRendering() so entities
        // behind walls (which vanilla culls) are still included.
        for (Entity entity : mc.level.getEntitiesOfClass(
                Entity.class,
                mc.player.getBoundingBox().inflate(WALL_ESP_RANGE),
                this::shouldHighlight
        )) {
            AABB box = entity.getBoundingBox().inflate(0.05D);

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
    public boolean hasConfigurableSettings() {
        return true;
    }

    @Override
    public Screen createSettingsScreen(Screen parent) {
        return new ModuleSettingsScreen(parent, this);
    }

    public boolean isPlayersEnabled() { return playersEnabled; }
    public void setPlayersEnabled(boolean v) { this.playersEnabled = v; }

    public boolean isMobsEnabled() { return mobsEnabled; }
    public void setMobsEnabled(boolean v) { this.mobsEnabled = v; }

    public boolean isAnimalsEnabled() { return animalsEnabled; }
    public void setAnimalsEnabled(boolean v) { this.animalsEnabled = v; }

    private boolean shouldHighlight(Entity entity) {
        if (entity == mc.player) return false;

        if (entity instanceof LivingEntity living && !living.isAlive()) return false;

        if (playersEnabled && entity instanceof AbstractClientPlayer) return true;
        if (animalsEnabled && (entity instanceof Animal || entity instanceof AgeableMob)) return true;
        if (mobsEnabled && entity instanceof Mob) return true;

        return false;
    }

    private int getColor(Entity entity) {
        if (entity instanceof AbstractClientPlayer) return 0xFF55FFFF; // cyan
        if (entity instanceof Animal || entity instanceof AgeableMob) return 0xFF55FF55; // green
        if (entity instanceof Mob) return 0xFFFF5555; // red
        return 0xFFFFFF55; // yellow fallback
    }

    private void clearHighlights() {
        if (mc.level != null) {
            for (Integer id : highlightedEntities) {
                Entity e = mc.level.getEntity(id);
                if (e != null) e.setGlowingTag(false);
            }
        }
        highlightedEntities.clear();
    }
}