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
    private final boolean premadeOnly;
    private int pendingOverwriteSlot = -1;
    private long pendingOverwriteUntil;

    public ProfileScreen(Screen parent, ModuleManager moduleManager, boolean premadeOnly) {
        super(Component.literal(premadeOnly ? "Premade Profiles" : "Custom Profiles"));
        this.parent = parent;
        this.moduleManager = moduleManager;
        this.premadeOnly = premadeOnly;
    }

    @Override
    protected void init() {
        this.clearWidgets();

        int centerX = this.width / 2;
        int startY = 50;

        int rowIdx = 0;
        for (int slot = 0; slot < ProfileManager.SLOT_COUNT; slot++) {
            final int currentSlot = slot;
            boolean isBundled = ProfileManager.hasBundledProfile(slot);
            if (premadeOnly && !isBundled) continue;
            
            int y = startY + rowIdx * (ROW_HEIGHT + ROW_SPACING);
            rowIdx++;

            if (!premadeOnly) {
                // Custom Mode: editable name and save button
                EditBox nameBox = new EditBox(this.font, centerX - 120, y, 140, ROW_HEIGHT, Component.literal("Name"));
                nameBox.setMaxLength(32);
                nameBox.setValue(ProfileManager.getProfileName(slot));
                nameBox.setResponder(name -> ProfileManager.setProfileName(currentSlot, name));
                this.addRenderableWidget(nameBox);

                this.addRenderableWidget(Button.builder(
                        Component.literal(getSaveLabel(currentSlot)),
                        button -> handleSave(currentSlot)).bounds(centerX + 28, y, 52, ROW_HEIGHT).build());
            } else {
                // Premade Mode: Use a label (static bundled name)
                Button label = Button.builder(Component.literal(ProfileManager.getBundledName(slot)), b -> {})
                        .bounds(centerX - 120, y, 200, ROW_HEIGHT)
                        .build();
                label.active = false;
                this.addRenderableWidget(label);
            }

            // Load button
            this.addRenderableWidget(Button.builder(
                    Component.literal("Load"),
                    button -> {
                        clearPendingOverwrite();
                        boolean loaded = premadeOnly
                                ? ProfileManager.loadBundledSlot(currentSlot, moduleManager)
                                : ProfileManager.loadSlot(currentSlot, moduleManager);
                        if (loaded) {
                            NotificationManager.push("Loaded " + (premadeOnly ? ProfileManager.getBundledName(currentSlot) : ProfileManager.getProfileName(currentSlot)));
                        } else {
                            NotificationManager.push("No data for this slot");
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
