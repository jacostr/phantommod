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
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.scores.Team;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ESP extends Module {
    private static final double WALL_ESP_RANGE = 192.0D;
    private double chestRange = 64.0;

    // Entity ESP toggles
    private boolean playersEnabled = true;
    private boolean mobsEnabled = false;
    private boolean animalsEnabled = false;
    private boolean throughWalls = true;

    // Chest ESP toggles
    private boolean chestsEnabled = false;
    private boolean enderChestsEnabled = false;
    private boolean trappedChestsEnabled = false;


    public ESP() {
        super("ESP", "Team-colored 3D hitboxes with optional through-wall rendering.\nDetectability: Safe",
                ModuleCategory.PLAYER, GLFW.GLFW_KEY_Y);
    }

    @Override
    public void onRender(WorldRenderContext context) {
        if (mc.level == null || mc.player == null) {
            return;
        }

        MultiBufferSource consumers = context.consumers();
        if (consumers == null) {
            return;
        }

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.position();

        // ── Entity ESP (visible) ──────────────────────────────────────────────
        for (Entity entity : mc.level.getEntitiesOfClass(
                Entity.class,
                mc.player.getBoundingBox().inflate(WALL_ESP_RANGE),
                this::shouldHighlight)) {
            if (!mc.player.hasLineOfSight(entity)) {
                continue;
            }
            var visibleBuffer = consumers.getBuffer(RenderTypes.lines());
            renderEspShape(context, visibleBuffer, cameraPos, entity, getEspColor(entity));
        }

        if (consumers instanceof MultiBufferSource.BufferSource bufferSource) {
            bufferSource.endBatch(RenderTypes.lines());
        }

        // ── Entity ESP (through walls) ────────────────────────────────────────
        if (throughWalls) {
            GL11.glDepthFunc(GL11.GL_ALWAYS);
            try {
                for (Entity entity : mc.level.getEntitiesOfClass(
                        Entity.class,
                        mc.player.getBoundingBox().inflate(WALL_ESP_RANGE),
                        this::shouldHighlight)) {
                    if (mc.player.hasLineOfSight(entity)) {
                        continue;
                    }
                    var hiddenBuffer = consumers.getBuffer(RenderTypes.lines());
                    renderEspShape(context, hiddenBuffer, cameraPos, entity, getEspColor(entity));
                }

                if (consumers instanceof MultiBufferSource.BufferSource bufferSource) {
                    bufferSource.endBatch(RenderTypes.lines());
                }
            } finally {
                GL11.glDepthFunc(GL11.GL_LEQUAL);
            }
        }

        // ── Chest ESP ─────────────────────────────────────────────────────────
        boolean anyChestEnabled = chestsEnabled || enderChestsEnabled || trappedChestsEnabled;
        if (anyChestEnabled) {
            List<BlockPos> chestPositions = getNearbyChests();

            if (!chestPositions.isEmpty()) {
                GL11.glDepthFunc(GL11.GL_ALWAYS);
                try {
                    for (BlockPos pos : chestPositions) {
                        BlockEntity be = mc.level.getBlockEntity(pos);
                        int color = getChestColor(be);
                        if (color == 0) continue;

                        AABB box = new AABB(pos);
                        var chestBuffer = consumers.getBuffer(RenderTypes.lines());
                        renderEspBox(context, chestBuffer, cameraPos, box, color);
                    }

                    if (consumers instanceof MultiBufferSource.BufferSource bufferSource) {
                        bufferSource.endBatch(RenderTypes.lines());
                    }
                } finally {
                    GL11.glDepthFunc(GL11.GL_LEQUAL);
                }
            }
        }
    }

    // ── Rendering helpers ─────────────────────────────────────────────────────

    private void renderEspShape(WorldRenderContext context, com.mojang.blaze3d.vertex.VertexConsumer buffer,
            Vec3 cameraPos, Entity entity, int color) {
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

    private void renderEspBox(WorldRenderContext context, com.mojang.blaze3d.vertex.VertexConsumer buffer,
            Vec3 cameraPos, AABB box, int color) {
        ShapeRenderer.renderShape(
                context.matrices(),
                buffer,
                Shapes.create(box.inflate(0.02D)),
                -cameraPos.x,
                -cameraPos.y,
                -cameraPos.z,
                color,
                1.0F);
    }


    // ── Chest helpers ─────────────────────────────────────────────────────────

    private List<BlockPos> getNearbyChests() {
        List<BlockPos> result = new ArrayList<>();
        if (mc.level == null || mc.player == null) return result;

        BlockPos playerPos = mc.player.blockPosition();
        int range = (int) chestRange;

        BlockPos min = playerPos.offset(-range, -range, -range);
        BlockPos max = playerPos.offset( range,  range,  range);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (pos.distSqr(playerPos) > chestRange * chestRange) continue;

            BlockEntity be = mc.level.getBlockEntity(pos);
            if (be == null) continue;

            if (chestsEnabled        && be instanceof ChestBlockEntity
                    && !(be instanceof TrappedChestBlockEntity)) {
                result.add(pos.immutable());
            } else if (trappedChestsEnabled && be instanceof TrappedChestBlockEntity) {
                result.add(pos.immutable());
            } else if (enderChestsEnabled   && be instanceof EnderChestBlockEntity) {
                result.add(pos.immutable());
            }
        }
        return result;
    }

    /** Returns the ARGB highlight color for a given chest block entity, or 0 to skip. */
    private int getChestColor(BlockEntity be) {
        if (be instanceof TrappedChestBlockEntity && trappedChestsEnabled) {
            return 0xFFFF5555; // red  – trapped chests
        }
        if (be instanceof ChestBlockEntity && chestsEnabled) {
            return 0xFFFFAA00; // orange – normal chests
        }
        if (be instanceof EnderChestBlockEntity && enderChestsEnabled) {
            return 0xFFAA00FF; // purple – ender chests
        }
        return 0;
    }

    // ── Entity color helpers ──────────────────────────────────────────────────

    private int getEspColor(Entity entity) {
        if (!(entity instanceof Player player)) {
            return getColor(entity);
        }

        Integer teamColor = getScoreboardTeamColor(player);
        if (teamColor != null) {
            return teamColor;
        }

        Integer armorColor = getArmorColor(player);
        if (armorColor != null) {
            return 0xFF000000 | armorColor;
        }

        return getColor(entity);
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

    // ── shouldHighlight ───────────────────────────────────────────────────────

    private boolean shouldHighlight(Entity entity) {
        if (entity == mc.player) return false;
        if (entity instanceof LivingEntity living && !living.isAlive()) return false;
        if (AntiBot.isBot(entity)) return false;
        if (playersEnabled  && entity instanceof AbstractClientPlayer) return true;
        if (animalsEnabled  && (entity instanceof Animal || entity instanceof AgeableMob)) return true;
        return mobsEnabled  && entity instanceof Mob;
    }

    // ── Settings screen ───────────────────────────────────────────────────────

    @Override
    public boolean hasConfigurableSettings() {
        return true;
    }

    @Override
    public Screen createSettingsScreen(Screen parent) {
        return new ModuleSettingsScreen(parent, this);
    }

    // ── Getters / setters (used by ModuleSettingsScreen) ─────────────────────

    public boolean isPlayersEnabled()           { return playersEnabled; }
    public void setPlayersEnabled(boolean v)    { playersEnabled = v;    saveConfig(); }

    public boolean isMobsEnabled()              { return mobsEnabled; }
    public void setMobsEnabled(boolean v)       { mobsEnabled = v;       saveConfig(); }

    public boolean isAnimalsEnabled()           { return animalsEnabled; }
    public void setAnimalsEnabled(boolean v)    { animalsEnabled = v;    saveConfig(); }

    public boolean isThroughWalls()             { return throughWalls; }
    public void setThroughWalls(boolean v)      { throughWalls = v;      saveConfig(); }

    public boolean isChestsEnabled()            { return chestsEnabled; }
    public void setChestsEnabled(boolean v)     { chestsEnabled = v;     saveConfig(); }

    public boolean isEnderChestsEnabled()       { return enderChestsEnabled; }
    public void setEnderChestsEnabled(boolean v){ enderChestsEnabled = v; saveConfig(); }

    public boolean isTrappedChestsEnabled()     { return trappedChestsEnabled; }
    public void setTrappedChestsEnabled(boolean v){ trappedChestsEnabled = v; saveConfig(); }

    public double getChestRange()           { return chestRange; }
    public void setChestRange(double v)     { chestRange = Math.max(8.0, Math.min(128.0, v)); saveConfig(); }

    // ── Config persistence ────────────────────────────────────────────────────

    @Override
    public void loadConfig(Properties properties) {
        super.loadConfig(properties);
        playersEnabled       = Boolean.parseBoolean(properties.getProperty("esp.players",         Boolean.toString(playersEnabled)));
        mobsEnabled          = Boolean.parseBoolean(properties.getProperty("esp.mobs",            Boolean.toString(mobsEnabled)));
        animalsEnabled       = Boolean.parseBoolean(properties.getProperty("esp.animals",         Boolean.toString(animalsEnabled)));
        throughWalls         = Boolean.parseBoolean(properties.getProperty("esp.through_walls",   Boolean.toString(throughWalls)));
        chestsEnabled        = Boolean.parseBoolean(properties.getProperty("esp.chests",          Boolean.toString(chestsEnabled)));
        enderChestsEnabled   = Boolean.parseBoolean(properties.getProperty("esp.ender_chests",    Boolean.toString(enderChestsEnabled)));
        trappedChestsEnabled = Boolean.parseBoolean(properties.getProperty("esp.trapped_chests",  Boolean.toString(trappedChestsEnabled)));
        try { chestRange = Double.parseDouble(properties.getProperty("esp.chest_range", Double.toString(chestRange))); } catch (NumberFormatException ignored) {}
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("esp.players",        Boolean.toString(playersEnabled));
        properties.setProperty("esp.mobs",           Boolean.toString(mobsEnabled));
        properties.setProperty("esp.animals",        Boolean.toString(animalsEnabled));
        properties.setProperty("esp.through_walls",  Boolean.toString(throughWalls));
        properties.setProperty("esp.chests",         Boolean.toString(chestsEnabled));
        properties.setProperty("esp.ender_chests",   Boolean.toString(enderChestsEnabled));
        properties.setProperty("esp.trapped_chests", Boolean.toString(trappedChestsEnabled));
        properties.setProperty("esp.chest_range",     Double.toString(chestRange));
    }
}