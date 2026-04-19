/* Copyright (c) 2026 PhantomMod. All rights reserved. */
package com.phantom.gui;

import com.phantom.module.impl.render.HudModule;
import com.phantom.PhantomMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class DebugSettingsScreen extends Screen {
    private final Screen parent;
    private final HudModule hudModule;

    public DebugSettingsScreen(Screen parent) {
        super(Component.literal("Debug & Stealth Settings"));
        this.parent = parent;
        this.hudModule = PhantomMod.getModuleManager().getModuleByClass(HudModule.class);
    }

    @Override
    protected void init() {
        this.clearWidgets();
        int centerX = this.width / 2;
        int y = 60;

        if (hudModule != null) {
            // Debug Console Toggle
            this.addRenderableWidget(Button.builder(
                    Component.literal("Debug Console: " + (hudModule.isDebugLogger() ? "ON" : "OFF")),
                    button -> {
                        hudModule.setDebugLogger(!hudModule.isDebugLogger());
                        init();
                    }).bounds(centerX - 80, y, 160, 20).build());
            y += 24;

            // Log File Toggle
            this.addRenderableWidget(Button.builder(
                    Component.literal("Log File: " + (hudModule.isFileLogger() ? "ON" : "OFF")),
                    button -> {
                        hudModule.setFileLogger(!hudModule.isFileLogger());
                        init();
                    }).bounds(centerX - 80, y, 160, 20).build());
            y += 24;
        }

        // Back Button
        this.addRenderableWidget(Button.builder(Component.literal("Back"), button -> {
            this.minecraft.setScreen(parent);
        }).bounds(centerX - 80, this.height - 40, 160, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
        
        graphics.drawCenteredString(this.font, "Stealth & Logging Configuration", this.width / 2, 40, 0xFFA8E6A3);

        int textY = 120;
        graphics.drawCenteredString(this.font, "Debug Console: Prints active mod events to your launch terminal.", this.width / 2, textY, 0xFFBBBBBB);
        graphics.drawCenteredString(this.font, "Log File: Silently writes mod activity to '.minecraft/phantom.log'.", this.width / 2, textY + 12, 0xFFBBBBBB);
        graphics.drawCenteredString(this.font, "Stealth Mode: The mod is 100% silent unless toggled ON.", this.width / 2, textY + 24, 0xFF888888);
        
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, 0x90101010);
    }
}
