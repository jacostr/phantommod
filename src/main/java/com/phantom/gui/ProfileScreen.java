package com.phantom.gui;

import com.phantom.config.ProfileManager;
import com.phantom.gui.framework.*;
import com.phantom.module.ModuleManager;
import com.phantom.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Modernized ProfileScreen with a liquid glassy aesthetic.
 */
public class ProfileScreen extends Screen {
    private int panelWidth;
    private int panelHeight;
    private static final long SAVE_CONFIRM_WINDOW_MS = 2500L;

    private final Screen parent;
    private final ModuleManager moduleManager;
    private final boolean premadeOnly;
    private final List<BaseComponent> components = new ArrayList<>();
    
    private int pendingOverwriteSlot = -1;
    private long pendingOverwriteUntil;

    private boolean isPremade;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    public ProfileScreen(Screen parent, ModuleManager moduleManager, boolean premadeOnly) {
        super(Component.literal(premadeOnly ? "Premade Profiles" : "Custom Profiles"));
        this.parent = parent;
        this.moduleManager = moduleManager;
        this.premadeOnly = premadeOnly;
        this.isPremade = premadeOnly;
    }

    @Override
    protected void init() {
        this.panelWidth = Math.min(this.width - 40, (int)(this.width * 0.7));
        this.panelHeight = Math.min(this.height - 40, (int)(this.height * 0.6));
        rebuildUI();
    }

    private void rebuildUI() {
        components.clear();
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int startX = centerX - panelWidth / 2 + 20;
        int startY = centerY - panelHeight / 2 + 50 - scrollOffset;

        // Switcher Button
        components.add(new ModernButton(centerX + panelWidth / 2 - 90, centerY - panelHeight / 2 + 10, 80, 20, 
            Component.literal(isPremade ? "To Custom" : "To Premade"), btn -> {
                isPremade = !isPremade;
                scrollOffset = 0;
                rebuildUI();
            }));

        int slotY = startY;
        for (int slot = 0; slot < ProfileManager.SLOT_COUNT; slot++) {
            final int currentSlot = slot;
            boolean isBundled = ProfileManager.hasBundledProfile(slot);
            if (isPremade && !isBundled) continue;

            if (!isPremade) {
                // Custom Name Field
                ModernTextField nameField = new ModernTextField(startX, slotY, 140, 20, "Profile Name", name -> {
                    ProfileManager.setProfileName(currentSlot, name);
                });
                nameField.setText(ProfileManager.getProfileName(slot));
                components.add(nameField);

                // Save Button
                components.add(new ModernButton(startX + 150, slotY, 50, 20, Component.literal(getSaveLabel(currentSlot)), btn -> {
                    handleSave(currentSlot);
                }));
            }

            // Load Button
            components.add(new ModernButton(startX + 210, slotY, 50, 20, Component.literal("Load"), btn -> {
                pendingOverwriteSlot = -1;
                boolean loaded = isPremade 
                    ? ProfileManager.loadBundledSlot(currentSlot, moduleManager) 
                    : ProfileManager.loadSlot(currentSlot, moduleManager);
                if (loaded) {
                    NotificationManager.push("Loaded " + (isPremade ? ProfileManager.getBundledName(currentSlot) : ProfileManager.getProfileName(currentSlot)));
                } else {
                    NotificationManager.push("No data for this slot");
                }
            }));
            slotY += 30;
        }

        maxScroll = Math.max(0, slotY + scrollOffset - (centerY + panelHeight / 2 - 40));

        // Back Button
        components.add(new ModernButton(centerX - 40, centerY + panelHeight / 2 - 30, 80, 20, Component.literal("Back"), btn -> {
            this.minecraft.setScreen(parent);
        }));
    }

    private void handleSave(int slot) {
        String profileName = ProfileManager.getProfileName(slot);
        boolean hasExistingSave = ProfileManager.hasSavedSlot(slot);
        boolean confirmingOverwrite = pendingOverwriteSlot == slot && System.currentTimeMillis() <= pendingOverwriteUntil;

        if (hasExistingSave && !confirmingOverwrite) {
            pendingOverwriteSlot = slot;
            pendingOverwriteUntil = System.currentTimeMillis() + SAVE_CONFIRM_WINDOW_MS;
            NotificationManager.push("Click Save again to overwrite " + profileName);
            rebuildUI();
            return;
        }

        ProfileManager.saveSlot(slot, moduleManager);
        pendingOverwriteSlot = -1;
        NotificationManager.push("Saved " + profileName);
        rebuildUI();
    }

