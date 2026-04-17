/* Copyright (c) 2026 PhantomMod. All rights reserved. */
/*
 * Indicators.java — On-screen projectile warnings.
 *
 * Shows compact HUD markers around the screen center for incoming projectiles.
 * Detectability: Safe — purely client-side visual overlay.
 */
package com.phantom.module.impl.render;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public class Indicators extends Module {
    private static final double SCAN_RANGE = 96.0D;

    public enum AlertType {
        ALWAYS("Always"),
        THREAT("Threat"),
        HIT_ONLY("Hit Only");

        private final String label;

        AlertType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public AlertType next() {
            return switch (this) {
                case ALWAYS -> THREAT;
                case THREAT -> HIT_ONLY;
                case HIT_ONLY -> ALWAYS;
            };
        }

        public static AlertType fromString(String value) {
            if (value == null) {
                return THREAT;
            }
            try {
                return AlertType.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return THREAT;
            }
        }
    }

    private AlertType alertType = AlertType.THREAT;
    private boolean uncommonProjectileColor = true;
    private boolean showArrows = true;
    private boolean showPearls = true;
    private boolean showPotions = true;
    private boolean showEggs = true;
    private boolean showSnowballs = true;
    private boolean showFireballs = true;
    private double radiusScale = 1.0D;
    private boolean showDistance = true;
    private boolean onlyWhenApproaching = false;
    private boolean renderItem = true;

    private final java.util.Map<Integer, Vec3> lastPositions = new java.util.HashMap<>();

    public Indicators() {
        super("Indicators",
                "Displays on-screen warnings for nearby projectiles, with optional threat filtering.\nDetectability: Safe",
                ModuleCategory.RENDER,
                -1);
    }

    @Override
    public void onHudRender(GuiGraphics graphics) {
        if (mc.player == null || mc.level == null || mc.options.hideGui) {
            return;
        }

        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2;
        int radius = (int) Math.round(58.0D * radiusScale);

        List<IndicatorEntry> entries = new ArrayList<>();
        for (Entity entity : mc.level.getEntities(mc.player, mc.player.getBoundingBox().inflate(SCAN_RANGE))) {
            if (!(entity instanceof Projectile projectile)) {
                continue;
            }
            if (projectile.getOwner() == mc.player) {
                continue;
            }

            ProjectileKind kind = ProjectileKind.fromEntity(projectile);
            if (kind == null || !isEnabledForKind(kind)) {
                continue;
            }

            ThreatLevel threatLevel = classifyThreat(projectile);
            if (!matchesAlertType(threatLevel)) {
                continue;
            }

            if (onlyWhenApproaching && !isApproaching(projectile)) {
                continue;
            }

            entries.add(new IndicatorEntry(projectile, kind, threatLevel));
        }
        
        // Clean up stale positions
        lastPositions.keySet().removeIf(id -> mc.level.getEntity(id) == null);

        entries.sort(Comparator.comparingDouble(entry -> mc.player.distanceTo(entry.projectile)));

        for (IndicatorEntry entry : entries) {
            drawIndicator(graphics, entry, centerX, centerY, radius);
        }
    }

    private void drawIndicator(GuiGraphics graphics, IndicatorEntry entry, int centerX, int centerY, int radius) {
        double dx = entry.projectile.getX() - mc.player.getX();
        double dz = entry.projectile.getZ() - mc.player.getZ();
        float worldYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
        float angle = Mth.wrapDegrees(worldYaw - mc.player.getYRot());
        double radians = Math.toRadians(angle);

        int x = centerX + (int) Math.round(Math.sin(radians) * radius);
        int y = centerY - (int) Math.round(Math.cos(radians) * radius);

        int color = getColor(entry.kind);
        String marker = entry.kind.shortLabel();
        String label = showDistance
                ? marker + " " + String.format(Locale.ROOT, "%.1f", mc.player.distanceTo(entry.projectile))
                : marker;
        int width = mc.font.width(label) + 8;

        graphics.fill(x - width / 2, y - 8, x + width / 2, y + 8, 0x90000000);
        graphics.renderOutline(x - width / 2, y - 8, width, 16, color);
        graphics.drawCenteredString(mc.font, Component.literal(label), x, y - 4, color);

        if (renderItem) {
            ItemStack stack = entry.kind.getIconStack();
            if (stack != null) {
                graphics.pose().pushMatrix();
                graphics.pose().translate(x - width / 2 - 18, y - 8);
                graphics.renderFakeItem(stack, 0, 0);
                graphics.pose().popMatrix();
            }
        }
    }

    private boolean isApproaching(Projectile projectile) {
        Vec3 currentPos = projectile.position();
        Vec3 lastPos = lastPositions.get(projectile.getId());
        lastPositions.put(projectile.getId(), currentPos);

        if (lastPos == null) return true;

        double distNow = mc.player.distanceTo(projectile);
        double distThen = (float)Math.sqrt(lastPos.distanceToSqr(mc.player.position()));
        return distNow < distThen;
    }

    private boolean matchesAlertType(ThreatLevel threatLevel) {
        return switch (alertType) {
            case ALWAYS -> true;
            case THREAT -> threatLevel == ThreatLevel.THREAT || threatLevel == ThreatLevel.HIT_ONLY;
            case HIT_ONLY -> threatLevel == ThreatLevel.HIT_ONLY;
        };
    }

    private ThreatLevel classifyThreat(Projectile projectile) {
        Vec3 playerPos = mc.player.getEyePosition();
        Vec3 projectilePos = projectile.position();
        Vec3 toPlayer = playerPos.subtract(projectilePos);
        Vec3 relativeVelocity = projectile.getDeltaMovement().subtract(mc.player.getDeltaMovement());
        double speedSq = relativeVelocity.lengthSqr();
        if (speedSq < 1.0E-5D) {
            return ThreatLevel.NONE;
        }

        double timeToClosest = Mth.clamp(toPlayer.dot(relativeVelocity) / speedSq, 0.0D, 12.0D);
        Vec3 closestOffset = toPlayer.subtract(relativeVelocity.scale(timeToClosest));
        double closestDistance = closestOffset.length();
        double closingStrength = relativeVelocity.normalize().dot(toPlayer.normalize());

        if (timeToClosest <= 3.0D && closestDistance <= 0.9D && closingStrength >= 0.98D) {
            return ThreatLevel.HIT_ONLY;
        }
        if (timeToClosest <= 8.0D && closestDistance <= 2.5D && closingStrength >= 0.90D) {
            return ThreatLevel.THREAT;
        }
        return ThreatLevel.NONE;
    }

    private boolean isEnabledForKind(ProjectileKind kind) {
        return switch (kind) {
            case ARROW -> showArrows;
            case PEARL -> showPearls;
            case POTION -> showPotions;
            case EGG -> showEggs;
            case SNOWBALL -> showSnowballs;
            case FIREBALL -> showFireballs;
        };
    }

    private int getColor(ProjectileKind kind) {
        return switch (kind) {
            case FIREBALL -> uncommonProjectileColor ? 0xFFFF9050 : 0xFFFFFFFF;
            case PEARL -> 0xFF9A7DFF;
            case POTION -> 0xFFFF66CC;
            case EGG -> 0xFFFFFFAA;
            case SNOWBALL -> 0xFF8FE8FF;
            case ARROW -> 0xFFFFFFFF;
        };
    }

    public AlertType getAlertType() {
        return alertType;
    }

    public void cycleAlertType() {
        alertType = alertType.next();
        saveConfig();
    }

    public boolean isUncommonProjectileColor() {
        return uncommonProjectileColor;
    }

    public void setUncommonProjectileColor(boolean uncommonProjectileColor) {
        this.uncommonProjectileColor = uncommonProjectileColor;
        saveConfig();
    }

    public boolean isShowArrows() {
        return showArrows;
    }

    public void setShowArrows(boolean showArrows) {
        this.showArrows = showArrows;
        saveConfig();
    }

    public boolean isShowPearls() {
        return showPearls;
    }

    public void setShowPearls(boolean showPearls) {
        this.showPearls = showPearls;
        saveConfig();
    }

    public boolean isShowPotions() {
        return showPotions;
    }

    public void setShowPotions(boolean showPotions) {
        this.showPotions = showPotions;
        saveConfig();
    }

    public boolean isShowEggs() {
        return showEggs;
    }

    public void setShowEggs(boolean showEggs) {
        this.showEggs = showEggs;
        saveConfig();
    }

    public boolean isShowSnowballs() {
        return showSnowballs;
    }

    public void setShowSnowballs(boolean showSnowballs) {
        this.showSnowballs = showSnowballs;
        saveConfig();
    }

    public boolean isShowFireballs() {
        return showFireballs;
    }

    public void setShowFireballs(boolean showFireballs) {
        this.showFireballs = showFireballs;
        saveConfig();
    }

    public double getRadiusScale() {
        return radiusScale;
    }

    public void setRadiusScale(double radiusScale) {
        this.radiusScale = Math.max(0.5D, Math.min(2.0D, radiusScale));
        saveConfig();
    }

    public boolean isShowDistance() {
        return showDistance;
    }

    public void setShowDistance(boolean showDistance) {
        this.showDistance = showDistance;
        saveConfig();
    }

    public boolean isOnlyWhenApproaching() { return onlyWhenApproaching; }
    public void setOnlyWhenApproaching(boolean v) { this.onlyWhenApproaching = v; saveConfig(); }

    public boolean isRenderItem() { return renderItem; }
    public void setRenderItem(boolean v) { this.renderItem = v; saveConfig(); }

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
        alertType = AlertType.fromString(properties.getProperty("indicators.alert_type"));
        uncommonProjectileColor = Boolean.parseBoolean(properties.getProperty(
                "indicators.uncommon_projectile_color",
                Boolean.toString(uncommonProjectileColor)));
        showArrows = Boolean.parseBoolean(properties.getProperty("indicators.show_arrows", Boolean.toString(showArrows)));
        showPearls = Boolean.parseBoolean(properties.getProperty("indicators.show_pearls", Boolean.toString(showPearls)));
        showPotions = Boolean.parseBoolean(properties.getProperty("indicators.show_potions", Boolean.toString(showPotions)));
        showEggs = Boolean.parseBoolean(properties.getProperty("indicators.show_eggs", Boolean.toString(showEggs)));
        showSnowballs = Boolean.parseBoolean(properties.getProperty("indicators.show_snowballs", Boolean.toString(showSnowballs)));
        showFireballs = Boolean.parseBoolean(properties.getProperty("indicators.show_fireballs", Boolean.toString(showFireballs)));
        String radius = properties.getProperty("indicators.radius_scale");
        if (radius != null) {
            try {
                radiusScale = Math.max(0.5D, Math.min(2.0D, Double.parseDouble(radius.trim())));
            } catch (NumberFormatException ignored) {
            }
        }
        showDistance = Boolean.parseBoolean(properties.getProperty("indicators.show_distance", Boolean.toString(showDistance)));
        onlyWhenApproaching = Boolean.parseBoolean(properties.getProperty("indicators.only_when_approaching", Boolean.toString(onlyWhenApproaching)));
        renderItem = Boolean.parseBoolean(properties.getProperty("indicators.render_item", Boolean.toString(renderItem)));
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("indicators.alert_type", alertType.name());
        properties.setProperty("indicators.uncommon_projectile_color", Boolean.toString(uncommonProjectileColor));
        properties.setProperty("indicators.show_arrows", Boolean.toString(showArrows));
        properties.setProperty("indicators.show_pearls", Boolean.toString(showPearls));
        properties.setProperty("indicators.show_potions", Boolean.toString(showPotions));
        properties.setProperty("indicators.show_eggs", Boolean.toString(showEggs));
        properties.setProperty("indicators.show_snowballs", Boolean.toString(showSnowballs));
        properties.setProperty("indicators.show_fireballs", Boolean.toString(showFireballs));
        properties.setProperty("indicators.radius_scale", Double.toString(radiusScale));
        properties.setProperty("indicators.show_distance", Boolean.toString(showDistance));
        properties.setProperty("indicators.only_when_approaching", Boolean.toString(onlyWhenApproaching));
        properties.setProperty("indicators.render_item", Boolean.toString(renderItem));
    }

    private enum ThreatLevel {
        NONE,
        THREAT,
        HIT_ONLY
    }

    private record IndicatorEntry(Projectile projectile, ProjectileKind kind, ThreatLevel threatLevel) {
    }

    private enum ProjectileKind {
        ARROW("Ar"),
        PEARL("Pe"),
        POTION("Po"),
        EGG("Eg"),
        SNOWBALL("Sn"),
        FIREBALL("Fb");

        private final String shortLabel;

        ProjectileKind(String shortLabel) {
            this.shortLabel = shortLabel;
        }

        public String shortLabel() {
            return shortLabel;
        }

        public static ProjectileKind fromEntity(Projectile projectile) {
            if (projectile instanceof AbstractArrow) {
                return ARROW;
            }
            if (projectile instanceof ThrownEnderpearl) {
                return PEARL;
            }
            if (projectile instanceof AbstractThrownPotion) {
                return POTION;
            }
            if (projectile instanceof ThrownEgg) {
                return EGG;
            }
            if (projectile instanceof Snowball) {
                return SNOWBALL;
            }
            if (projectile instanceof Fireball) {
                return FIREBALL;
            }
            return null;
        }

        public ItemStack getIconStack() {
            return switch (this) {
                case ARROW -> new ItemStack(net.minecraft.world.item.Items.ARROW);
                case PEARL -> new ItemStack(net.minecraft.world.item.Items.ENDER_PEARL);
                case POTION -> new ItemStack(net.minecraft.world.item.Items.SPLASH_POTION);
                case EGG -> new ItemStack(net.minecraft.world.item.Items.EGG);
                case SNOWBALL -> new ItemStack(net.minecraft.world.item.Items.SNOWBALL);
                case FIREBALL -> new ItemStack(net.minecraft.world.item.Items.FIRE_CHARGE);
            };
        }
    }
}
