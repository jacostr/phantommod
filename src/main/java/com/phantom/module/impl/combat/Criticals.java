/* Copyright (c) 2026 PhantomMod. All rights reserved. */
/*
 * Criticals.java — Forces critical hits via spoofed position packets (Combat module).
 *
 * Uses MultiPlayerGameModeMixin to send 4 movement packets before each attack that
 * simulate a tiny vertical bounce. The server thinks the player was airborne and
 * registers the hit as a critical. A configurable chance slider randomises when
 * crits fire to reduce anti-cheat pattern detection.
 * Detectability: Blatant — servers can correlate position packets with attack timing.
 */
package com.phantom.module.impl.combat;

import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;

import java.util.Properties;

public class Criticals extends Module {
    private double chance = 0.6; // 60% default

    public Criticals() {
        super("Criticals", "Forces critical hits by spoofing mini-jumps before attacks. Uses a Chance % limit to reduce anti-cheat flags.\nDetectability: Blatant", ModuleCategory.COMBAT, -1);
    }

    @Override
    public boolean hasConfigurableSettings() {
        return true;
    }



    public double getChance() {
        return chance;
    }

    public void setChance(double chance) {
        this.chance = Math.max(0.0, Math.min(1.0, chance));
        saveConfig();
    }

    @Override
    public void loadConfig(Properties properties) {
        super.loadConfig(properties);
        String c = properties.getProperty("criticals.chance");
        if (c != null) {
            try {
                chance = Math.max(0.0, Math.min(1.0, Double.parseDouble(c.trim())));
            } catch (NumberFormatException ignored) {}
        }
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("criticals.chance", Double.toString(chance));
    }
}
