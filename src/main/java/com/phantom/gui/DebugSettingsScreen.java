package com.phantom.gui;

import com.phantom.PhantomMod;
import com.phantom.gui.framework.*;
import com.phantom.module.impl.render.HudModule;
import com.phantom.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Modernized DebugSettingsScreen with a liquid glassy aesthetic.
 */
public class DebugSettingsScreen extends Screen {
    private static final int PANEL_WIDTH = 280;
    private static final int PANEL_HEIGHT = 200;
    
    private final Screen parent;
    private final HudModule hudModule;
    private final List<BaseComponent> components = new ArrayList<>();

    public DebugSettingsScreen(Screen parent) {
        super(Component.literal("Debug & Stealth Settings"));
        this.parent = parent;
        this.hudModule = PhantomMod.getModuleManager().getModuleByClass(HudModule.class);
    }

    @Override
    protected void init() {
        rebuildUI();
    }

    private void rebuildUI() {
        components.clear();
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int startX = centerX - PANEL_WIDTH / 2 + 20;
        int y = centerY - PANEL_HEIGHT / 2 + 60;

        if (hudModule != null) {
            // Debug Console Toggle
            addToggle(startX, y, "Debug Console", hudModule.isDebugLogger(), val -> {
                hudModule.setDebugLogger(val);
            });
            y += 30;

            // Log File Toggle
            addToggle(startX, y, "Log File", hudModule.isFileLogger(), val -> {
                hudModule.setFileLogger(val);
            });
            y += 30;
        }

        // Back Button
        components.add(new ModernButton(centerX - 40, centerY + PANEL_HEIGHT / 2 - 30, 80, 20, Component.literal("Back"), btn -> {
            this.minecraft.setScreen(parent);
        }));
    }

    private void addToggle(int x, int y, String label, boolean enabled, java.util.function.Consumer<Boolean> consumer) {
        components.add(new ModernToggle(x, y, PANEL_WIDTH - 40, 20, label, enabled, consumer));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.parent.render(graphics, -1, -1, delta);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        // Main Glass Panel
        RenderUtil.drawGlassPanel(graphics, centerX - PANEL_WIDTH / 2, centerY - PANEL_HEIGHT / 2, 
            PANEL_WIDTH, PANEL_HEIGHT, 0x90050505, 0x40FFFFFF);

        graphics.drawCenteredString(this.font, "STEALTH & LOGGING", centerX, centerY - PANEL_HEIGHT / 2 + 15, 0xFFA8E6A3);
        int y = centerY - PANEL_HEIGHT / 2 + 66;

        for (BaseComponent component : components) {
            component.render(graphics, mouseX, mouseY, delta);
        }

        NotificationManager.render(graphics);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean isFocused) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        for (BaseComponent component : components) {
            if (component.mouseClicked(mouseX, mouseY, button)) return true;
        }
        return super.mouseClicked(event, isFocused);
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        for (BaseComponent component : components) {
            component.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
