/*
 * Blatant: when you take damage and hold a shield offhand, briefly holds use after a delay.
 * Strength 0–100: higher = shorter reaction wait and longer block hold (100 ≈ very strong).
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

    private int lastHurtTime;
    private int reactionWaitTicks;
    private int holdTicksRemaining;
    private boolean weAreHoldingUse;

    public AutoBlock() {
        super("Auto Block",
                "Raises shield (offhand) after you get hit. Strength slider: higher = quicker block and longer hold.",
                ModuleCategory.BLATANT,
                -1);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.options == null || mc.screen != null) {
            releaseOurUse();
            return;
        }

        if (!canRaiseShield()) {
            releaseOurUse();
            return;
        }

        int hurt = mc.player.hurtTime;
        if (hurt > lastHurtTime) {
            reactionWaitTicks = computeReactionDelay();
        }
        lastHurtTime = hurt;

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

    private void releaseOurUse() {
        if (weAreHoldingUse && mc.options != null) {
            mc.options.keyUse.setDown(false);
        }
        weAreHoldingUse = false;
    }

    private boolean canRaiseShield() {
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

    public void applyPresetLegit() {
        setStrength(22);
    }

    public void applyPresetNormal() {
        setStrength(55);
    }

    public void applyPresetObvious() {
        setStrength(92);
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
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("autoblock.strength", Integer.toString(strength));
    }
}
