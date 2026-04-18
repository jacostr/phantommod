/* Copyright (c) 2026 PhantomMod. All rights reserved. */
package com.phantom.module.impl.render;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import com.phantom.module.impl.player.AntiBot;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;

import java.util.Comparator;
import java.util.Properties;

public class Arrows extends Module {
    private double radius = 58.0D;
    private boolean showDistance = true;
    private boolean playersOnly = true;
    private boolean onlyOffScreen = false;
    private int arrowStyle = 0; // 0: Caret, 1: Triangle, 2: GT

    private final String[] styles = {"Caret", "Triangle", "Greater Than"};

    public Arrows() {
        super("Arrows",
                "Draws directional arrows toward nearby players around your crosshair.\nDetectability: Safe",
                ModuleCategory.RENDER, -1);
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
    public void onHudRender(GuiGraphics graphics) {
        if (mc.player == null || mc.level == null || mc.options.hideGui) {
            return;
        }

        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2;

        mc.level.players().stream()
                .filter(this::shouldRender)
                .sorted(Comparator.comparingDouble(mc.player::distanceTo))
                .limit(12)
                .forEach(player -> drawArrow(graphics, player, centerX, centerY));
    }

    private boolean shouldRender(Player player) {
        if (player == mc.player || !player.isAlive()) {
            return false;
        }
        if (AntiBot.isBot(player)) {
            return false;
        }
        if (onlyOffScreen && isInView(player)) {
            return false;
        }
        return true;
    }

    private boolean isInView(Player target) {
        Vec3 toTarget = target.position().subtract(mc.player.position()).normalize();
        Vec3 look = mc.player.getLookAngle().normalize();
        return toTarget.dot(look) > 0.0; // Simple front-half check
    }

    private void drawArrow(GuiGraphics graphics, Player target, int centerX, int centerY) {
        double dx = target.getX() - mc.player.getX();
        double dz = target.getZ() - mc.player.getZ();
        float worldYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
        float angle = worldYaw - mc.player.getYRot();
        double radians = Math.toRadians(angle);

        int renderX = centerX + (int) Math.round(Math.sin(radians) * radius);
        int renderY = centerY - (int) Math.round(Math.cos(radians) * radius);
        int color = getArrowColor(target);

        graphics.pose().pushMatrix();
        graphics.pose().translate(renderX, renderY);
        graphics.pose().rotate((float) Math.toRadians(angle));
        
        String icon = switch (arrowStyle) {
            case 1 -> "▼";
            case 2 -> ">";
            default -> "^";
        };
        
        graphics.drawString(mc.font, Component.literal(icon), -2, -4, color, true);
        graphics.pose().popMatrix();

        if (showDistance) {
            String distance = Integer.toString(Math.round(mc.player.distanceTo(target)));
            graphics.drawCenteredString(mc.font, Component.literal(distance), renderX, renderY + 6, color);
        }
    }

    private int getArrowColor(Player player) {
        Team team = player.getTeam();
        if (team != null) {
            ChatFormatting formatting = team.getColor();
            if (formatting != null && formatting.getColor() != null) {
                return 0xFF000000 | formatting.getColor();
            }
        }

        int[] armorSlots = {36, 37, 38, 39};
        for (int slot : armorSlots) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            DyedItemColor dyed = stack.get(DataComponents.DYED_COLOR);
            if (dyed != null) {
                return 0xFF000000 | dyed.rgb();
            }
        }

        return 0xFFFFFFFF;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = Math.max(30.0D, Math.min(140.0D, radius));
        saveConfig();
    }

    public boolean isShowDistance() {
        return showDistance;
    }

    public void setShowDistance(boolean showDistance) {
        this.showDistance = showDistance;
        saveConfig();
    }

    public boolean isPlayersOnly() {
        return playersOnly;
    }

    public void setPlayersOnly(boolean playersOnly) {
        this.playersOnly = playersOnly;
        saveConfig();
    }

    public boolean isOnlyOffScreen() { return onlyOffScreen; }
    public void setOnlyOffScreen(boolean v) { this.onlyOffScreen = v; saveConfig(); }

    public int getArrowStyle() { return arrowStyle; }
    public void cycleArrowStyle() { arrowStyle = (arrowStyle + 1) % styles.length; saveConfig(); }
    public String getStyleLabel() { return styles[arrowStyle]; }

    @Override
    public void loadConfig(Properties properties) {
        super.loadConfig(properties);
        String radiusValue = properties.getProperty("arrows.radius");
        if (radiusValue != null) {
            try {
                radius = Math.max(30.0D, Math.min(140.0D, Double.parseDouble(radiusValue.trim())));
            } catch (NumberFormatException ignored) {
            }
        }
        showDistance = Boolean.parseBoolean(properties.getProperty("arrows.show_distance", Boolean.toString(showDistance)));
        playersOnly = Boolean.parseBoolean(properties.getProperty("arrows.players_only", Boolean.toString(playersOnly)));
        onlyOffScreen = Boolean.parseBoolean(properties.getProperty("arrows.only_offscreen", Boolean.toString(onlyOffScreen)));
        arrowStyle = Integer.parseInt(properties.getProperty("arrows.style", "0"));
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("arrows.radius", Double.toString(radius));
        properties.setProperty("arrows.show_distance", Boolean.toString(showDistance));
        properties.setProperty("arrows.players_only", Boolean.toString(playersOnly));
        properties.setProperty("arrows.only_offscreen", Boolean.toString(onlyOffScreen));
        properties.setProperty("arrows.style", Integer.toString(arrowStyle));
    }
}
