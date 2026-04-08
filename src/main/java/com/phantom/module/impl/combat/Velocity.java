/*
 * Velocity.java — Reduces incoming knockback by a configurable percentage (Combat module).
 *
 * Works via ClientPacketListenerMixin which scales player.deltaMovement after the server
 * applies knockback. A global kbPercent slider (0%=none, 100%=vanilla) controls strength.
 * Presets: Legit 90%, Subtle 75%, Blatant 40%, None 0%.
 * Detectability: Subtle at ≥70%, Blatant at low values.
 */
package com.phantom.module.impl.combat;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.gui.screens.Screen;

import java.util.Properties;

public class Velocity extends Module {
    private double kbPercent = 0.0; // 0.0 = 0% kb (None), 1.0 = 100% (Vanilla)

    public Velocity() {
        super("Velocity", "Reduces incoming knockback according to your percentage.\nDetectability: Blatant/Subtle", ModuleCategory.COMBAT, -1);
    }

    @Override
    public boolean hasConfigurableSettings() {
        return true;
    }

    @Override
    public Screen createSettingsScreen(Screen parent) {
        return new ModuleSettingsScreen(parent, this);
    }

    public double getKbPercent() {
        return kbPercent;
    }

    public void setKbPercent(double kbPercent) {
        this.kbPercent = Math.max(0.0, Math.min(1.0, kbPercent));
        saveConfig();
    }

    public void applyPresetLegit() {
        setKbPercent(0.90);
    }

    public void applyPresetSubtle() {
        setKbPercent(0.75);
    }

    public void applyPresetBlatant() {
        setKbPercent(0.40);
    }

    public void applyPresetNone() {
        setKbPercent(0.0);
    }

    @Override
    public void loadConfig(Properties properties) {
        super.loadConfig(properties);
        String v = properties.getProperty("velocity.kb");
        if (v != null) {
            try {
                kbPercent = Math.max(0.0, Math.min(1.0, Double.parseDouble(v.trim())));
            } catch (NumberFormatException ignored) {}
        }
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("velocity.kb", Double.toString(kbPercent));
    }
}
