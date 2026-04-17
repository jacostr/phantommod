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
    private static final long SAVE_CONFIRM_WINDOW_MS = 2500L;

    private final Screen parent;
    private final ModuleManager moduleManager;
    private int pendingOverwriteSlot = -1;
    private long pendingOverwriteUntil;

    public ProfileScreen(Screen parent, ModuleManager moduleManager) {
        super(Component.literal("Configs"));
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
                    Component.literal(getSaveLabel(currentSlot)),
                    button -> handleSave(currentSlot)).bounds(centerX + 28, y, 52, ROW_HEIGHT).build());

            // Load button
            this.addRenderableWidget(Button.builder(
                    Component.literal("Load"),
                    button -> {
                        clearPendingOverwrite();
                        boolean loaded = ProfileManager.loadSlot(currentSlot, moduleManager);
                        if (loaded) {
                            NotificationManager.push("Loaded " + ProfileManager.getProfileName(currentSlot));
                        } else {
                            NotificationManager.push("No saved data for " + ProfileManager.getProfileName(currentSlot));
                        }
                    }).bounds(centerX + 84, y, 52, ROW_HEIGHT).build());
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
        
        graphics.drawCenteredString(this.font, "To import your friends configs manually,", this.width / 2, this.height - 40, 0xFFAAAAAA);
        graphics.drawCenteredString(this.font, "copy .properties files into .minecraft/phantom-profiles", this.width / 2, this.height - 26, 0xFFAAAAAA);
        
        super.render(graphics, mouseX, mouseY, delta);
        NotificationManager.render(graphics, 10, this.height - 54);
    }

    @Override
    public void tick() {
        if (pendingOverwriteSlot != -1 && System.currentTimeMillis() > pendingOverwriteUntil) {
            clearPendingOverwrite();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void handleSave(int slot) {
        String profileName = ProfileManager.getProfileName(slot);
        boolean hasExistingSave = ProfileManager.hasSavedSlot(slot);
        boolean confirmingOverwrite = pendingOverwriteSlot == slot
                && System.currentTimeMillis() <= pendingOverwriteUntil;

        if (hasExistingSave && !confirmingOverwrite) {
            pendingOverwriteSlot = slot;
            pendingOverwriteUntil = System.currentTimeMillis() + SAVE_CONFIRM_WINDOW_MS;
            NotificationManager.push("Press Save again to overwrite " + profileName);
            init();
            return;
        }

        ProfileManager.saveSlot(slot, moduleManager);
        clearPendingOverwrite();
        NotificationManager.push("Saved " + profileName);
        init();
    }

    private void clearPendingOverwrite() {
        if (pendingOverwriteSlot == -1) {
            return;
        }
        pendingOverwriteSlot = -1;
        pendingOverwriteUntil = 0L;
        init();
    }

    private String getSaveLabel(int slot) {
        boolean waitingForConfirm = pendingOverwriteSlot == slot
                && System.currentTimeMillis() <= pendingOverwriteUntil;
        return waitingForConfirm ? "Sure?" : "Save";
    }
}
