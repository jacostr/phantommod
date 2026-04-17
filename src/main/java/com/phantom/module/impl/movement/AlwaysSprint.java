/* Copyright (c) 2026 PhantomMod. All rights reserved. */
/*
 * AlwaysSprint.java — Automatically keeps the player sprinting (Movement module).
 *
 * Sets mc.player.setSprinting(true) every tick when moving forward and sprint is
 * normally allowed (not too hungry, not in liquid, etc.). Eliminates the need to
 * double-tap or hold a sprint key.
 * Detectability: Safe — vanilla sprint behaviour, just automated.
 */
package com.phantom.module.impl.movement;

import com.phantom.PhantomMod;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import com.phantom.module.impl.combat.WTap;

import java.util.Properties;

public class AlwaysSprint extends Module {
    private boolean allowUsingItem = false;
    private boolean allowBackwards = false;
    private boolean allowSideways = false;
    private boolean allowInInventory = false;

    public AlwaysSprint() {
        super("AlwaysSprint",
                "Keeps player sprinting automatically when moving forward.\nIncludes omni-sprint options.\nDetectability: Safe/Subtle",
                ModuleCategory.MOVEMENT, -1);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.player.input == null || mc.options == null)
            return;

        WTap wTap = PhantomMod.getModuleManager().getModuleByClass(WTap.class);
        if (wTap != null && wTap.isEnabled() && wTap.isTapActive()) {
            mc.player.setSprinting(false);
            return;
        }

        boolean inGame = mc.screen == null;
        boolean inInv = allowInInventory && mc.screen != null;
        if (!inGame && !inInv) {
            return;
        }

        boolean movingForward = mc.player.input.hasForwardImpulse() || mc.options.keyUp.isDown();
        boolean movingBackwards = mc.options.keyDown.isDown();
        boolean movingSideways = mc.options.keyLeft.isDown() || mc.options.keyRight.isDown();

        boolean isMoving = movingForward || 
                (allowBackwards && movingBackwards) || 
                (allowSideways && movingSideways);

        boolean usingItemMatches = allowUsingItem || !mc.player.isUsingItem();

        boolean canSprint = isMoving &&
                !mc.player.horizontalCollision &&
                !mc.player.isShiftKeyDown() &&
                mc.player.getFoodData().getFoodLevel() > 6 &&
                usingItemMatches &&
                !mc.player.isInWater() &&
                !mc.player.isInLava() &&
                !mc.player.getAbilities().flying;

        if (canSprint) {
            mc.player.setSprinting(true);
        }
    }

    public boolean isAllowUsingItem() { return allowUsingItem; }
    public void setAllowUsingItem(boolean allowUsingItem) { 
        this.allowUsingItem = allowUsingItem; 
        saveConfig(); 
    }

    public boolean isAllowBackwards() { return allowBackwards; }
    public void setAllowBackwards(boolean allowBackwards) { 
        this.allowBackwards = allowBackwards; 
        saveConfig(); 
    }

    public boolean isAllowSideways() { return allowSideways; }
    public void setAllowSideways(boolean allowSideways) { 
        this.allowSideways = allowSideways; 
        saveConfig(); 
    }

    public boolean isAllowInInventory() { return allowInInventory; }
    public void setAllowInInventory(boolean allowInInventory) { 
        this.allowInInventory = allowInInventory; 
        saveConfig(); 
    }

    @Override
    public void loadConfig(Properties properties) {
        super.loadConfig(properties);
        allowUsingItem = Boolean.parseBoolean(properties.getProperty("alwayssprint.using_item", Boolean.toString(allowUsingItem)));
        allowBackwards = Boolean.parseBoolean(properties.getProperty("alwayssprint.backwards", Boolean.toString(allowBackwards)));
        allowSideways = Boolean.parseBoolean(properties.getProperty("alwayssprint.sideways", Boolean.toString(allowSideways)));
        allowInInventory = Boolean.parseBoolean(properties.getProperty("alwayssprint.in_inventory", Boolean.toString(allowInInventory)));
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("alwayssprint.using_item", Boolean.toString(allowUsingItem));
        properties.setProperty("alwayssprint.backwards", Boolean.toString(allowBackwards));
        properties.setProperty("alwayssprint.sideways", Boolean.toString(allowSideways));
        properties.setProperty("alwayssprint.in_inventory", Boolean.toString(allowInInventory));
    }
}
