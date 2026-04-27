/* Copyright (c) 2026 PhantomMod. All rights reserved. */
/*
 * Reach.java — Extends entity and block interaction range (Combat module).
 *
 * Uses Minecraft's Attributes system with transient AttributeModifiers to extend
 * ENTITY_INTERACTION_RANGE and BLOCK_INTERACTION_RANGE independently.
 *
 * ⚠ HYPIXEL / SERVER LIMIT: Hypixel and most major servers use server-side
 * anti-cheat (Watchdog + NCP). Entity reach above ~3.2 blocks will be silently
 * rejected or flagged — your hits simply won't register. Block reach has more
 * leniency (~5.0). For Hypixel, only use the "Legit" preset (3.1 entity / 4.7 block).
 * Anything above that works in singleplayer or unprotected servers only.
 */
package com.phantom.module.impl.combat;

import com.phantom.PhantomMod;
import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.util.Logger;

import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Properties;

public class Reach extends Module {

    public enum Preset {
        LEGIT("Legit"), NORMAL("Normal"), OBVIOUS("Obvious"), BLATANT("Blatant");
        private final String name;
        Preset(String name) { this.name = name; }
        public String getName() { return name; }
        public Preset next() { return values()[(this.ordinal() + 1) % values().length]; }
    }

    private double entityReach = 3.5;
    private double blockReach  = 5.0;
    private boolean onlyWhileSprinting = true;
    private boolean movingOnly = false;
    private boolean disableInWater = true;
    private Preset currentPreset = Preset.NORMAL;

    private static final double EPSILON = 1e-9;

   private static final Identifier MODIFIER_ENTITY =
        Identifier.fromNamespaceAndPath(PhantomMod.MOD_ID, "reach_entity");
   private static final Identifier MODIFIER_BLOCK =
        Identifier.fromNamespaceAndPath(PhantomMod.MOD_ID, "reach_block");
        
    public Reach() {
        super(
            "Reach",
            "Increases your attack and interaction range.\n" +
            "⚠ Hypixel: Entity reach above 3.2 will be rejected by Watchdog — hits won't register.\n" +
            "  Block reach above ~5.0 may also flag. Use Legit preset on Hypixel/major servers.\n" +
            "  Values above Legit only work on singleplayer or unprotected servers.",
            ModuleCategory.COMBAT,
            -1
        );
    }

