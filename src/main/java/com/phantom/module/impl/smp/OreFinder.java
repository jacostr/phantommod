/* Copyright (c) 2026 PhantomMod. All rights reserved. */
/*
 * OreFinder.java — Logs exact coordinates of nearby ores and provides direction hints.
 *
 * Scans for valuable blocks (Ancient Debris, Diamonds, Gold, etc.) within a radius.
 * Displays a list of the 8 nearest matches on the HUD with their exact X, Y, Z.
 * Provides relative direction hints (Ahead, Behind, Left, Right) to guide mining.
 * Detectability: Safe — purely client-side detection and HUD rendering.
 */
package com.phantom.module.impl.smp;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

public class OreFinder extends Module {
    private double range = 32.0;
    private boolean debrisOnly = true;
    private boolean showList = true;
    private boolean showSelfCoords = true;
    private boolean showDirections = true;

    private final List<BlockPos> foundOres = new ArrayList<>();
    private long lastScanTick = 0;

    public OreFinder() {
        super("OreFinder", "Logs exact coordinates of nearby ores and provides direction hints.\nDetectability: Safe",
                ModuleCategory.SMP, -1);
    }

    @Override
    public void onTick() {
        if (mc.level == null || mc.player == null) return;
        
        long currentTick = mc.level.getGameTime();
        if (currentTick - lastScanTick >= 40) { // Scan every 2 seconds
            lastScanTick = currentTick;
            scan();
        }
    }

    private void scan() {
        foundOres.clear();
        BlockPos playerPos = mc.player.blockPosition();
        int r = (int)range;

        for (BlockPos pos : BlockPos.betweenClosed(playerPos.offset(-r, -r, -r), playerPos.offset(r, r, r))) {
            BlockState state = mc.level.getBlockState(pos);
            if (isValuable(state.getBlock())) {
                foundOres.add(pos.immutable());
            }
        }

        // Sort by distance to player
        foundOres.sort(Comparator.comparingDouble(p -> p.distSqr(mc.player.blockPosition())));
    }

    private boolean isValuable(Block block) {
        if (block == Blocks.ANCIENT_DEBRIS) return true;
        
        if (debrisOnly) return false;
        
        return block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE ||
               block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE ||
               block == Blocks.NETHER_GOLD_ORE || block == Blocks.GILDED_BLACKSTONE ||
               block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE ||
               block == Blocks.NETHER_QUARTZ_ORE ||
               block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE ||
               block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE ||
               block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE ||
               block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE ||
               block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE;
    }

    @Override
    public void onHudRender(GuiGraphics graphics) {
        if (!showList || foundOres.isEmpty() || mc.options.hideGui) return;

        int y = 40;
        graphics.drawString(mc.font, "§6§lOre Finder", 10, y, 0xFFFFFFFF, true);
        y += 12;

        if (showSelfCoords) {
            BlockPos self = mc.player.blockPosition();
            graphics.drawString(mc.font, String.format("§7Self: §f%d, %d, %d", self.getX(), self.getY(), self.getZ()), 10, y, 0xFFFFFFFF, true);
            y += 10;
        }

        int count = 0;
        for (BlockPos pos : foundOres) {
            BlockState state = mc.level.getBlockState(pos);
            String name = getBlockName(state.getBlock());
            double dist = Math.sqrt(pos.distSqr(mc.player.blockPosition()));
            String direction = showDirections ? " §e[" + getRelativeDirection(pos) + "]" : "";
            
            String text = String.format("§7%s: §f%d, %d, %d §a(%.1fm)%s", 
                name, pos.getX(), pos.getY(), pos.getZ(), dist, direction);
            
            graphics.drawString(mc.font, text, 10, y, 0xFFFFFFFF, true);
            y += 10;
            if (++count >= 8) break;
        }
    }

