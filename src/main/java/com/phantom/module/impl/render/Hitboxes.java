/*
 * Draws wireframe boxes around players, mobs, and animals (client-side; works in multiplayer).
 */
package com.phantom.module.impl.render;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import com.phantom.render.EntityOutlineRender;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;

import java.util.List;
import java.util.Properties;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;

public class Hitboxes extends Module {
    private static final double RANGE = 192.0D;

    private boolean playersEnabled = true;
    private boolean mobsEnabled = true;
    private boolean animalsEnabled = true;

    public Hitboxes() {
        super("Hitboxes",
                "Wireframe boxes around players, mobs, and animals. Filters in settings.",
                ModuleCategory.GHOST,
                -1);
    }

    @Override
    public void onRender(WorldRenderContext context) {
        if (mc.level == null || mc.player == null) {
            return;
        }
        List<Entity> targets = mc.level.getEntitiesOfClass(
                Entity.class,
                mc.player.getBoundingBox().inflate(RANGE),
                this::shouldDraw
        );
        for (Entity entity : targets) {
            EntityOutlineRender.drawEntityBox(context, entity, 0.35f, 0.85f, 1f, 0.95f);
        }
    }

    @Override
    public boolean hasConfigurableSettings() {
        return true;
    }

    @Override
    public Screen createSettingsScreen(Screen parent) {
        return new ModuleSettingsScreen(parent, this);
    }

    public boolean isPlayersEnabled() {
        return playersEnabled;
    }

    public void setPlayersEnabled(boolean v) {
        this.playersEnabled = v;
        saveConfig();
    }

    public boolean isMobsEnabled() {
        return mobsEnabled;
    }

    public void setMobsEnabled(boolean v) {
        this.mobsEnabled = v;
        saveConfig();
    }

    public boolean isAnimalsEnabled() {
        return animalsEnabled;
    }

    public void setAnimalsEnabled(boolean v) {
        this.animalsEnabled = v;
        saveConfig();
    }

    @Override
    public void loadConfig(Properties properties) {
        super.loadConfig(properties);
        playersEnabled = Boolean.parseBoolean(properties.getProperty("hitboxes.players", "true"));
        mobsEnabled = Boolean.parseBoolean(properties.getProperty("hitboxes.mobs", "true"));
        animalsEnabled = Boolean.parseBoolean(properties.getProperty("hitboxes.animals", "true"));
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("hitboxes.players", Boolean.toString(playersEnabled));
        properties.setProperty("hitboxes.mobs", Boolean.toString(mobsEnabled));
        properties.setProperty("hitboxes.animals", Boolean.toString(animalsEnabled));
    }

    private boolean shouldDraw(Entity entity) {
        if (entity == mc.player) return false;
        if (entity instanceof LivingEntity living && !living.isAlive()) return false;
        if (playersEnabled && entity instanceof AbstractClientPlayer) return true;
        if (animalsEnabled && (entity instanceof Animal || entity instanceof AgeableMob)) return true;
        if (mobsEnabled && entity instanceof Mob) return true;
        return false;
    }
}
