package com.phantom.module;

import com.phantom.PhantomMod;
import com.phantom.gui.NotificationManager;
import com.phantom.gui.ModuleSettingsScreen;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

import java.util.Locale;
import java.util.Properties;

public abstract class Module {
    private final String name;
    private final String description;
    private final ModuleCategory category;
    private int key;
    private boolean enabled;
    private boolean keyWasDown;
    protected static final Minecraft mc = Minecraft.getInstance();

    public Module(String name, String description, ModuleCategory category, int key) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.key = key;
        this.enabled = false;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /** Longer usage text for the all-modules help screen; defaults to the short description. */
    public String getUsageGuide() {
        return description;
    }

    public ModuleCategory getCategory() {
        return category;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
        saveConfig();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean wasKeyDown() {
        return keyWasDown;
    }

    public void setKeyWasDown(boolean keyWasDown) {
        this.keyWasDown = keyWasDown;
    }

    public void initializeEnabledSilently(boolean enabled) {
        this.enabled = enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }

        this.enabled = enabled;
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }

        NotificationManager.push(name + (enabled ? " enabled" : " disabled"));
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    /** Disable without toast or {@link #saveConfig()} — use with a single batch save after (e.g. panic). */
    public void disableSilently() {
        if (!enabled) {
            return;
        }
        enabled = false;
        onDisable();
    }

    /**
     * When false, the bound key is hold-to-use instead of edge-toggle in {@link ModuleManager#handleKeybinds()}.
     */
    public boolean usesToggleKeybind() {
        return true;
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public void onTick() {
    }

    public void onRender(WorldRenderContext context) {
    }

    public void onHudRender(GuiGraphics graphics) {
    }

    public boolean hasSettings() {
        return true;
    }

    public boolean hasConfigurableSettings() {
        return false;
    }

    public String getSettingsButtonLabel() {
        return "Opt";
    }

    public Screen createSettingsScreen(Screen parent) {
        return new ModuleSettingsScreen(parent, this);
    }

    public void loadConfig(Properties properties) {
        String id = getConfigId();
        
        String savedKey = properties.getProperty(id + ".key");
        if (savedKey != null) {
            try {
                this.key = Integer.parseInt(savedKey);
            } catch (NumberFormatException ignored) {
            }
        }

        String savedEnabled = properties.getProperty(id + ".enabled");
        if (savedEnabled != null) {
            this.enabled = Boolean.parseBoolean(savedEnabled);
        }
    }

    public void saveConfig(Properties properties) {
        String id = getConfigId();
        properties.setProperty(id + ".key", Integer.toString(key));
        properties.setProperty(id + ".enabled", Boolean.toString(enabled));
    }

    protected void saveConfig() {
        PhantomMod.saveConfig();
    }

    private String getConfigId() {
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
    }

    /**
     * After config load, sync runtime side effects with the loaded {@code enabled} flag
     * (load does not call {@link #onEnable()} or {@link #onDisable()}).
     */
    public void applyLoadedEnableState() {
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }
}
