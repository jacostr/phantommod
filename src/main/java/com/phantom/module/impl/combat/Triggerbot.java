/*
 * Triggerbot.java — Automatically attacks entities you hover over.
 *
 * Modern-combat only: respects attack cooldown timing with configurable delay,
 * optional early hits, and optional humanising miss chance.
 * Detectability: Moderate to Blatant — aggressive timing or early hits are easy to flag.
 */
package com.phantom.module.impl.combat;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

public class Triggerbot extends Module {

    public enum TargetMode {
        PLAYERS("Players"),
        MOBS("Mobs"),
        BOTH("Both");

        private final String label;
        TargetMode(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private int extraDelayTicks;
    private boolean requireMouseDown = true;
    private boolean airCrits = true;
    private boolean shieldCheck = true;
    private double targetMissChance = 0.0D;
    private double earlyHitChance = 0.0D;
    private boolean limitItems = true;
    private TargetMode targetMode = TargetMode.BOTH;

    private int readyTicks;
    private int lastAttackGameTick = -1;

    public Triggerbot() {
        super("Triggerbot",
                "Automatically attacks entities you hover over using modern attack cooldown timing.\nDetectability: Moderate to Blatant",
                ModuleCategory.COMBAT,
                -1);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null || mc.gameMode == null || mc.hitResult == null || mc.screen != null) {
            readyTicks = 0;
            return;
        }
        if (shouldPauseForBedMining()) {
            readyTicks = 0;
            return;
        }

        if (requireMouseDown && !mc.options.keyAttack.isDown()) {
            readyTicks = 0;
            return;
        }

        if (!(mc.hitResult instanceof EntityHitResult entityHitResult)) {
            readyTicks = 0;
            return;
        }

        Entity entity = entityHitResult.getEntity();
        if (!(entity instanceof LivingEntity living) || entity == mc.player || !living.isAlive()) {
            readyTicks = 0;
            return;
        }
        if (!isValidTarget(entity)) {
            readyTicks = 0;
            return;
        }
        if (isTeammateTarget(entity)) {
            readyTicks = 0;
            return;
        }

        if (!canUseCurrentItem()) {
            readyTicks = 0;
            return;
        }

        if (shieldCheck && living instanceof Player playerTarget && playerTarget.isBlocking()) {
            return;
        }

        if (airCrits && !canAttackForAirCrits()) {
            return;
        }

        float cooldown = mc.player.getAttackStrengthScale(0.0F);
        boolean ready = cooldown >= 1.0F;
        readyTicks = ready ? readyTicks + 1 : 0;

        boolean shouldAttack = false;
        if (extraDelayTicks >= 0) {
            shouldAttack = readyTicks > extraDelayTicks;
        } else {
            float earlyThreshold = Mth.clamp(1.0F + extraDelayTicks * 0.12F, 0.2F, 1.0F);
            if (cooldown >= earlyThreshold) {
                shouldAttack = true;
            }
        }

        if (!shouldAttack && earlyHitChance > 0.0D && cooldown >= 0.75F
                && ThreadLocalRandom.current().nextDouble() < earlyHitChance) {
            shouldAttack = true;
        }

        if (!shouldAttack) {
            return;
        }

        if (mc.player.tickCount == lastAttackGameTick) {
            return;
        }

        lastAttackGameTick = mc.player.tickCount;
        readyTicks = 0;

        if (targetMissChance > 0.0D && ThreadLocalRandom.current().nextDouble() < targetMissChance) {
            mc.player.swing(InteractionHand.MAIN_HAND);
            return;
        }

        mc.gameMode.attack(mc.player, entity);
        mc.player.swing(InteractionHand.MAIN_HAND);
    }

    private boolean canUseCurrentItem() {
        if (!limitItems) {
            return true;
        }
        String id = mc.player.getMainHandItem().getItem().getDescriptionId().toLowerCase(Locale.ROOT);
        return id.contains("sword") || id.contains("axe") || id.contains("trident") || id.contains("mace");
    }

    private boolean canAttackForAirCrits() {
        if (mc.player.onGround()) {
            return true;
        }
        return mc.player.fallDistance > 0.0F
                && !mc.player.isInWater()
                && !mc.player.isInLava()
                && !mc.player.onClimbable();
    }

    public int getExtraDelayTicks() {
        return extraDelayTicks;
    }

    public void setExtraDelayTicks(int extraDelayTicks) {
        this.extraDelayTicks = Math.max(-4, Math.min(10, extraDelayTicks));
        saveConfig();
    }

    public boolean isRequireMouseDown() {
        return requireMouseDown;
    }

    public void setRequireMouseDown(boolean requireMouseDown) {
        this.requireMouseDown = requireMouseDown;
        saveConfig();
    }

    public boolean isAirCrits() {
        return airCrits;
    }

    public void setAirCrits(boolean airCrits) {
        this.airCrits = airCrits;
        saveConfig();
    }

    public boolean isShieldCheck() {
        return shieldCheck;
    }

