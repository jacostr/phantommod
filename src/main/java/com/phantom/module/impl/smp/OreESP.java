/* Copyright (c) 2026 PhantomMod. All rights reserved. */
/*
 * OreESP.java — Highlights ore blocks through walls.
 *
 * Scans loaded chunks for diamond ore, redstone ore, ancient debris, lapis lazuli, and coal,
 * plus nether-specific ores like quartz and gold.
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
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.lwjgl.opengl.GL11;
import com.phantom.util.ESPColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class OreESP extends Module {
    private double range = 32.0;
    
    // Overworld Ores
    private boolean diamondEnabled = true;
    private ESPColor diamondColor = ESPColor.CYAN;
    private boolean goldEnabled = true;
    private ESPColor goldColor = ESPColor.YELLOW;
    private boolean emeraldEnabled = true;
    private ESPColor emeraldColor = ESPColor.GREEN;
    private boolean redstoneEnabled = false;
    private ESPColor redstoneColor = ESPColor.RED;
    private boolean lapisEnabled = true;
    private ESPColor lapisColor = ESPColor.BLUE;
    private boolean coalEnabled = false;
    private ESPColor coalColor = ESPColor.WHITE;
    
    // Nether Ores
    private boolean ancientDebrisEnabled = true;
    private ESPColor ancientDebrisColor = ESPColor.ORANGE;
    private boolean netherGoldEnabled = true;
    private ESPColor netherGoldColor = ESPColor.YELLOW;
    private boolean quartzEnabled = false;
    private ESPColor quartzColor = ESPColor.WHITE;

    // Cache to avoid scanning every frame
    private List<OreEntry> cachedOres = new ArrayList<>();
    private long lastScanTick = 0;
    private static final int SCAN_INTERVAL = 20;
    private boolean onlyCaves = false;

    private record OreEntry(BlockPos pos, int color) {}

    public OreESP() {
        super("OreESP", "Highlights ore blocks through walls.\nDetectability: Safe",
                ModuleCategory.SMP, -1);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) return;
        long currentTick = mc.level.getGameTime();
        if (currentTick - lastScanTick >= SCAN_INTERVAL) {
            lastScanTick = currentTick;
            rescanOres();
        }
    }

    private void rescanOres() {
        cachedOres.clear();
        BlockPos playerPos = mc.player.blockPosition();
        int r = (int) range;
        BlockPos min = playerPos.offset(-r, -r, -r);
        BlockPos max = playerPos.offset(r, r, r);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (pos.distSqr(playerPos) > range * range) continue;
            BlockState state = mc.level.getBlockState(pos);
            int color = getOreColor(state.getBlock());
            if (color == 0) continue;
            if (onlyCaves && !hasExposedFace(pos)) continue;
            cachedOres.add(new OreEntry(pos.immutable(), color));
        }
    }

    private boolean hasExposedFace(BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockState neighbor = mc.level.getBlockState(pos.relative(dir));
            if (neighbor.isAir()) return true;
        }
        return false;
    }

    private int getOreColor(Block block) {
        // Diamonds
        if (diamondEnabled && (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE)) {
            return diamondColor.getColor();
        }
        // Gold
        if (goldEnabled && (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE)) {
            return goldColor.getColor();
        }
        // Emeralds
        if (emeraldEnabled && (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE)) {
            return emeraldColor.getColor();
        }
        // Redstone
        if (redstoneEnabled && (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE)) {
            return redstoneColor.getColor();
        }
        // Lapis
        if (lapisEnabled && (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE)) {
            return lapisColor.getColor();
        }
        // Coal
        if (coalEnabled && (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE)) {
            return coalColor.getColor();
        }
        // Nether Ores
        if (ancientDebrisEnabled && block == Blocks.ANCIENT_DEBRIS) {
            return ancientDebrisColor.getColor();
        }
        if (netherGoldEnabled && (block == Blocks.NETHER_GOLD_ORE || block == Blocks.GILDED_BLACKSTONE)) {
            return netherGoldColor.getColor();
        }
        if (quartzEnabled && block == Blocks.NETHER_QUARTZ_ORE) {
            return quartzColor.getColor();
        }
        return 0;
    }

    @Override
    public void onRender(WorldRenderContext context) {
        if (mc.level == null || mc.player == null || cachedOres.isEmpty()) return;
        MultiBufferSource consumers = context.consumers();
        if (consumers == null) return;
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.position();
        GL11.glDepthFunc(GL11.GL_ALWAYS);
        try {
            for (OreEntry entry : cachedOres) {
                AABB box = new AABB(entry.pos);
                var buffer = consumers.getBuffer(RenderTypes.lines());
                ShapeRenderer.renderShape(
                        context.matrices(), buffer,
                        Shapes.create(box.inflate(0.02D)),
                        -cameraPos.x, -cameraPos.y, -cameraPos.z,
                        entry.color, 1.0F);
            }
            if (consumers instanceof MultiBufferSource.BufferSource bs) {
                bs.endBatch(RenderTypes.lines());
            }
        } finally {
            GL11.glDepthFunc(GL11.GL_LEQUAL);
        }
    }

    @Override
    public void onDisable() {
        cachedOres.clear();
        lastScanTick = 0;
    }

    public double getRange() { return range; }
    public void setRange(double v) { range = Mth.clamp(v, 8.0, 128.0); saveConfig(); }
    public boolean isDiamondEnabled() { return diamondEnabled; }
    public void setDiamondEnabled(boolean v) { diamondEnabled = v; saveConfig(); }
    public ESPColor getDiamondColor() { return diamondColor; }
    public void cycleDiamondColor() { diamondColor = diamondColor.next(); saveConfig(); }
    public boolean isGoldEnabled() { return goldEnabled; }
    public void setGoldEnabled(boolean v) { goldEnabled = v; saveConfig(); }
    public ESPColor getGoldColor() { return goldColor; }
    public void cycleGoldColor() { goldColor = goldColor.next(); saveConfig(); }
    public boolean isEmeraldEnabled() { return emeraldEnabled; }
    public void setEmeraldEnabled(boolean v) { emeraldEnabled = v; saveConfig(); }
    public ESPColor getEmeraldColor() { return emeraldColor; }
    public void cycleEmeraldColor() { emeraldColor = emeraldColor.next(); saveConfig(); }
    public boolean isRedstoneEnabled() { return redstoneEnabled; }
    public void setRedstoneEnabled(boolean v) { redstoneEnabled = v; saveConfig(); }
    public ESPColor getRedstoneColor() { return redstoneColor; }
    public void cycleRedstoneColor() { redstoneColor = redstoneColor.next(); saveConfig(); }
    public boolean isAncientDebrisEnabled() { return ancientDebrisEnabled; }
    public void setAncientDebrisEnabled(boolean v) { ancientDebrisEnabled = v; saveConfig(); }
    public ESPColor getAncientDebrisColor() { return ancientDebrisColor; }
    public void cycleAncientDebrisColor() { ancientDebrisColor = ancientDebrisColor.next(); saveConfig(); }
    public boolean isNetherGoldEnabled() { return netherGoldEnabled; }
    public void setNetherGoldEnabled(boolean v) { netherGoldEnabled = v; saveConfig(); }
    public ESPColor getNetherGoldColor() { return netherGoldColor; }
    public void cycleNetherGoldColor() { netherGoldColor = netherGoldColor.next(); saveConfig(); }
    public boolean isQuartzEnabled() { return quartzEnabled; }
    public void setQuartzEnabled(boolean v) { quartzEnabled = v; saveConfig(); }
    public ESPColor getQuartzColor() { return quartzColor; }
    public void cycleQuartzColor() { quartzColor = quartzColor.next(); saveConfig(); }
    public boolean isLapisEnabled() { return lapisEnabled; }
    public void setLapisEnabled(boolean v) { lapisEnabled = v; saveConfig(); }
    public ESPColor getLapisColor() { return lapisColor; }
    public void cycleLapisColor() { lapisColor = lapisColor.next(); saveConfig(); }
    public boolean isCoalEnabled() { return coalEnabled; }
    public void setCoalEnabled(boolean v) { coalEnabled = v; saveConfig(); }
    public ESPColor getCoalColor() { return coalColor; }
    public void cycleCoalColor() { coalColor = coalColor.next(); saveConfig(); }
    public boolean isOnlyCaves() { return onlyCaves; }
    public void setOnlyCaves(boolean v) { onlyCaves = v; saveConfig(); }

    @Override public boolean hasConfigurableSettings() { return true; }
    @Override public Screen createSettingsScreen(Screen parent) { return new ModuleSettingsScreen(parent, this); }

    @Override
    public void loadConfig(Properties p) {
        super.loadConfig(p);
        try { range = Mth.clamp(Double.parseDouble(p.getProperty("oreesp.range", "32.0")), 8.0, 128.0); } catch (Exception ignored) {}
        diamondEnabled = Boolean.parseBoolean(p.getProperty("oreesp.diamond", Boolean.toString(diamondEnabled)));
        diamondColor = ESPColor.fromString(p.getProperty("oreesp.diamond.color"), ESPColor.CYAN);
        goldEnabled = Boolean.parseBoolean(p.getProperty("oreesp.gold", Boolean.toString(goldEnabled)));
        goldColor = ESPColor.fromString(p.getProperty("oreesp.gold.color"), ESPColor.YELLOW);
        emeraldEnabled = Boolean.parseBoolean(p.getProperty("oreesp.emerald", Boolean.toString(emeraldEnabled)));
        emeraldColor = ESPColor.fromString(p.getProperty("oreesp.emerald.color"), ESPColor.GREEN);
        redstoneEnabled = Boolean.parseBoolean(p.getProperty("oreesp.redstone", Boolean.toString(redstoneEnabled)));
        redstoneColor = ESPColor.fromString(p.getProperty("oreesp.redstone.color"), ESPColor.RED);
        ancientDebrisEnabled = Boolean.parseBoolean(p.getProperty("oreesp.debris", Boolean.toString(ancientDebrisEnabled)));
        ancientDebrisColor = ESPColor.fromString(p.getProperty("oreesp.debris.color"), ESPColor.ORANGE);
        netherGoldEnabled = Boolean.parseBoolean(p.getProperty("oreesp.nether_gold", Boolean.toString(netherGoldEnabled)));
        netherGoldColor = ESPColor.fromString(p.getProperty("oreesp.nether_gold.color"), ESPColor.YELLOW);
        quartzEnabled = Boolean.parseBoolean(p.getProperty("oreesp.quartz", Boolean.toString(quartzEnabled)));
        quartzColor = ESPColor.fromString(p.getProperty("oreesp.quartz.color"), ESPColor.WHITE);
        lapisEnabled = Boolean.parseBoolean(p.getProperty("oreesp.lapis", Boolean.toString(lapisEnabled)));
        lapisColor = ESPColor.fromString(p.getProperty("oreesp.lapis.color"), ESPColor.BLUE);
        coalEnabled = Boolean.parseBoolean(p.getProperty("oreesp.coal", Boolean.toString(coalEnabled)));
        coalColor = ESPColor.fromString(p.getProperty("oreesp.coal.color"), ESPColor.WHITE);
        onlyCaves = Boolean.parseBoolean(p.getProperty("oreesp.only_caves", Boolean.toString(onlyCaves)));
    }

    @Override
    public void saveConfig(Properties p) {
        super.saveConfig(p);
        p.setProperty("oreesp.range", Double.toString(range));
        p.setProperty("oreesp.diamond", Boolean.toString(diamondEnabled));
        p.setProperty("oreesp.diamond.color", diamondColor.name());
        p.setProperty("oreesp.gold", Boolean.toString(goldEnabled));
        p.setProperty("oreesp.gold.color", goldColor.name());
        p.setProperty("oreesp.emerald", Boolean.toString(emeraldEnabled));
        p.setProperty("oreesp.emerald.color", emeraldColor.name());
        p.setProperty("oreesp.redstone", Boolean.toString(redstoneEnabled));
        p.setProperty("oreesp.redstone.color", redstoneColor.name());
        p.setProperty("oreesp.debris", Boolean.toString(ancientDebrisEnabled));
        p.setProperty("oreesp.debris.color", ancientDebrisColor.name());
        p.setProperty("oreesp.nether_gold", Boolean.toString(netherGoldEnabled));
        p.setProperty("oreesp.nether_gold.color", netherGoldColor.name());
        p.setProperty("oreesp.quartz", Boolean.toString(quartzEnabled));
        p.setProperty("oreesp.quartz.color", quartzColor.name());
        p.setProperty("oreesp.lapis", Boolean.toString(lapisEnabled));
        p.setProperty("oreesp.lapis.color", lapisColor.name());
        p.setProperty("oreesp.coal", Boolean.toString(coalEnabled));
        p.setProperty("oreesp.coal.color", coalColor.name());
        p.setProperty("oreesp.only_caves", Boolean.toString(onlyCaves));
    }
}
