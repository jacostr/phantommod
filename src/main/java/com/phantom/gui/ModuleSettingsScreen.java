package com.phantom.gui;

import com.phantom.module.Module;
import com.phantom.module.impl.movement.SpeedBridge;
import com.phantom.module.impl.render.ESP;
import com.phantom.module.impl.render.HudModule;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class ModuleSettingsScreen extends Screen {
    private static final int PANEL_WIDTH = 260;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_SPACING = 4;
    private static final int TEXT_SPACING = 10;

    private final Screen parent;
    private final Module module;
    private int scrollOffset;
    private int maxScroll;
    private boolean listeningForKey;

    public ModuleSettingsScreen(Screen parent, Module module) {
        super(Component.literal(module.getName() + " Settings"));
        this.parent = parent;
        this.module = module;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        updateMaxScroll();

        int centerX = this.width / 2;
        int y = 54 - scrollOffset;
        int left = centerX - PANEL_WIDTH / 2;

        List<FormattedCharSequence> descriptionLines = this.font.split(Component.literal(module.getDescription()),
                PANEL_WIDTH - 20);
        y += descriptionLines.size() * 9 + TEXT_SPACING;

        if (module instanceof ESP esp) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("Players: " + onOff(esp.isPlayersEnabled())),
                    button -> {
                        esp.setPlayersEnabled(!esp.isPlayersEnabled());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(Button.builder(
                    Component.literal("Mobs: " + onOff(esp.isMobsEnabled())),
                    button -> {
                        esp.setMobsEnabled(!esp.isMobsEnabled());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(Button.builder(
                    Component.literal("Animals: " + onOff(esp.isAnimalsEnabled())),
                    button -> {
                        esp.setAnimalsEnabled(!esp.isAnimalsEnabled());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
        }

        if (module instanceof HudModule hudModule) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("Show FPS: " + onOff(hudModule.isShowFps())),
                    button -> {
                        hudModule.setShowFps(!hudModule.isShowFps());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(Button.builder(
                    Component.literal("Show Ping: " + onOff(hudModule.isShowPing())),
                    button -> {
                        hudModule.setShowPing(!hudModule.isShowPing());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
        }

        if (module instanceof SpeedBridge) {
            y += ROW_SPACING;
        }

        this.addRenderableWidget(Button.builder(
                Component.literal("Hotkey: " + getKeyName(module.getKey())),
                button -> {
                }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
        y += ROW_HEIGHT + ROW_SPACING;

        this.addRenderableWidget(Button.builder(
                Component.literal(listeningForKey ? "Press a key..." : "Set Hotkey"),
                button -> {
                    listeningForKey = true;
                    init();
                }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
        y += ROW_HEIGHT + ROW_SPACING;

        this.addRenderableWidget(Button.builder(
                Component.literal("Clear Hotkey"),
                button -> {
                    module.setKey(-1);
                    listeningForKey = false;
                    init();
                }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Back"),
                button -> this.minecraft.setScreen(parent)).bounds(centerX - 50, this.height - 42, 100, ROW_HEIGHT)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, 0x90101010);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 22, 0xFFFFFFFF);
        drawDescription(graphics);
        if (listeningForKey) {
            graphics.drawCenteredString(this.font, Component.literal("Press any key to bind it. Press ESC to clear."),
                    this.width / 2, this.height - 64, 0xFFE6C278);
        }
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (maxScroll <= 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        int nextOffset = clamp(scrollOffset - (int) (scrollY * 18.0D), 0, maxScroll);
        if (nextOffset == scrollOffset) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        scrollOffset = nextOffset;
        init();
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (listeningForKey) {
            module.setKey(event.key() == GLFW.GLFW_KEY_ESCAPE ? -1 : event.key());
            listeningForKey = false;
            init();
            return true;
        }

        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.minecraft.setScreen(parent);
            return true;
        }

        return super.keyPressed(event);
    }

    private String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }

    private void updateMaxScroll() {
        int contentHeight = getDescriptionHeight() + TEXT_SPACING;

        if (module instanceof ESP) {
            contentHeight += 3 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof HudModule) {
            contentHeight += 2 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof SpeedBridge) {
            contentHeight += ROW_SPACING;
        }

        contentHeight += 3 * (ROW_HEIGHT + ROW_SPACING);

        int visibleHeight = Math.max(40, this.height - 104);
        maxScroll = Math.max(0, contentHeight - visibleHeight);
        scrollOffset = clamp(scrollOffset, 0, maxScroll);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void drawDescription(GuiGraphics graphics) {
        int left = this.width / 2 - PANEL_WIDTH / 2 + 10;
        int y = 54 - scrollOffset;

        for (FormattedCharSequence line : this.font.split(Component.literal(module.getDescription()),
                PANEL_WIDTH - 20)) {
            graphics.drawString(this.font, line, left, y, 0xFFD9D9D9);
            y += 9;
        }
    }

    private int getDescriptionHeight() {
        return this.font.split(Component.literal(module.getDescription()), PANEL_WIDTH - 20).size() * 9;
    }

    private String getKeyName(int key) {
        if (key == -1) {
            return "NONE";
        }

        return InputConstants.getKey(new KeyEvent(key, 0, 0)).getDisplayName().getString().toUpperCase();
    }
}