    public void setShieldCheck(boolean shieldCheck) {
        this.shieldCheck = shieldCheck;
        saveConfig();
    }

    public double getTargetMissChance() {
        return targetMissChance;
    }

    public void setTargetMissChance(double targetMissChance) {
        this.targetMissChance = Math.max(0.0D, Math.min(1.0D, targetMissChance));
        saveConfig();
    }

    public double getEarlyHitChance() {
        return earlyHitChance;
    }

    public void setEarlyHitChance(double earlyHitChance) {
        this.earlyHitChance = Math.max(0.0D, Math.min(1.0D, earlyHitChance));
        saveConfig();
    }

    public boolean isLimitItems() {
        return limitItems;
    }

    public void setLimitItems(boolean limitItems) {
        this.limitItems = limitItems;
        saveConfig();
    }

    public TargetMode getTargetMode() {
        return targetMode;
    }

    public void setTargetMode(TargetMode mode) {
        this.targetMode = mode;
        saveConfig();
    }

    public void cycleTargetMode() {
        TargetMode[] values = TargetMode.values();
        targetMode = values[(targetMode.ordinal() + 1) % values.length];
        saveConfig();
    }

    private boolean isValidTarget(Entity entity) {
        return switch (targetMode) {
            case PLAYERS -> entity instanceof Player;
            case MOBS -> entity instanceof Mob;
            case BOTH -> entity instanceof Player || entity instanceof Mob;
        };
    }

    public void applyPresetLegit() {
        setExtraDelayTicks(1);
        setRequireMouseDown(true);
        setAirCrits(false);
        setShieldCheck(true);
        setTargetMissChance(0.10);
        setEarlyHitChance(0.0);
        setLimitItems(true);
    }

    public void applyPresetNormal() {
        setExtraDelayTicks(0);
        setRequireMouseDown(true);
        setAirCrits(true);
        setShieldCheck(true);
        setTargetMissChance(0.05);
        setEarlyHitChance(0.05);
        setLimitItems(true);
    }

    public void applyPresetObvious() {
        setExtraDelayTicks(-1);
        setRequireMouseDown(true);
        setAirCrits(true);
        setShieldCheck(false);
        setTargetMissChance(0.02);
        setEarlyHitChance(0.20);
        setLimitItems(true);
    }

    public void applyPresetBlatant() {
        setExtraDelayTicks(-3);
        setRequireMouseDown(false);
        setAirCrits(true);
        setShieldCheck(false);
        setTargetMissChance(0.0);
        setEarlyHitChance(0.45);
        setLimitItems(false);
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
        String extraDelay = properties.getProperty("triggerbot.extra_delay_ticks");
        if (extraDelay != null) {
            try {
                extraDelayTicks = Math.max(-4, Math.min(10, Integer.parseInt(extraDelay.trim())));
            } catch (NumberFormatException ignored) {
            }
        }
        requireMouseDown = Boolean.parseBoolean(properties.getProperty("triggerbot.require_mouse_down", Boolean.toString(requireMouseDown)));
        airCrits = Boolean.parseBoolean(properties.getProperty("triggerbot.air_crits", Boolean.toString(airCrits)));
        shieldCheck = Boolean.parseBoolean(properties.getProperty("triggerbot.shield_check", Boolean.toString(shieldCheck)));

        String missChance = properties.getProperty("triggerbot.target_miss_chance");
        if (missChance != null) {
            try {
                targetMissChance = Math.max(0.0D, Math.min(1.0D, Double.parseDouble(missChance.trim())));
            } catch (NumberFormatException ignored) {
            }
        }

        String earlyChance = properties.getProperty("triggerbot.early_hit_chance");
        if (earlyChance != null) {
            try {
                earlyHitChance = Math.max(0.0D, Math.min(1.0D, Double.parseDouble(earlyChance.trim())));
            } catch (NumberFormatException ignored) {
            }
        }

        limitItems = Boolean.parseBoolean(properties.getProperty("triggerbot.limit_items", Boolean.toString(limitItems)));
        String mode = properties.getProperty("triggerbot.target_mode");
        if (mode != null) {
            try { targetMode = TargetMode.valueOf(mode.trim().toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException ignored) {}
        }
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("triggerbot.extra_delay_ticks", Integer.toString(extraDelayTicks));
        properties.setProperty("triggerbot.require_mouse_down", Boolean.toString(requireMouseDown));
        properties.setProperty("triggerbot.air_crits", Boolean.toString(airCrits));
        properties.setProperty("triggerbot.shield_check", Boolean.toString(shieldCheck));
        properties.setProperty("triggerbot.target_miss_chance", Double.toString(targetMissChance));
        properties.setProperty("triggerbot.early_hit_chance", Double.toString(earlyHitChance));
        properties.setProperty("triggerbot.limit_items", Boolean.toString(limitItems));
        properties.setProperty("triggerbot.target_mode", targetMode.name());
    }
}
