/*
 * Hold-to-zoom: enable the module in the GUI, then hold the bound key (default C) to zoom.
 * Strength is configured in settings; releasing the key restores FOV immediately.
 */
package com.phantom.module.impl.render;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

import java.util.Properties;

public class Zoom extends Module {
    /** Larger value = stronger zoom (FOV is divided by this while holding). */
    private double zoomLevel = 4.0;
    /** FOV captured when zoom starts; restored when the hold ends or zoom is disabled. */
    private double baseFov;
    private boolean zoomingActive;

    public Zoom() {
        super("Zoom",
                "Hold the bound key to zoom in. Turn the module on in the GUI first; adjust strength in settings. Default key: C.",
                ModuleCategory.RENDER,
                GLFW.GLFW_KEY_C);
    }

    @Override
    public boolean usesToggleKeybind() {
        return false;
    }

    @Override
    public void onDisable() {
        endZoom();
    }

    /**
     * While the module is enabled, applies zoom only while the key is held and no menu is open.
     */
    @Override
    public void onTick() {
        if (mc.options == null || mc.getWindow() == null) {
            return;
        }

        if (mc.screen != null) {
            endZoom();
            return;
        }

        int key = getKey();
        if (key == -1) {
            endZoom();
            return;
        }

        boolean held = InputConstants.isKeyDown(mc.getWindow(), key);
        if (held) {
            if (!zoomingActive) {
                baseFov = mc.options.fov().get();
                zoomingActive = true;
            }
            mc.options.fov().set((int) (baseFov / zoomLevel));
        } else {
            endZoom();
        }
    }

    private void endZoom() {
        if (!zoomingActive || mc.options == null) {
            zoomingActive = false;
            return;
        }
        mc.options.fov().set((int) baseFov);
        zoomingActive = false;
    }

    @Override
    public boolean hasConfigurableSettings() {
        return true;
    }

    @Override
    public Screen createSettingsScreen(Screen parent) {
        return new ModuleSettingsScreen(parent, this);
    }

    public double getZoomLevel() {
        return zoomLevel;
    }

    public void setZoomLevel(double zoomLevel) {
        this.zoomLevel = Math.max(2.0, Math.min(10.0, zoomLevel));
        if (isEnabled() && zoomingActive && mc.options != null) {
            mc.options.fov().set((int) (baseFov / this.zoomLevel));
        }
        saveConfig();
    }

    @Override
    public void loadConfig(Properties properties) {
        super.loadConfig(properties);
        String v = properties.getProperty("zoom.level");
        if (v != null) {
            try {
                zoomLevel = Math.max(2.0, Math.min(10.0, Double.parseDouble(v)));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("zoom.level", Double.toString(zoomLevel));
    }
}