    private String getBlockName(Block block) {
        if (block == Blocks.ANCIENT_DEBRIS) return "Debris";
        if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) return "Diamond";
        if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE) return "Gold";
        if (block == Blocks.NETHER_GOLD_ORE) return "Nether Gold";
        if (block == Blocks.GILDED_BLACKSTONE) return "Gilded Blackstone";
        if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) return "Emerald";
        if (block == Blocks.NETHER_QUARTZ_ORE) return "Quartz";
        if (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE) return "Lapis";
        if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) return "Redstone";
        if (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE) return "Coal";
        if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) return "Iron";
        if (block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE) return "Copper";
        return "Ore";
    }

    private String getRelativeDirection(BlockPos target) {
        if (mc.player == null) return "Unknown";
        
        StringBuilder dir = new StringBuilder();
        
        // Vertical
        int yDiff = target.getY() - mc.player.blockPosition().getY();
        if (yDiff > 1) dir.append("Above");
        else if (yDiff < -1) dir.append("Below");
        
        // Horizontal
        double dx = (target.getX() + 0.5) - mc.player.getX();
        double dz = (target.getZ() + 0.5) - mc.player.getZ();
        
        float angleToTarget = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float relativeAngle = Mth.wrapDegrees(angleToTarget - mc.player.getYRot());
        
        if (dir.length() > 0) dir.append("-");

        if (relativeAngle >= -22.5 && relativeAngle < 22.5) dir.append("Ahead");
        else if (relativeAngle >= 22.5 && relativeAngle < 67.5) dir.append("Ahead-Right");
        else if (relativeAngle >= 67.5 && relativeAngle < 112.5) dir.append("Right");
        else if (relativeAngle >= 112.5 && relativeAngle < 157.5) dir.append("Back-Right");
        else if (relativeAngle >= 157.5 || relativeAngle < -157.5) dir.append("Back");
        else if (relativeAngle >= -157.5 && relativeAngle < -112.5) dir.append("Back-Left");
        else if (relativeAngle >= -112.5 && relativeAngle < -67.5) dir.append("Left");
        else if (relativeAngle >= -67.5 && relativeAngle < -22.5) dir.append("Ahead-Left");
        
        return dir.toString();
    }

    @Override
    public void onDisable() {
        foundOres.clear();
    }

    public double getRange() { return range; }
    public void setRange(double v) { range = Mth.clamp(v, 8.0, 128.0); saveConfig(); }
    public boolean isDebrisOnly() { return debrisOnly; }
    public void setDebrisOnly(boolean v) { debrisOnly = v; saveConfig(); }
    public boolean isShowList() { return showList; }
    public void setShowList(boolean v) { showList = v; saveConfig(); }
    public boolean isShowSelfCoords() { return showSelfCoords; }
    public void setShowSelfCoords(boolean v) { showSelfCoords = v; saveConfig(); }
    public boolean isShowDirections() { return showDirections; }
    public void setShowDirections(boolean v) { showDirections = v; saveConfig(); }

    @Override public boolean hasConfigurableSettings() { return true; }
    @Override public Screen createSettingsScreen(Screen parent) { return new ModuleSettingsScreen(parent, this); }

    @Override
    public void loadConfig(Properties p) {
        super.loadConfig(p);
        try { range = Double.parseDouble(p.getProperty("orefinder.range", "32.0")); } catch (Exception ignored) {}
        debrisOnly = Boolean.parseBoolean(p.getProperty("orefinder.debris_only", "true"));
        showList = Boolean.parseBoolean(p.getProperty("orefinder.show_list", "true"));
        showSelfCoords = Boolean.parseBoolean(p.getProperty("orefinder.show_self", "true"));
        showDirections = Boolean.parseBoolean(p.getProperty("orefinder.show_directions", "true"));
    }

    @Override
    public void saveConfig(Properties p) {
        super.saveConfig(p);
        p.setProperty("orefinder.range", Double.toString(range));
        p.setProperty("orefinder.debris_only", Boolean.toString(debrisOnly));
        p.setProperty("orefinder.show_list", Boolean.toString(showList));
        p.setProperty("orefinder.show_self", Boolean.toString(showSelfCoords));
        p.setProperty("orefinder.show_directions", Boolean.toString(showDirections));
    }
}
