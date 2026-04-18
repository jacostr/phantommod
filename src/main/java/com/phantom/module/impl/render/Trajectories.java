/* Copyright (c) 2026 PhantomMod. All rights reserved. */
package com.phantom.module.impl.render;

import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.phantom.gui.ModuleSettingsScreen;

import com.phantom.util.ESPColor;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Trajectories extends Module {
    private boolean onlyWhenDrawing = true;
    private double maxTicks = 100.0;
    private ESPColor color = ESPColor.GREEN;
    private float thickness = 2.0f;

    public Trajectories() {
        super("Trajectories", "Visualizes the path of projectiles for bows, crossbows, pearls, and potions.", ModuleCategory.RENDER, -1);
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
        if (mc.player == null || mc.level == null) return;

        ItemStack stack = mc.player.getMainHandItem();
        if (stack.isEmpty()) stack = mc.player.getOffhandItem();
        if (stack.isEmpty()) return;

        Item item = stack.getItem();
        if (!isProjectile(item)) return;
        if (onlyWhenDrawing && item instanceof BowItem && !mc.player.isUsingItem()) return;

        double velocity = getVelocity(stack);
        double gravity = getGravity(item);
        double drag = getDrag(item);

        List<Vec3> points = calculateTrajectory(mc.player, velocity, gravity, drag);
        if (points.isEmpty()) return;

        renderPath(context, points);
    }

    private boolean isProjectile(Item item) {
        return item instanceof BowItem || item instanceof CrossbowItem || item instanceof EnderpearlItem ||
               item instanceof PotionItem || item instanceof SnowballItem || item instanceof EggItem ||
               item instanceof ExperienceBottleItem;
    }

    private double getVelocity(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof BowItem) {
            int useCount = mc.player.getTicksUsingItem();
            float f = (float)useCount / 20.0F;
            f = (f * f + f * 2.0F) / 3.0F;
            if (f > 1.0F) f = 1.0F;
            return f * 3.0F;
        } else if (item instanceof CrossbowItem) {
            return 3.15;
        } else if (item instanceof PotionItem) {
            return 0.5;
        } else if (item instanceof ExperienceBottleItem) {
            return 0.7;
        }
        return 1.5; // Snowballs, eggs, pearls
    }

    private double getGravity(Item item) {
        if (item instanceof BowItem || item instanceof CrossbowItem) return 0.05;
        if (item instanceof PotionItem) return 0.05;
        if (item instanceof ExperienceBottleItem) return 0.07;
        return 0.03; // Snowballs, pearls, eggs
    }

    private double getDrag(Item item) {
        return 0.99;
    }

    private List<Vec3> calculateTrajectory(Entity shooter, double velocity, double gravity, double drag) {
        List<Vec3> points = new ArrayList<>();
        
        float yaw = shooter.getYRot();
        float pitch = shooter.getXRot();

        double x = shooter.getX() - (Mth.cos(yaw / 180.0F * (float)Math.PI) * 0.16F);
        double y = shooter.getEyeY() - 0.1D;
        double z = shooter.getZ() - (Mth.sin(yaw / 180.0F * (float)Math.PI) * 0.16F);

        double motionX = -Mth.sin(yaw / 180.0F * (float)Math.PI) * Mth.cos(pitch / 180.0F * (float)Math.PI) * velocity;
        double motionY = -Mth.sin(pitch / 180.0F * (float)Math.PI) * velocity;
        double motionZ = Mth.cos(yaw / 180.0F * (float)Math.PI) * Mth.cos(pitch / 180.0F * (float)Math.PI) * velocity;

        points.add(new Vec3(x, y, z));

        for (int i = 0; i < maxTicks; i++) {
            Vec3 start = new Vec3(x, y, z);
            Vec3 end = new Vec3(x + motionX, y + motionY, z + motionZ);

            HitResult hit = mc.level.clip(new net.minecraft.world.level.ClipContext(start, end, 
                    net.minecraft.world.level.ClipContext.Block.OUTLINE, 
                    net.minecraft.world.level.ClipContext.Fluid.NONE, shooter));
            
            if (hit.getType() != HitResult.Type.MISS) {
                points.add(hit.getLocation());
                break;
            }

            x += motionX;
            y += motionY;
            z += motionZ;
            
            motionX *= drag;
            motionY *= drag;
            motionZ *= drag;
            motionY -= gravity;

            points.add(new Vec3(x, y, z));
        }

        return points;
    }

    private void renderPath(WorldRenderContext context, List<Vec3> points) {
        PoseStack matrices = context.matrices();
        Vec3 camPos = mc.gameRenderer.getMainCamera().position();
        VertexConsumer buffer = context.consumers().getBuffer(RenderTypes.lines());

        matrices.pushPose();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        float r = ((color.getColor() >> 16) & 0xFF) / 255.0f;
        float g = ((color.getColor() >> 8) & 0xFF) / 255.0f;
        float b = (color.getColor() & 0xFF) / 255.0f;
        float a = ((color.getColor() >> 24) & 0xFF) / 255.0f;
        if (a == 0) a = 1.0f;

        var matrix = matrices.last().pose();
        for (int i = 0; i < points.size() - 1; i++) {
            Vec3 p1 = points.get(i);
            Vec3 p2 = points.get(i + 1);

            buffer.addVertex(matrix, (float)p1.x, (float)p1.y, (float)p1.z).setColor(r, g, b, a).setNormal(0, 1, 0).setLineWidth(thickness);
            buffer.addVertex(matrix, (float)p2.x, (float)p2.y, (float)p2.z).setColor(r, g, b, a).setNormal(0, 1, 0).setLineWidth(thickness);
        }

        matrices.popPose();
    }

    @Override
    public void loadConfig(Properties props) {
        super.loadConfig(props);
        onlyWhenDrawing = Boolean.parseBoolean(props.getProperty("trajectories.only_drawing", "true"));
        try { maxTicks = Double.parseDouble(props.getProperty("trajectories.max_ticks", "100.0")); } catch (NumberFormatException ignored) {}
        try { thickness = Float.parseFloat(props.getProperty("trajectories.thickness", "2.0")); } catch (NumberFormatException ignored) {}
        color = ESPColor.fromString(props.getProperty("trajectories.color"), ESPColor.GREEN);
    }

    @Override
    public void saveConfig(Properties props) {
        super.saveConfig(props);
        props.setProperty("trajectories.only_drawing", Boolean.toString(onlyWhenDrawing));
        props.setProperty("trajectories.max_ticks", Double.toString(maxTicks));
        props.setProperty("trajectories.thickness", Float.toString(thickness));
        props.setProperty("trajectories.color", color.name());
    }

    public boolean isOnlyWhenDrawing() { return onlyWhenDrawing; }
    public void setOnlyWhenDrawing(boolean v) { onlyWhenDrawing = v; saveConfig(); }
    public double getMaxTicks() { return maxTicks; }
    public void setMaxTicks(double v) { maxTicks = v; saveConfig(); }
    public float getThickness() { return thickness; }
    public void setThickness(float v) { thickness = v; saveConfig(); }
    public ESPColor getColor() { return color; }
    public void cycleColor() { color = color.next(); saveConfig(); }
}