    // -------------------------------------------------------------------------
    // Module lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void onEnable() {
        updateReach();
    }

    @Override
    public void onTick() {
        updateReach();
    }

    @Override
    public void onDisable() {
        clearModifiers();
    }

    // No onTick — modifiers are stable once applied.
    // Re-applying every tick caused the modifier to be briefly absent on
    // the exact tick an attack was sent, making other players appear invincible.

    // -------------------------------------------------------------------------
    // Modifier management
    // -------------------------------------------------------------------------

    private void clearModifiers() {
        if (mc.player == null) return;
        removeIfPresent(Attributes.ENTITY_INTERACTION_RANGE, MODIFIER_ENTITY);
        removeIfPresent(Attributes.BLOCK_INTERACTION_RANGE,  MODIFIER_BLOCK);
    }

    private void removeIfPresent(Holder<Attribute> attribute, Identifier id) {
        AttributeInstance attr = mc.player.getAttribute(attribute);
        if (attr != null) attr.removeModifier(id);
    }

    private void updateReach() {
        if (mc.player == null) return;

        if (!shouldApplyReach()) {
            clearModifiers();
            return;
        }

        double entityBonus = entityReach - 3.0;
        double blockBonus  = blockReach  - 4.5;

        applyModifier(Attributes.ENTITY_INTERACTION_RANGE, MODIFIER_ENTITY, entityBonus);
        applyModifier(Attributes.BLOCK_INTERACTION_RANGE,  MODIFIER_BLOCK,  blockBonus);
    }

    private void applyModifier(Holder<Attribute> attribute, Identifier id, double amount) {
        AttributeInstance instance = mc.player.getAttribute(attribute);
        if (instance == null) return;

        AttributeModifier existing = instance.getModifier(id);
        if (existing != null && Math.abs(existing.amount() - amount) <= EPSILON) return;

        instance.removeModifier(id);
        instance.addTransientModifier(
            new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE)
        );
    }

    private boolean shouldApplyReach() {
        if (onlyWhileSprinting && !mc.player.isSprinting()) {
            return false;
        }
        if (movingOnly && mc.player.getDeltaMovement().horizontalDistanceSqr() < 1.0E-4D) {
            return false;
        }
        return !disableInWater || (!mc.player.isInWater() && !mc.player.isInLava());
    }

    // -------------------------------------------------------------------------
    // Getters / setters
    // -------------------------------------------------------------------------

    public Preset getCurrentPreset() { return currentPreset; }
    public void cyclePreset() {
        currentPreset = currentPreset.next();
        switch (currentPreset) {
            case LEGIT -> applyPresetLegit();
            case NORMAL -> applyPresetNormal();
            case OBVIOUS -> applyPresetObvious();
            case BLATANT -> applyPresetBlatant();
        }
    }

    public double getEntityReach() { return entityReach; }

    public void setEntityReach(double v) {
        this.entityReach = Math.max(3.0, Math.min(8.0, v));
        if (isEnabled()) updateReach();
        saveConfig();
    }

    public double getBlockReach() { return blockReach; }

    public void setBlockReach(double v) {
        this.blockReach = Math.max(4.5, Math.min(10.0, v));
        if (isEnabled()) updateReach();
        saveConfig();
    }

    public boolean isOnlyWhileSprinting() {
        return onlyWhileSprinting;
    }

    public void setOnlyWhileSprinting(boolean onlyWhileSprinting) {
        this.onlyWhileSprinting = onlyWhileSprinting;
        if (isEnabled()) {
            updateReach();
        }
        saveConfig();
    }

    public boolean isMovingOnly() {
        return movingOnly;
    }

    public void setMovingOnly(boolean movingOnly) {
        this.movingOnly = movingOnly;
        if (isEnabled()) {
            updateReach();
        }
        saveConfig();
    }

    public boolean isDisableInWater() {
        return disableInWater;
    }

    public void setDisableInWater(boolean disableInWater) {
        this.disableInWater = disableInWater;
        if (isEnabled()) {
            updateReach();
        }
        saveConfig();
    }

    // -------------------------------------------------------------------------
    // Presets
    // -------------------------------------------------------------------------

    /** Hypixel-safe. Barely above vanilla — hard to detect. */
    public void applyPresetLegit() {
        setEntityReach(3.1);
        setBlockReach(4.7);
        setMovingOnly(true);
    }

    /** Exact vanilla values — effectively disables the bonus. */
    public void applyPresetNormal() {
        setEntityReach(3.0);
        setBlockReach(4.5);
        setMovingOnly(false);
    }

    /** Noticeable. Works on unprotected servers, flagged on Hypixel. */
    public void applyPresetObvious() {
        setEntityReach(4.5);
        setBlockReach(6.5);
        setMovingOnly(false);
    }

    /** Very long. Singleplayer / unprotected servers only. */
    public void applyPresetBlatant() {
        setEntityReach(6.5);
        setBlockReach(9.0);
        setMovingOnly(false);
    }

    // -------------------------------------------------------------------------
    // Settings screen
    // -------------------------------------------------------------------------

    @Override
    public boolean hasConfigurableSettings() { return true; }

    @Override
    public Screen createSettingsScreen(Screen parent) {
        return new ModuleSettingsScreen(parent, this);
    }

    // -------------------------------------------------------------------------
    // Config persistence
    // -------------------------------------------------------------------------

    @Override
    public void loadConfig(Properties properties) {
        super.loadConfig(properties);
        String e = properties.getProperty("reach.entity");
        String b = properties.getProperty("reach.block");
        if (e != null) {
            try { entityReach = Math.max(3.0, Math.min(8.0, Double.parseDouble(e.trim()))); }
            catch (NumberFormatException ex) { Logger.warn("Reach: Failed to parse reach"); }
        }
        if (b != null) {
            try { blockReach = Math.max(4.5, Math.min(10.0, Double.parseDouble(b.trim()))); }
            catch (NumberFormatException ex) { Logger.warn("Reach: Failed to parse block_reach"); }
        }
        onlyWhileSprinting = Boolean.parseBoolean(properties.getProperty("reach.only_while_sprinting",
                Boolean.toString(onlyWhileSprinting)));
        movingOnly = Boolean.parseBoolean(properties.getProperty("reach.moving_only",
                Boolean.toString(movingOnly)));
        disableInWater = Boolean.parseBoolean(properties.getProperty("reach.disable_in_water",
                Boolean.toString(disableInWater)));
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("reach.entity", Double.toString(entityReach));
        properties.setProperty("reach.block",  Double.toString(blockReach));
        properties.setProperty("reach.only_while_sprinting", Boolean.toString(onlyWhileSprinting));
        properties.setProperty("reach.moving_only", Boolean.toString(movingOnly));
        properties.setProperty("reach.disable_in_water", Boolean.toString(disableInWater));
    }
}
