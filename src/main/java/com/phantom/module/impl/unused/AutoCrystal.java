/* Copyright (c) 2026 PhantomMod. All rights reserved. */
package com.phantom.module.impl.combat;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import com.phantom.module.impl.player.AntiBot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Properties;

public class AutoCrystal extends Module {
    private enum TargetMode {
        DISTANCE("Distance"),
        HEALTH("Health"),
        DAMAGE("Damage");

        private final String label;
        TargetMode(String label) { this.label = label; }
        public String getLabel() { return label; }
        public TargetMode next() {
            return switch(this) {
                case DISTANCE -> HEALTH;
                case HEALTH -> DAMAGE;
                case DAMAGE -> DISTANCE;
            };
        }
    }

    private int delay = 0;
    private boolean activateOnRightClick = false;
    private double range = 4.5;
    private TargetMode targetMode = TargetMode.DISTANCE;
    private boolean antiSuicide = true;
    private boolean targetPlayers = true;
    private boolean targetMobs = true;

    private int crystalPlaceClock = 0;
    private int crystalBreakClock = 0;

    public AutoCrystal() {
        super("AutoCrystal", "Auto places and breaks crystals with range and target sorting.\nDetectability: Blatant", ModuleCategory.COMBAT, -1);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) return;

        if (crystalPlaceClock > 0) crystalPlaceClock--;
        if (crystalBreakClock > 0) crystalBreakClock--;

        if (activateOnRightClick && !mc.options.keyUse.isDown()) {
            return;
        }

        if (mc.player.getMainHandItem().getItem() != Items.END_CRYSTAL) {
            return;
        }

        if (mc.hitResult instanceof EntityHitResult hit) {
            if (crystalBreakClock == 0 && (hit.getEntity() instanceof EndCrystal || hit.getEntity() instanceof Slime)) {
                crystalBreakClock = delay;
                mc.gameMode.attack(mc.player, hit.getEntity());
                mc.player.swing(InteractionHand.MAIN_HAND);
            }
        } else if (mc.hitResult instanceof BlockHitResult hit) {
            if (crystalPlaceClock == 0) {
                BlockPos targetPos = hit.getBlockPos().above();
                if (isValidPlacePosition(targetPos)) {
                    crystalPlaceClock = delay;
                    mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
                    mc.player.swing(InteractionHand.MAIN_HAND);
                }
            }
        }
    }

    private boolean isValidPlacePosition(BlockPos pos) {
        if (mc.player.distanceToSqr(Vec3.atCenterOf(pos)) > range * range) {
            return false;
        }

        if (!mc.level.getBlockState(pos).isAir()) {
            return false;
        }

        if (!mc.level.getBlockState(pos.below()).isSolid()) {
            return false;
        }

        if (antiSuicide) {
            AABB crystalBox = new AABB(pos);
            List<LivingEntity> entities = mc.level.getEntitiesOfClass(LivingEntity.class, crystalBox.inflate(1.5));
            for (LivingEntity entity : entities) {
                if (entity == mc.player) return false;
                if (isTeammateTarget(entity)) return false;
                if (entity instanceof Player && !targetPlayers) return false;
                if (entity instanceof net.minecraft.world.entity.Mob && !targetMobs) return false;
            }
        }

        return true;
    }

    @Override
    public boolean hasConfigurableSettings() { return true; }
    @Override public Screen createSettingsScreen(Screen parent) { return new ModuleSettingsScreen(parent, this); }

    public int getDelay() { return delay; }
    public void setDelay(double d) { delay = (int)d; saveConfig(); }
    public boolean isActivateOnRightClick() { return activateOnRightClick; }
    public void setActivateOnRightClick(boolean v) { activateOnRightClick = v; saveConfig(); }
    public double getRange() { return range; }
    public void setRange(double v) { range = Math.max(1.0, Math.min(8.0, v)); saveConfig(); }
    public TargetMode getTargetMode() { return targetMode; }
    public void cycleTargetMode() { targetMode = targetMode.next(); saveConfig(); }
    public boolean isAntiSuicide() { return antiSuicide; }
    public void setAntiSuicide(boolean v) { antiSuicide = v; saveConfig(); }
    public boolean isTargetPlayers() { return targetPlayers; }
    public void setTargetPlayers(boolean v) { targetPlayers = v; saveConfig(); }
    public boolean isTargetMobs() { return targetMobs; }
    public void setTargetMobs(boolean v) { targetMobs = v; saveConfig(); }

    @Override
    public void loadConfig(Properties p) {
        super.loadConfig(p);
        try { delay = Integer.parseInt(p.getProperty("autocrystal.delay", "0")); } catch (Exception ignored) {}
        activateOnRightClick = Boolean.parseBoolean(p.getProperty("autocrystal.onrightclick", "false"));
        try { range = Double.parseDouble(p.getProperty("autocrystal.range", "4.5")); } catch (Exception ignored) {}
        try { targetMode = TargetMode.valueOf(p.getProperty("autocrystal.targetmode", "DISTANCE")); } catch (Exception ignored) {}
        antiSuicide = Boolean.parseBoolean(p.getProperty("autocrystal.antisuicide", "true"));
        targetPlayers = Boolean.parseBoolean(p.getProperty("autocrystal.targetplayers", "true"));
        targetMobs = Boolean.parseBoolean(p.getProperty("autocrystal.targetmobs", "true"));
    }

    @Override
    public void saveConfig(Properties p) {
        super.saveConfig(p);
        p.setProperty("autocrystal.delay", Integer.toString(delay));
        p.setProperty("autocrystal.onrightclick", Boolean.toString(activateOnRightClick));
        p.setProperty("autocrystal.range", Double.toString(range));
        p.setProperty("autocrystal.targetmode", targetMode.name());
        p.setProperty("autocrystal.antisuicide", Boolean.toString(antiSuicide));
        p.setProperty("autocrystal.targetplayers", Boolean.toString(targetPlayers));
        p.setProperty("autocrystal.targetmobs", Boolean.toString(targetMobs));
    }
}