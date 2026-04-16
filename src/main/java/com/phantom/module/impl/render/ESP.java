package com.phantom.module.impl.render;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import com.phantom.module.impl.player.AntiBot;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
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
import net.minecraft.world.scores.Team;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.Properties;

public class ESP extends Module {
    private static final double WALL_ESP_RANGE = 192.0D;



    public enum FallbackColor {
        AQUA(0xFF55FFFF, "Aqua"), RED(0xFFFF5555, "Red"), GREEN(0xFF55FF55, "Green"), 
        WHITE(0xFFFFFFFF, "White"), YELLOW(0xFFFFFF55, "Yellow"), PURPLE(0xFFFF55FF, "Purple");
        private final int color;
        private final String label;
        FallbackColor(int color, String label) { this.color = color; this.label = label; }
        public int getColor() { return color; }
        public String getLabel() { return label; }
    }

    private boolean playersEnabled = true;
    private boolean mobsEnabled = false;
    private boolean animalsEnabled = false;
    private boolean throughWalls = true;
    private FallbackColor fallbackColor = FallbackColor.AQUA;

    public ESP() {
        super("ESP", "Team-colored 3D entity hitboxes with through-wall rendering.\nDetectability: Safe",
                ModuleCategory.PLAYER, GLFW.GLFW_KEY_Y);
    }

    @Override
    public void onRender(WorldRenderContext context) {
        if (mc.level == null || mc.player == null) return;

        MultiBufferSource consumers = context.consumers();
        if (consumers == null) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.position();

        List<Entity> targets = mc.level.getEntitiesOfClass(
                Entity.class,
                mc.player.getBoundingBox().inflate(WALL_ESP_RANGE),
                this::shouldHighlight);

        // Visible pass (normal depth) — hitboxes only, full color
        for (Entity entity : targets) {
            if (!mc.player.hasLineOfSight(entity)) continue;
            var buf = consumers.getBuffer(RenderTypes.lines());
            renderEspShape(context, buf, cameraPos, entity, getEspColor(entity));
        }
        if (consumers instanceof MultiBufferSource.BufferSource bs) bs.endBatch(RenderTypes.lines());

        // Through-walls pass — dimmed hitboxes for entities not in line-of-sight
        if (throughWalls) {
            org.lwjgl.opengl.GL11.glDepthFunc(org.lwjgl.opengl.GL11.GL_ALWAYS);
            try {
                for (Entity entity : targets) {
                    if (mc.player.hasLineOfSight(entity)) continue; // already drawn above
                    var buf = consumers.getBuffer(RenderTypes.lines());
                    renderEspShape(context, buf, cameraPos, entity, dimColor(getEspColor(entity)));
                }
                if (consumers instanceof MultiBufferSource.BufferSource bs) bs.endBatch(RenderTypes.lines());
            } finally {
                org.lwjgl.opengl.GL11.glDepthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
            }
        }

        // End visibility passes
    }

    private int dimColor(int color) {
        int a = (color >> 24) & 0xFF;
        int r = (int)(((color >> 16) & 0xFF) * 0.6f);
        int g = (int)(((color >> 8)  & 0xFF) * 0.6f);
        int b = (int)(( color        & 0xFF) * 0.6f);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void renderEspShape(WorldRenderContext context, VertexConsumer buffer,
            Vec3 cameraPos, Entity entity, int color) {
        // Always use the team/armor color for the hitbox — never tint it with health.
        ShapeRenderer.renderShape(
                context.matrices(),
                buffer,
                Shapes.create(entity.getBoundingBox().inflate(0.05D)),
                -cameraPos.x,
                -cameraPos.y,
                -cameraPos.z,
                color,
                1.0F);
    }



    private int getEspColor(Entity entity) {
        if (!(entity instanceof Player player)) return getColor(entity);

        Integer teamColor = getScoreboardTeamColor(player);
        if (teamColor != null) return teamColor;

        Integer armorColor = getArmorColor(player);
        if (armorColor != null) return 0xFF000000 | armorColor;

        return fallbackColor.getColor();
    }

    private Integer getScoreboardTeamColor(Player player) {
        Team team = player.getTeam();
        if (team == null) return null;
        ChatFormatting formatting = team.getColor();
        if (formatting == null) return null;
        Integer color = formatting.getColor();
        return color == null ? null : 0xFF000000 | color;
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

    private int getColor(Entity entity) {
        if (entity instanceof AbstractClientPlayer) return 0xFF55FFFF;
        if (entity instanceof Animal || entity instanceof AgeableMob) return 0xFF55FF55;
        if (entity instanceof Mob) return 0xFFFF5555;
        return 0xFFFFFF55;
    }

    private boolean shouldHighlight(Entity entity) {
        if (entity == mc.player) return false;
        if (entity instanceof LivingEntity living && !living.isAlive()) return false;
        if (AntiBot.isBot(entity)) return false;
        if (playersEnabled  && entity instanceof AbstractClientPlayer) return true;
        if (animalsEnabled  && (entity instanceof Animal || entity instanceof AgeableMob)) return true;
        return mobsEnabled  && entity instanceof Mob;
    }

    @Override public boolean hasConfigurableSettings() { return true; }
    @Override public Screen createSettingsScreen(Screen parent) { return new ModuleSettingsScreen(parent, this); }

    public boolean isPlayersEnabled()        { return playersEnabled; }
    public void setPlayersEnabled(boolean v) { playersEnabled = v; saveConfig(); }
    public boolean isMobsEnabled()           { return mobsEnabled; }
    public void setMobsEnabled(boolean v)    { mobsEnabled = v; saveConfig(); }
    public boolean isAnimalsEnabled()        { return animalsEnabled; }
    public void setAnimalsEnabled(boolean v) { animalsEnabled = v; saveConfig(); }
    public boolean isThroughWalls()          { return throughWalls; }
    public void setThroughWalls(boolean v)   { throughWalls = v; saveConfig(); }
    public FallbackColor getFallbackColor() { return fallbackColor; }
    public void cycleFallbackColor() {
        fallbackColor = FallbackColor.values()[(fallbackColor.ordinal() + 1) % FallbackColor.values().length];
        saveConfig();
    }

    @Override
    public void loadConfig(Properties properties) {
        super.loadConfig(properties);
        playersEnabled = Boolean.parseBoolean(properties.getProperty("esp.players",       Boolean.toString(playersEnabled)));
        mobsEnabled    = Boolean.parseBoolean(properties.getProperty("esp.mobs",          Boolean.toString(mobsEnabled)));
        animalsEnabled = Boolean.parseBoolean(properties.getProperty("esp.animals",       Boolean.toString(animalsEnabled)));
        throughWalls   = Boolean.parseBoolean(properties.getProperty("esp.through_walls", Boolean.toString(throughWalls)));
        
        try { fallbackColor = FallbackColor.valueOf(properties.getProperty("esp.fallback_color", fallbackColor.name())); }
        catch (IllegalArgumentException ignored) {}
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("esp.players",       Boolean.toString(playersEnabled));
        properties.setProperty("esp.mobs",          Boolean.toString(mobsEnabled));
        properties.setProperty("esp.animals",       Boolean.toString(animalsEnabled));
        properties.setProperty("esp.through_walls", Boolean.toString(throughWalls));
        properties.setProperty("esp.fallback_color", fallbackColor.name());
    }
}