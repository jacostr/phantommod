/* Copyright (c) 2026 PhantomMod. All rights reserved. */
package com.phantom.module.impl.render;

import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.vertex.PoseStack;
import com.phantom.gui.ModuleSettingsScreen;

import java.text.DecimalFormat;
import java.util.Properties;

public class TNTTimer extends Module {
    private final DecimalFormat timeFormatter = new DecimalFormat("0.0");
    private double scale = 1.0;

    public TNTTimer() {
        super("TNTTimer", "Displays a countdown timer above primed TNT entities.", ModuleCategory.RENDER, -1);
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
    public void onRender(WorldRenderContext context) {
        if (mc.level == null || mc.player == null) return;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof PrimedTnt tnt) {
                renderTimer(context, tnt);
            }
        }
    }

    private void renderTimer(WorldRenderContext context, PrimedTnt tnt) {
        PoseStack matrices = context.matrices();
        Vec3 camPos = mc.gameRenderer.getMainCamera().position();
        Font font = mc.font;
        
        int fuse = tnt.getFuse();
        if (fuse <= 0) return;

        String text = timeFormatter.format(fuse / 20.0f) + "s";
        float partialTicks = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        
        double x = Mth.lerp(partialTicks, tnt.xo, tnt.getX()) - camPos.x;
        double y = Mth.lerp(partialTicks, tnt.yo, tnt.getY()) - camPos.y + tnt.getBbHeight() + 0.5;
        double z = Mth.lerp(partialTicks, tnt.zo, tnt.getZ()) - camPos.z;

        matrices.pushPose();
        matrices.translate(x, y, z);
        matrices.mulPose(mc.gameRenderer.getMainCamera().rotation());
        matrices.scale(-0.025f * (float)scale, -0.025f * (float)scale, 0.025f * (float)scale);

        float halfWidth = (float)(-font.width(text) / 2);
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        
        // Background
        font.drawInBatch(text, halfWidth, 0, 0xFFFFFFFF, false, matrices.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0x40000000, 0xF000F0);
        bufferSource.endBatch();

        matrices.popPose();
    }

    @Override
    public void loadConfig(Properties props) {
        super.loadConfig(props);
        scale = Double.parseDouble(props.getProperty("tnttimer.scale", "1.0"));
    }

    @Override
    public void saveConfig(Properties props) {
        super.saveConfig(props);
        props.setProperty("tnttimer.scale", Double.toString(scale));
    }

    public double getScale() { return scale; }
    public void setScale(double v) { scale = v; saveConfig(); }
}
