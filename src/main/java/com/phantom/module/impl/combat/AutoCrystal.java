/* Copyright (c) 2026 PhantomMod. All rights reserved. */
package com.phantom.module.impl.combat;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.lwjgl.glfw.GLFW;

import java.util.Properties;

public class AutoCrystal extends Module {
    private int delay = 0;
    private boolean activateOnRightClick = false;

    private int crystalPlaceClock = 0;
    private int crystalBreakClock = 0;

    public AutoCrystal() {
        super("AutoCrystal", "Auto places and breaks crystals at your crosshair target.\nAdapted from MarlowClient.\nDetectability: Blatant", ModuleCategory.COMBAT, -1);
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
                crystalPlaceClock = delay;
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
                mc.player.swing(InteractionHand.MAIN_HAND);
            }
        }
    }

    public int getDelay() { return delay; }
    public void setDelay(double d) { delay = (int)d; saveConfig(); }
    public boolean isActivateOnRightClick() { return activateOnRightClick; }
    public void setActivateOnRightClick(boolean v) { activateOnRightClick = v; saveConfig(); }

    @Override public boolean hasConfigurableSettings() { return true; }
    @Override public Screen createSettingsScreen(Screen parent) { return new ModuleSettingsScreen(parent, this); }

    @Override
    public void loadConfig(Properties p) {
        super.loadConfig(p);
        try { delay = Integer.parseInt(p.getProperty("autocrystal.delay", "0")); } catch (Exception ignored) {}
        activateOnRightClick = Boolean.parseBoolean(p.getProperty("autocrystal.onrightclick", "false"));
    }

    @Override
    public void saveConfig(Properties p) {
        super.saveConfig(p);
        p.setProperty("autocrystal.delay", Integer.toString(delay));
        p.setProperty("autocrystal.onrightclick", Boolean.toString(activateOnRightClick));
    }
}