    private String getSaveLabel(int slot) {
        boolean waitingForConfirm = pendingOverwriteSlot == slot && System.currentTimeMillis() <= pendingOverwriteUntil;
        return waitingForConfirm ? "Sure?" : "Save";
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Parent as background
        this.parent.render(graphics, -1, -1, delta);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        // Main Glass Panel
        RenderUtil.drawGlassPanel(graphics, centerX - panelWidth / 2, centerY - panelHeight / 2, 
            panelWidth, panelHeight, 0xE0050505, 0x40FFFFFF);

        net.minecraft.network.chat.FontDescription cleanFont = new net.minecraft.network.chat.FontDescription.Resource(net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "uniform"));
        graphics.drawCenteredString(this.font, net.minecraft.network.chat.Component.literal(isPremade ? "PREMADE PROFILES" : "CUSTOM PROFILES").withStyle(s -> s.withFont(cleanFont)), centerX, centerY - panelHeight / 2 + 15, 0xFFA8E6A3);

        // Render premade labels if needed
        if (isPremade) {
            int labelX = centerX - panelWidth / 2 + 20;
            int yOffset = 0;
            int startY = centerY - panelHeight / 2 + 56 - scrollOffset;
            for (int slot = 0; slot < ProfileManager.SLOT_COUNT; slot++) {
                if (ProfileManager.hasBundledProfile(slot)) {
                    int y = startY + yOffset;
                    if (y > centerY - panelHeight / 2 + 40 && y < centerY + panelHeight / 2 - 40) {
                        graphics.drawString(this.font, net.minecraft.network.chat.Component.literal(ProfileManager.getBundledName(slot)).withStyle(s -> s.withFont(cleanFont)), labelX, y, 0xFFFFFFFF, false);
                    }
                    yOffset += 30;
                }
            }
        }

        for (BaseComponent component : components) {
            String label = "";
            if (component instanceof ModernButton mb) label = mb.getMessage().getString();

            if (label.equals("Back") || label.contains("To ")) {
                component.render(graphics, mouseX, mouseY, delta);
                continue;
            }
            if (component.getY() + component.getHeight() > centerY - panelHeight / 2 + 40 &&
                component.getY() < centerY + panelHeight / 2 - 40) {
                component.render(graphics, mouseX, mouseY, delta);
            }
        }

        // Draw Scroll Bar
        if (maxScroll > 0) {
            int scrollBarX = centerX + panelWidth / 2 - 8;
            int scrollBarY = centerY - panelHeight / 2 + 50;
            int scrollBarHeight = panelHeight - 90;
            graphics.fill(scrollBarX, scrollBarY, scrollBarX + 4, scrollBarY + scrollBarHeight, 0x40000000);
            int thumbHeight = Math.max(20, (int) ((double) scrollBarHeight * scrollBarHeight / (maxScroll + scrollBarHeight)));
            int thumbY = scrollBarY + (int) ((double) scrollOffset / maxScroll * (scrollBarHeight - thumbHeight));
            graphics.fill(scrollBarX, thumbY, scrollBarX + 4, thumbY + thumbHeight, 0xFFA8E6A3);
        }

        NotificationManager.render(graphics);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean isFocused) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        for (BaseComponent component : components) {
            String label = "";
            if (component instanceof ModernButton mb) label = mb.getMessage().getString();

            if (label.equals("Back") || label.contains("To ")) {
                if (component.mouseClicked(mouseX, mouseY, button)) return true;
                continue;
            }
            if (component.getY() + component.getHeight() > centerY - panelHeight / 2 + 40 &&
                component.getY() < centerY + panelHeight / 2 - 40) {
                if (component.mouseClicked(mouseX, mouseY, button)) return true;
            }
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
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        for (BaseComponent component : components) {
            if (component.keyPressed(event.key(), event.scancode(), event.modifiers())) return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        for (BaseComponent component : components) {
            if (component.charTyped((char) event.codepoint(), 0)) return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int nextOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int)(scrollY * 20)));
        if (nextOffset != scrollOffset) {
            scrollOffset = nextOffset;
            rebuildUI();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void tick() {
        if (pendingOverwriteSlot != -1 && System.currentTimeMillis() > pendingOverwriteUntil) {
            pendingOverwriteSlot = -1;
            rebuildUI();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
