/* Copyright (c) 2026 PhantomMod. All rights reserved. */
/*
 * ChestESP.java — Highlights chests, ender chests, and trapped chests through walls.
 *
 * Scans nearby block entities within range and draws colored outlines.
 * Normal chests = orange, trapped = red, ender = purple.
 * Detectability: Safe — purely client-side rendering.
 */
package com.phantom.module.impl.smp;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.lwjgl.opengl.GL11;
import com.mojang.blaze3d.vertex.PoseStack;
import com.phantom.util.ESPColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ChestESP extends Module {
    private double range = 64.0;
    private boolean chestsEnabled = true;
    private ESPColor chestsColor = ESPColor.ORANGE;
    private boolean enderChestsEnabled = true;
    private ESPColor enderChestsColor = ESPColor.PURPLE;
    private boolean trappedChestsEnabled = true;
    private ESPColor trappedChestsColor = ESPColor.RED;

    public ChestESP() {
        super("ChestESP", "Highlights chests through walls.\nDetectability: Safe",
                ModuleCategory.SMP, -1);
    }

    @Override
    public void onRender(WorldRenderContext context) {
        if (mc.level == null || mc.player == null) return;

        MultiBufferSource consumers = context.consumers();
        if (consumers == null) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.position();

        List<BlockPos> positions = getNearbyChests();
        if (positions.isEmpty()) return;

        GL11.glDepthFunc(GL11.GL_ALWAYS);
        try {
            for (BlockPos pos : positions) {
                BlockEntity be = mc.level.getBlockEntity(pos);
                int color = getChestColor(be);
                if (color == 0) continue;

                AABB box = new AABB(pos);
                var buffer = consumers.getBuffer(RenderTypes.lines());
                ShapeRenderer.renderShape(
                        context.matrices(), buffer,
                        Shapes.create(box.inflate(0.02D)),
                        -cameraPos.x, -cameraPos.y, -cameraPos.z,
                        color, 1.0F);
            }
            if (consumers instanceof MultiBufferSource.BufferSource bs) {
                bs.endBatch(RenderTypes.lines());
            }
        } finally {
            GL11.glDepthFunc(GL11.GL_LEQUAL);
        }
    }

    private List<BlockPos> getNearbyChests() {
        List<BlockPos> result = new ArrayList<>();
        BlockPos playerPos = mc.player.blockPosition();
        int r = (int) range;
        BlockPos min = playerPos.offset(-r, -r, -r);
        BlockPos max = playerPos.offset(r, r, r);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (pos.distSqr(playerPos) > range * range) continue;
            BlockEntity be = mc.level.getBlockEntity(pos);
            if (be == null) continue;
            if (chestsEnabled && be instanceof ChestBlockEntity && !(be instanceof TrappedChestBlockEntity)) {
                result.add(pos.immutable());
            } else if (trappedChestsEnabled && be instanceof TrappedChestBlockEntity) {
                result.add(pos.immutable());
            } else if (enderChestsEnabled && be instanceof EnderChestBlockEntity) {
                result.add(pos.immutable());
            }
        }
        return result;
    }

    private int getChestColor(BlockEntity be) {
        if (be instanceof TrappedChestBlockEntity && trappedChestsEnabled) return trappedChestsColor.getColor();
        if (be instanceof ChestBlockEntity && chestsEnabled) return chestsColor.getColor();
        if (be instanceof EnderChestBlockEntity && enderChestsEnabled) return enderChestsColor.getColor();
        return 0;
    }

    public double getRange() { return range; }
    public void setRange(double v) { range = Mth.clamp(v, 8.0, 128.0); saveConfig(); }
    public boolean isChestsEnabled() { return chestsEnabled; }
    public void setChestsEnabled(boolean v) { chestsEnabled = v; saveConfig(); }
    public ESPColor getChestsColor() { return chestsColor; }
    public void cycleChestsColor() { chestsColor = chestsColor.next(); saveConfig(); }
    public boolean isEnderChestsEnabled() { return enderChestsEnabled; }
    public void setEnderChestsEnabled(boolean v) { enderChestsEnabled = v; saveConfig(); }
    public ESPColor getEnderChestsColor() { return enderChestsColor; }
    public void cycleEnderChestsColor() { enderChestsColor = enderChestsColor.next(); saveConfig(); }
    public boolean isTrappedChestsEnabled() { return trappedChestsEnabled; }
    public void setTrappedChestsEnabled(boolean v) { trappedChestsEnabled = v; saveConfig(); }
    public ESPColor getTrappedChestsColor() { return trappedChestsColor; }
    public void cycleTrappedChestsColor() { trappedChestsColor = trappedChestsColor.next(); saveConfig(); }

    @Override public boolean hasConfigurableSettings() { return true; }
    @Override public Screen createSettingsScreen(Screen parent) { return new ModuleSettingsScreen(parent, this); }

    @Override
    public void loadConfig(Properties p) {
        super.loadConfig(p);
        try { range = Mth.clamp(Double.parseDouble(p.getProperty("chestesp.range", "64.0")), 8.0, 128.0); } catch (Exception ignored) {}
        chestsEnabled = Boolean.parseBoolean(p.getProperty("chestesp.chests", Boolean.toString(chestsEnabled)));
        chestsColor = ESPColor.fromString(p.getProperty("chestesp.chests.color"), ESPColor.ORANGE);
        enderChestsEnabled = Boolean.parseBoolean(p.getProperty("chestesp.ender", Boolean.toString(enderChestsEnabled)));
        enderChestsColor = ESPColor.fromString(p.getProperty("chestesp.ender.color"), ESPColor.PURPLE);
        trappedChestsEnabled = Boolean.parseBoolean(p.getProperty("chestesp.trapped", Boolean.toString(trappedChestsEnabled)));
        trappedChestsColor = ESPColor.fromString(p.getProperty("chestesp.trapped.color"), ESPColor.RED);
    }

    @Override
    public void saveConfig(Properties p) {
        super.saveConfig(p);
        p.setProperty("chestesp.range", Double.toString(range));
        p.setProperty("chestesp.chests", Boolean.toString(chestsEnabled));
        p.setProperty("chestesp.chests.color", chestsColor.name());
        p.setProperty("chestesp.ender", Boolean.toString(enderChestsEnabled));
        p.setProperty("chestesp.ender.color", enderChestsColor.name());
        p.setProperty("chestesp.trapped", Boolean.toString(trappedChestsEnabled));
        p.setProperty("chestesp.trapped.color", trappedChestsColor.name());
    }
}
