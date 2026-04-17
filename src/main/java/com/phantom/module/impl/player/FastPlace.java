/* Copyright (c) 2026 PhantomMod. All rights reserved. */
/*
 * FastPlace.java — Reduces vanilla right-click delay for placing blocks or using items.
 *
 * Primarily intended for block placement, but it can also speed up other held-use
 * actions if the "blocks only" guard is disabled.
 * Detectability: Subtle to Obvious — aggressive values can create very unnatural
 * placement timing on servers that inspect click cadence.
 */
package com.phantom.module.impl.player;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.mixin.MinecraftClientAccessor;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.BlockItem;

import java.util.Properties;

public class FastPlace extends Module {
    private int delayTicks;
    private boolean blocksOnly = true;

    public FastPlace() {
        super("FastPlace",
                "Reduces the vanilla right-click delay when placing blocks or using items.\nDetectability: Obvious at very low delays",
                ModuleCategory.PLAYER,
                -1);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.options == null || mc.screen != null || !mc.options.keyUse.isDown()) {
            return;
        }

        if (blocksOnly && !(mc.player.getMainHandItem().getItem() instanceof BlockItem)
                && !(mc.player.getOffhandItem().getItem() instanceof BlockItem)) {
            return;
        }

        MinecraftClientAccessor accessor = (MinecraftClientAccessor) mc;
        if (accessor.phantom$getRightClickDelay() > delayTicks) {
            accessor.phantom$setRightClickDelay(delayTicks);
        }
    }

    public int getDelayTicks() {
        return delayTicks;
    }

    public void setDelayTicks(int delayTicks) {
        this.delayTicks = Math.max(0, Math.min(4, delayTicks));
        saveConfig();
    }

    public boolean isBlocksOnly() {
        return blocksOnly;
    }

    public void setBlocksOnly(boolean blocksOnly) {
        this.blocksOnly = blocksOnly;
        saveConfig();
    }

    public void applyPresetLegit() {
        setDelayTicks(3);
        setBlocksOnly(true);
    }

    public void applyPresetNormal() {
        setDelayTicks(2);
        setBlocksOnly(true);
    }

    public void applyPresetObvious() {
        setDelayTicks(1);
        setBlocksOnly(true);
    }

    public void applyPresetBlatant() {
        setDelayTicks(0);
        setBlocksOnly(false);
    }

    @Override
    public boolean hasConfigurableSettings() {
        return true;
    }

    @Override
    public Screen createSettingsScreen(Screen parent) {
        return new ModuleSettingsScreen(parent, this);
    }

    @Override
    public void loadConfig(Properties properties) {
        super.loadConfig(properties);
        String delay = properties.getProperty("fastplace.delay_ticks");
        if (delay != null) {
            try {
                this.delayTicks = Math.max(0, Math.min(4, Integer.parseInt(delay.trim())));
            } catch (NumberFormatException ignored) {
            }
        }

        blocksOnly = Boolean.parseBoolean(properties.getProperty("fastplace.blocks_only", Boolean.toString(blocksOnly)));
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("fastplace.delay_ticks", Integer.toString(delayTicks));
        properties.setProperty("fastplace.blocks_only", Boolean.toString(blocksOnly));
    }
}
