/*
 * AutoBlock.java — Automatic sword blocking after hitting an entity (Combat module).
 *
 * After each sword hit, virtually holds the right-click (use) key for a configurable
 * duration. On Hypixel 1.21, holding right-click with a sword triggers 1.8-style
 * block-hitting. Strength slider controls blocking speed and duration.
 * Detectability: Blatant — consistent block-hit timing is easily flagged.
 */
package com.phantom.module.impl.combat;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Properties;

public class AutoBlock extends Module {
    /** 0 = slow/weak reaction, 100 = fast, long block. */
    private int strength = 50;
    /** 0 = never block, 100 = always block (if hurt/attacking). */
    private int chance = 30;

    private int lastHurtTime;
    private int reactionWaitTicks;
    private int holdTicksRemaining;
    private boolean weAreHoldingUse;
    private int lastAttackTime;

    public AutoBlock() {
        super("AutoBlock", "Automatically blocks after you hit an entity to significantly reduce incoming damage.\nDetectability: Blatant", ModuleCategory.COMBAT, -1);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.options == null || mc.screen != null) {
            releaseOurUse();
            return;
        }
        if (shouldPauseForBedMining()) {
            releaseOurUse();
            return;
        }

        if (!canRaiseShield()) {
            releaseOurUse();
            return;
        }

        // Logic for "Sometimes Automatically" (Randomization)
        int hurt = mc.player.hurtTime;
        if (hurt > lastHurtTime) {
            if (mc.player.getRandom().nextInt(100) < chance) {
                reactionWaitTicks = computeReactionDelay();
            }
        }
        lastHurtTime = hurt;

        // Also block sometimes after attacking (Legit Block-Hitting)
        if (mc.player.attackAnim > 0 && mc.player.attackAnim < 0.1) { // Just started attacking
            if (mc.player.getRandom().nextInt(100) < chance) {
                reactionWaitTicks = computeReactionDelay();
            }
        }

        if (holdTicksRemaining > 0) {
            holdTicksRemaining--;
            mc.options.keyUse.setDown(true);
            weAreHoldingUse = true;
            if (holdTicksRemaining <= 0) {
                releaseOurUse();
            }
            return;
        }

        if (reactionWaitTicks > 0) {
            reactionWaitTicks--;
            if (reactionWaitTicks == 0) {
                holdTicksRemaining = computeHoldTicks();
                mc.options.keyUse.setDown(true);
                weAreHoldingUse = true;
            }
        }
    }

    @Override
    public void onDisable() {
        releaseOurUse();
        lastHurtTime = 0;
        reactionWaitTicks = 0;
        holdTicksRemaining = 0;
    }

    public boolean isHoldingUse() {
        return weAreHoldingUse;
    }

    private void releaseOurUse() {
        if (weAreHoldingUse && mc.options != null) {
            mc.options.keyUse.setDown(false);
        }
        weAreHoldingUse = false;
    }

    private boolean canRaiseShield() {
        ItemStack main = mc.player.getMainHandItem();
        if (main.getItem().getDescriptionId().toLowerCase().contains("sword")) {
            return true;
        }
        ItemStack off = mc.player.getOffhandItem();
        return off.is(Items.SHIELD) && !mc.player.getCooldowns().isOnCooldown(off);
    }

    /** Higher strength → fewer ticks before we press use. */
    private int computeReactionDelay() {
        return Math.max(0, (100 - strength) / 12);
    }

    /** Higher strength → hold block longer. */
    private int computeHoldTicks() {
        return 4 + strength / 5;
    }

    @Override
    public boolean hasConfigurableSettings() {
        return true;
    }

    @Override
    public Screen createSettingsScreen(Screen parent) {
        return new ModuleSettingsScreen(parent, this);
    }

    public int getStrength() {
        return strength;
    }

    public void setStrength(int strength) {
        this.strength = Math.max(0, Math.min(100, strength));
        saveConfig();
    }

    public int getChance() {
        return chance;
    }

    public void setChance(int chance) {
        this.chance = Math.max(0, Math.min(100, chance));
        saveConfig();
    }

    public void applyPresetLegit() {
        setStrength(35);
        setChance(30);
    }

    public void applyPresetNormal() {
        setStrength(55);
    }

    public void applyPresetObvious() {
        setStrength(92);
    }

    public void applyPresetBlatant() {
        setStrength(100);
    }

    @Override
    public void loadConfig(Properties properties) {
        super.loadConfig(properties);
        String v = properties.getProperty("autoblock.strength");
        if (v != null) {
            try {
                strength = Math.max(0, Math.min(100, Integer.parseInt(v.trim())));
            } catch (NumberFormatException ignored) {
            }
        }
        String c = properties.getProperty("autoblock.chance");
        if (c != null) {
            try {
                chance = Math.max(0, Math.min(100, Integer.parseInt(c.trim())));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("autoblock.strength", Integer.toString(strength));
        properties.setProperty("autoblock.chance", Integer.toString(chance));
    }
}
