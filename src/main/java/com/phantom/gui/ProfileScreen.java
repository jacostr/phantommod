/*
 * ProfileScreen.java — UI screen for managing 4 custom profile slots.
 *
 * Each row shows the profile name (editable), a Save button, and a Load button.
 * Profiles are stored as individual .properties files in config/phantom-profiles/.
 */
package com.phantom.gui;

import com.phantom.config.ProfileManager;
import com.phantom.module.ModuleManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ProfileScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final int ROW_SPACING = 8;

    private final Screen parent;
    private final ModuleManager moduleManager;

    public ProfileScreen(Screen parent, ModuleManager moduleManager) {
        super(Component.literal("Profiles"));
        this.parent = parent;
        this.moduleManager = moduleManager;
    }

    @Override
    protected void init() {
        this.clearWidgets();

        int centerX = this.width / 2;
        int startY = 50;

        for (int slot = 0; slot < ProfileManager.SLOT_COUNT; slot++) {
            int y = startY + slot * (ROW_HEIGHT + ROW_SPACING);
            final int currentSlot = slot;

            // Name edit box
            EditBox nameBox = new EditBox(this.font, centerX - 120, y, 140, ROW_HEIGHT, Component.literal("Name"));
            nameBox.setMaxLength(32);
            nameBox.setValue(ProfileManager.getProfileName(slot));
            nameBox.setResponder(name -> ProfileManager.setProfileName(currentSlot, name));
            this.addRenderableWidget(nameBox);

            // Save button
            this.addRenderableWidget(Button.builder(
                    Component.literal("Save"),
                    button -> {
                        ProfileManager.saveSlot(currentSlot, moduleManager);
                        NotificationManager.push("Saved " + ProfileManager.getProfileName(currentSlot));
                    }).bounds(centerX + 28, y, 44, ROW_HEIGHT).build());

            // Load button
            this.addRenderableWidget(Button.builder(
                    Component.literal("Load"),
                    button -> {
                        boolean loaded = ProfileManager.loadSlot(currentSlot, moduleManager);
                        if (loaded) {
                            NotificationManager.push("Loaded " + ProfileManager.getProfileName(currentSlot));
                        } else {
                            NotificationManager.push("No saved data for " + ProfileManager.getProfileName(currentSlot));
                        }
                    }).bounds(centerX + 76, y, 44, ROW_HEIGHT).build());
        }

        // Back button
        this.addRenderableWidget(Button.builder(
                Component.literal("Back"),
                button -> this.minecraft.setScreen(parent))
                .bounds(centerX - 40, startY + ProfileManager.SLOT_COUNT * (ROW_HEIGHT + ROW_SPACING) + 16, 80, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, 0x90101010);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 24, 0xFFA8E6A3);
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
