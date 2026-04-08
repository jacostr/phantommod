package com.phantom.module;

/**
 * ClickGUI tabs, similar to ghost vs blatant splits on clients like Vape:
 * <ul>
 *   <li>{@link #BLATANT} — obvious automation (scaffold, reach, auto block).</li>
 *   <li>{@link #GHOST} — subtler assists (ESP, hitboxes, speedbridge, no jump delay, autosprint, autotool, autojump).</li>
 *   <li>{@link #RENDER} — visual QoL that is not combat automation (HUD, zoom, gamma).</li>
 * </ul>
 */
public enum ModuleCategory {
    BLATANT("Blatant"),
    GHOST("Ghost"),
    RENDER("Render");

    private final String label;

    ModuleCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
