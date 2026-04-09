package com.phantom.module.impl.render;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.AbstractClientPlayer;
import com.phantom.module.impl.player.AntiBot;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.lwjgl.glfw.GLFW;

import java.util.Properties;

public class ESP extends Module {
    private static final double WALL_ESP_RANGE = 192.0D;

    private boolean playersEnabled = true;
    private boolean mobsEnabled = false;
    private boolean animalsEnabled = false;
    private boolean throughWalls = true;

    public ESP() {
        super("ESP", "Vape Style: White hitboxes when visible, BedWars team colors when behind walls.\nDetectability: Safe",
                ModuleCategory.PLAYER, GLFW.GLFW_KEY_Y);
    }

    @Override
    public void onTick() {
        // We don't need onTick for glowing effect anymore
    }

    @Override
    public void onRender(WorldRenderContext context) {
        if (mc.level == null || mc.player == null) {
            return;
        }

        Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();
        var vertexConsumer = context.consumers().getBuffer(RenderTypes.lines());

        for (Entity entity : mc.level.getEntitiesOfClass(
                Entity.class,
                mc.player.getBoundingBox().inflate(WALL_ESP_RANGE),
                this::shouldHighlight)) {
            
            AABB box = entity.getBoundingBox().inflate(0.05D);
            boolean visible = mc.player.hasLineOfSight(entity);
            if (!visible && !throughWalls) continue;

            int color = visible ? 0xFFFFFFFF : getBedWarsColor(entity);

            ShapeRenderer.renderShape(
                    context.matrices(),
                    vertexConsumer,
                    Shapes.create(box),
                    -cameraPos.x,
                    -cameraPos.y,
                    -cameraPos.z,
                    color,
                    1.0F);
        }
    }

    private int getBedWarsColor(Entity entity) {
        if (!(entity instanceof Player player)) {
            return getColor(entity);
        }

        Integer armorColor = getArmorColor(player);
        if (armorColor != null) {
            return 0xFF000000 | armorColor; // Ensure opaque
        }

        return getColor(entity);
    }

    private Integer getArmorColor(Player player) {
        int[] armorSlots = {36, 37, 38, 39};
        for (int slot : armorSlots) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) continue;
            DyedItemColor dyed = stack.get(DataComponents.DYED_COLOR);
            if (dyed != null) return dyed.rgb();
        }
        return null;
    }

    @Override
    public void onDisable() {
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

    public boolean isThroughWalls() {
        return throughWalls;
    }

    public void setThroughWalls(boolean throughWalls) {
        this.throughWalls = throughWalls;
        saveConfig();
    }

    @Override
    public void loadConfig(Properties properties) {
        super.loadConfig(properties);
        playersEnabled = Boolean.parseBoolean(properties.getProperty("esp.players", Boolean.toString(playersEnabled)));
        mobsEnabled = Boolean.parseBoolean(properties.getProperty("esp.mobs", Boolean.toString(mobsEnabled)));
        animalsEnabled = Boolean.parseBoolean(properties.getProperty("esp.animals", Boolean.toString(animalsEnabled)));
        throughWalls = Boolean.parseBoolean(properties.getProperty("esp.through_walls", Boolean.toString(throughWalls)));
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("esp.players", Boolean.toString(playersEnabled));
        properties.setProperty("esp.mobs", Boolean.toString(mobsEnabled));
        properties.setProperty("esp.animals", Boolean.toString(animalsEnabled));
        properties.setProperty("esp.through_walls", Boolean.toString(throughWalls));
    }

    private boolean shouldHighlight(Entity entity) {
        if (entity == mc.player)
            return false;

        if (entity instanceof LivingEntity living && !living.isAlive())
            return false;

        if (AntiBot.isBot(entity))
            return false;

        if (playersEnabled && entity instanceof AbstractClientPlayer)
            return true;
        if (animalsEnabled && (entity instanceof Animal || entity instanceof AgeableMob))
            return true;
        if (mobsEnabled && entity instanceof Mob)
            return true;

        return false;
    }

    private int getColor(Entity entity) {
        if (entity instanceof AbstractClientPlayer)
            return 0xFF55FFFF; // cyan
        if (entity instanceof Animal || entity instanceof AgeableMob)
            return 0xFF55FF55; // green
        if (entity instanceof Mob)
            return 0xFFFF5555; // red
        return 0xFFFFFF55; // yellow fallback
    }
}
