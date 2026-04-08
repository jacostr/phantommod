/*
 * ModuleCategory.java — Enum for sorting modules into UI tabs.
 *
 * Defines the main categories (Combat, Movement, Player) used by the ClickGUI
 * and ModuleManager. The "Render" category was merged into "Player" to optimize
 * screen space.
 */
package com.phantom.module;

/**
 * Tabs shown in the ClickGUI. Modules are sorted alphabetically within each tab.
 *
 * <ul>
 *   <li>{@link #COMBAT}   — PvP automation: reach, velocity, aim, crits, autoblock.</li>
 *   <li>{@link #MOVEMENT} — Traversal helpers: sprint, scaffold, speedbridge, no-jump-delay.</li>
 *   <li>{@link #PLAYER}   — Everything else: QoL, visuals, HUD, tools, fall protection, zoom, ESP.</li>
 * </ul>
 *
 * RENDER was merged into PLAYER to keep the tab bar from overflowing the screen width.
 */
public enum ModuleCategory {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    PLAYER("Player");

    private final String label;

    ModuleCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

