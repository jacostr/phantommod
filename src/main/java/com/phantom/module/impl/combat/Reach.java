/*
 * Reach.java — Extends entity and block interaction range (Combat module).
 *
 * Uses Minecraft's Attributes system with transient AttributeModifiers to extend
 * ENTITY_INTERACTION_RANGE and BLOCK_INTERACTION_RANGE independently. Modifiers are
 * added on enable and removed on disable. Configurable via separate sliders +
 * Legit/Normal/Obvious/Blatant presets.
 * Detectability: Blatant — servers log hit distances directly.
 */
package com.phantom.module.impl.combat;

import com.phantom.PhantomMod;
import com.phantom.gui.ModuleSettingsScreen;
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
    private double entityReach = 3.5;
    private double blockReach = 5.0;

    private static final Identifier MODIFIER_ENTITY = Identifier.fromNamespaceAndPath(PhantomMod.MOD_ID, "reach_entity");
    private static final Identifier MODIFIER_BLOCK = Identifier.fromNamespaceAndPath(PhantomMod.MOD_ID, "reach_block");

    public Reach() {
        super("Reach", "Increases your attack and interaction range.\nDetectability: Blatant", ModuleCategory.COMBAT, -1);
    }

    @Override
    public void onEnable() {
        updateReach();
    }

    @Override
    public void onDisable() {
        clearModifiers();
    }

    @Override
    public void onTick() {
        updateReach();
    }

    private void clearModifiers() {
        if (mc.player == null) {
            return;
        }
        removeIfPresent(Attributes.ENTITY_INTERACTION_RANGE, MODIFIER_ENTITY);
        removeIfPresent(Attributes.BLOCK_INTERACTION_RANGE, MODIFIER_BLOCK);
    }

    private void removeIfPresent(Holder<Attribute> attribute, Identifier id) {
        AttributeInstance attr = mc.player.getAttribute(attribute);
        if (attr != null) {
            attr.removeModifier(id);
        }
    }

    private void updateReach() {
        if (mc.player == null) {
            return;
        }
        double entityBonus = entityReach - 3.0;
        double blockBonus = blockReach - 4.5;
        applyModifier(Attributes.ENTITY_INTERACTION_RANGE, MODIFIER_ENTITY, entityBonus);
        applyModifier(Attributes.BLOCK_INTERACTION_RANGE, MODIFIER_BLOCK, blockBonus);
    }

    private void applyModifier(Holder<Attribute> attribute, Identifier modifierId, double amount) {
        AttributeInstance instance = mc.player.getAttribute(attribute);
        if (instance != null) {
            AttributeModifier existing = instance.getModifier(modifierId);
            if (existing == null || existing.amount() != amount) {
                instance.removeModifier(modifierId);
                instance.addTransientModifier(new AttributeModifier(modifierId, amount, AttributeModifier.Operation.ADD_VALUE));
            }
        }
    }

    @Override
    public boolean hasConfigurableSettings() {
        return true;
    }

    @Override
    public Screen createSettingsScreen(Screen parent) {
        return new ModuleSettingsScreen(parent, this);
    }

    public double getEntityReach() {
        return entityReach;
    }

    public void setEntityReach(double v) {
        this.entityReach = Math.max(3.0, Math.min(8.0, v));
        if (isEnabled()) {
            updateReach();
        }
        saveConfig();
    }

    public double getBlockReach() {
        return blockReach;
    }

    public void setBlockReach(double v) {
        this.blockReach = Math.max(4.5, Math.min(10.0, v));
        if (isEnabled()) {
            updateReach();
        }
        saveConfig();
    }

    /** ~vanilla+ — subtle. */
    public void applyPresetLegit() {
        setEntityReach(3.1);
        setBlockReach(4.7);
    }

    /** Vanilla standards. */
    public void applyPresetNormal() {
        setEntityReach(3.0);
        setBlockReach(4.5);
    }

    /** Noticeable but common cheat values. */
    public void applyPresetObvious() {
        setEntityReach(4.5);
        setBlockReach(6.5);
    }

    /** Very long. */
    public void applyPresetBlatant() {
        setEntityReach(6.5);
        setBlockReach(9.0);
    }

    @Override
    public void loadConfig(Properties properties) {
        super.loadConfig(properties);
        String e = properties.getProperty("reach.entity");
        String b = properties.getProperty("reach.block");
        if (e != null) {
            try {
                entityReach = Math.max(3.0, Math.min(8.0, Double.parseDouble(e.trim())));
            } catch (NumberFormatException ignored) {
            }
        }
        if (b != null) {
            try {
                blockReach = Math.max(4.5, Math.min(10.0, Double.parseDouble(b.trim())));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("reach.entity", Double.toString(entityReach));
        properties.setProperty("reach.block", Double.toString(blockReach));
    }
}
