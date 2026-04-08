package com.phantom.gui;

import com.phantom.PhantomMod;
import com.phantom.config.ClientConfig;
import com.phantom.module.ModuleManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Panic key binding and entry to the all-modules help / keybind screen.
 */
public class PhantomSettingsScreen extends Screen {
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_SPACING = 6;

    private final Screen parent;
    private final ModuleManager moduleManager;

    private boolean listeningPanicKey;

    public PhantomSettingsScreen(Screen parent, ModuleManager moduleManager) {
        super(Component.literal("Phantom — Client"));
        this.parent = parent;
        this.moduleManager = moduleManager;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        int cx = this.width / 2;

        this.addRenderableWidget(Button.builder(
                        Component.literal(listeningPanicKey ? "Press a key…" : "Set panic key: " + panicKeyName()),
                        b -> listeningPanicKey = true)
                .bounds(cx - 100, 46, 200, ROW_HEIGHT)
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.literal("Reset panic key to End"),
                        b -> {
                            ClientConfig.setPanicKeyGlfw(GLFW.GLFW_KEY_END);
                            PhantomMod.syncPanicKeybinding();
                            listeningPanicKey = false;
                            PhantomMod.saveConfig();
                            init();
                        })
                .bounds(cx - 100, 46 + ROW_HEIGHT + ROW_SPACING, 200, ROW_HEIGHT)
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.literal("Module keys & how to use"),
                        b -> this.minecraft.setScreen(new AllModulesConfigScreen(this, moduleManager)))
                .bounds(cx - 100, 46 + 2 * (ROW_HEIGHT + ROW_SPACING), 200, ROW_HEIGHT)
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.literal("Back"),
                        b -> this.minecraft.setScreen(parent))
                .bounds(cx - 50, this.height - 32, 100, ROW_HEIGHT)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, 0xC0101010);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 18, 0xFFFFFFFF);
        graphics.drawCenteredString(this.font, Component.literal("Panic disables every module and saves."), this.width / 2, 32, 0xFFAAAAAA);
        graphics.drawCenteredString(this.font,
                Component.literal("Your settings are kept in config/phantom-memory.properties."),
                this.width / 2, 86, 0xFF888888);
        if (listeningPanicKey) {
            graphics.drawCenteredString(this.font, Component.literal("Press a key for panic. ESC = End."),
                    this.width / 2, this.height - 48, 0xFFE6C278);
        }
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (listeningPanicKey) {
            int k = event.key() == GLFW.GLFW_KEY_ESCAPE ? GLFW.GLFW_KEY_END : event.key();
            ClientConfig.setPanicKeyGlfw(k);
            PhantomMod.syncPanicKeybinding();
            listeningPanicKey = false;
            PhantomMod.saveConfig();
            init();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.minecraft.setScreen(parent);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private String panicKeyName() {
        int k = ClientConfig.getPanicKeyGlfw();
        return InputConstants.getKey(new KeyEvent(k, 0, 0)).getDisplayName().getString().toUpperCase();
    }
}
