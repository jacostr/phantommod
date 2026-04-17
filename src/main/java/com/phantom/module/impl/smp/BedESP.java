/* Copyright (c) 2026 PhantomMod. All rights reserved. */
/*
 * BedESP.java — Highlights bed blocks through walls.
 *
 * Scans nearby blocks for bed block entities and renders colored wireframes.
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
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.lwjgl.opengl.GL11;
import com.phantom.util.ESPColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class BedESP extends Module {
    private double range = 64.0;
    private ESPColor colorMode = ESPColor.PINK;

    private List<BlockPos> cachedBeds = new ArrayList<>();
    private long lastScanTick = 0;

    public BedESP() {
        super("BedESP", "Highlights bed blocks through walls.\nDetectability: Safe",
                ModuleCategory.SMP, -1);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) return;
        long currentTick = mc.level.getGameTime();
        if (currentTick - lastScanTick >= 20) {
            lastScanTick = currentTick;
            rescan();
        }
    }

    private void rescan() {
        cachedBeds.clear();
        BlockPos playerPos = mc.player.blockPosition();
        int r = (int) range;
        for (BlockPos pos : BlockPos.betweenClosed(playerPos.offset(-r, -r, -r), playerPos.offset(r, r, r))) {
            if (pos.distSqr(playerPos) > range * range) continue;
            BlockState state = mc.level.getBlockState(pos);
            if (state.getBlock() instanceof BedBlock) {
                cachedBeds.add(pos.immutable());
            }
        }
    }

    @Override
    public void onRender(WorldRenderContext context) {
        if (mc.level == null || mc.player == null || cachedBeds.isEmpty()) return;
        MultiBufferSource consumers = context.consumers();
        if (consumers == null) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.position();

        GL11.glDepthFunc(GL11.GL_ALWAYS);
        try {
            for (BlockPos pos : cachedBeds) {
                AABB box = new AABB(pos);
                var buffer = consumers.getBuffer(RenderTypes.lines());
                ShapeRenderer.renderShape(
                        context.matrices(), buffer,
                        Shapes.create(box.inflate(0.02D)),
                        -cameraPos.x, -cameraPos.y, -cameraPos.z,
                        colorMode.getColor(), 1.0F); 
            }
            if (consumers instanceof MultiBufferSource.BufferSource bs) {
                bs.endBatch(RenderTypes.lines());
            }
        } finally {
            GL11.glDepthFunc(GL11.GL_LEQUAL);
        }
    }

    @Override
    public void onDisable() { cachedBeds.clear(); lastScanTick = 0; }

    public double getRange() { return range; }
    public void setRange(double v) { range = Mth.clamp(v, 8.0, 128.0); saveConfig(); }
    public ESPColor getColorMode() { return colorMode; }
    public void cycleColorMode() { colorMode = colorMode.next(); saveConfig(); }

    @Override public boolean hasConfigurableSettings() { return true; }
    @Override public Screen createSettingsScreen(Screen parent) { return new ModuleSettingsScreen(parent, this); }

    @Override
    public void loadConfig(Properties p) {
        super.loadConfig(p);
        try { range = Mth.clamp(Double.parseDouble(p.getProperty("bedesp.range", "64.0")), 8.0, 128.0); } catch (Exception ignored) {}
        colorMode = ESPColor.fromString(p.getProperty("bedesp.color"), ESPColor.PINK);
    }

    @Override
    public void saveConfig(Properties p) {
        super.saveConfig(p);
        p.setProperty("bedesp.range", Double.toString(range));
        p.setProperty("bedesp.color", colorMode.name());
    }
}
