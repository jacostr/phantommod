/*
 * ModuleCategory.java — Enum for sorting modules into UI tabs.
 *
 * Defines the main categories (Combat, Movement, Player, SMP) used by the ClickGUI
 * and ModuleManager. The "Render" category was merged into "Player" to optimize
 * screen space. SMP holds server-survival utilities (ESP for blocks, XP throwing, etc.).
 */
package com.phantom.module;

/**
 * Tabs shown in the ClickGUI. Modules are sorted alphabetically within each tab.
 *
 * <ul>
 *   <li>{@link #COMBAT}   — PvP automation: reach, aim, crits, block-hit, trigger logic.</li>
 *   <li>{@link #MOVEMENT} — Traversal helpers: sprint, W-tap, scaffold, speedbridge, no-jump-delay.</li>
 *   <li>{@link #PLAYER}   — QoL, visuals, HUD, tools, fall protection, entity ESP.</li>
 *   <li>{@link #SMP}      — Survival multiplayer utilities: ore/chest/bed/shulker ESP, XP throwing.</li>
 * </ul>
 */
public enum ModuleCategory {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    PLAYER("Player"),
    SMP("SMP");

    private final String label;

    ModuleCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
