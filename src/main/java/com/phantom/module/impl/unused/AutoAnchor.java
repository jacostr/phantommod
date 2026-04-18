/* Copyright (c) 2026 PhantomMod. All rights reserved. */
package com.phantom.module.impl.combat;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import com.phantom.util.InventoryUtil;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Properties;

public class AutoAnchor extends Module {
    private int delay = 5;
    private boolean activateOnRightClick = false;
    private boolean autoSwitch = true;

    private int anchorPlaceClock = 0;
    private int anchorChargeClock = 0;

    public AutoAnchor() {
        super("AutoAnchor", "Automatically places and charges respawn anchors for combat.\nDetectability: Blatant", ModuleCategory.COMBAT, -1);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) return;

        if (anchorPlaceClock > 0) anchorPlaceClock--;
        if (anchorChargeClock > 0) anchorChargeClock--;

        if (activateOnRightClick && !mc.options.keyUse.isDown()) {
            return;
        }

        if (mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hit.getBlockPos();
            var state = mc.level.getBlockState(pos);

            if (state.getBlock() == Blocks.RESPAWN_ANCHOR) {
                if (anchorChargeClock == 0) {
                    int charges = 0;
                    try {
                        for (var prop : state.getProperties()) {
                            if (prop.getName().equals("charges")) {
                                charges = (Integer) state.getValue(prop);
                                break;
                            }
                        }
                    } catch (Exception ignored) {}
                    if (charges < 4) {
                        if (autoSwitch && mc.player.getMainHandItem().getItem() != Items.GLOWSTONE) {
                            for (int i = 0; i < 9; i++) {
                                if (mc.player.getInventory().getItem(i).getItem() == Items.GLOWSTONE) {
                                    InventoryUtil.setSelectedSlot(i);
                                    break;
                                }
                            }
                        }
                        if (mc.player.getMainHandItem().getItem() == Items.GLOWSTONE) {
                            anchorChargeClock = delay;
                            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
                            mc.player.swing(InteractionHand.MAIN_HAND);
                        }
                    }
                }
            } else {
                if (anchorPlaceClock == 0) {
                    if (autoSwitch && mc.player.getMainHandItem().getItem() != Items.RESPAWN_ANCHOR) {
                        for (int i = 0; i < 9; i++) {
                            if (mc.player.getInventory().getItem(i).getItem() == Items.RESPAWN_ANCHOR) {
                                InventoryUtil.setSelectedSlot(i);
                                break;
                            }
                        }
                    }
                    if (mc.player.getMainHandItem().getItem() == Items.RESPAWN_ANCHOR) {
                        anchorPlaceClock = delay;
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
                        mc.player.swing(InteractionHand.MAIN_HAND);
                    }
                }
            }
        }
    }

    public int getDelay() { return delay; }
    public void setDelay(double d) { delay = (int)d; saveConfig(); }
    public boolean isActivateOnRightClick() { return activateOnRightClick; }
    public void setActivateOnRightClick(boolean v) { activateOnRightClick = v; saveConfig(); }
    public boolean isAutoSwitch() { return autoSwitch; }
    public void setAutoSwitch(boolean v) { autoSwitch = v; saveConfig(); }

    @Override public boolean hasConfigurableSettings() { return true; }
    @Override public Screen createSettingsScreen(Screen parent) { return new ModuleSettingsScreen(parent, this); }

    @Override
    public void loadConfig(Properties p) {
        super.loadConfig(p);
        try { delay = Integer.parseInt(p.getProperty("autoanchor.delay", "5")); } catch (Exception ignored) {}
        activateOnRightClick = Boolean.parseBoolean(p.getProperty("autoanchor.onrightclick", "false"));
        autoSwitch = Boolean.parseBoolean(p.getProperty("autoanchor.autoswitch", "true"));
    }

    @Override
    public void saveConfig(Properties p) {
        super.saveConfig(p);
        p.setProperty("autoanchor.delay", Integer.toString(delay));
        p.setProperty("autoanchor.onrightclick", Boolean.toString(activateOnRightClick));
        p.setProperty("autoanchor.autoswitch", Boolean.toString(autoSwitch));
    }
}
