/*
 * ClickGUIScreen.java — The main module toggle overlay, opened with M.
 *
 * Displays all registered modules in categorised tabs (Combat, Movement, Player).
 * Each row has an enable/disable button and a hamburger (≡) icon that opens
 * ModuleSettingsScreen for that module. Includes a search bar and scroll support.
 *
 * This is the primary interface users interact with to manage PhantomMod features.
 */
package com.phantom.gui;

import com.phantom.PhantomMod;
import com.phantom.config.ProfileManager;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import com.phantom.module.ModuleManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ClickGUIScreen extends Screen {
    private static final int BUTTON_WIDTH = 150;
    private static final int CFG_WIDTH = 34;
    private static final int PROFILE_WIDTH = 86;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_SPACING = 6;
    private static final int TAB_HEIGHT = 20;
    /** Pixels from top: tabs + search box. */
    private static final int LIST_TOP = 52;

    private final ModuleManager moduleManager;
    private ModuleCategory selectedCategory = ModuleCategory.COMBAT;
    private int scrollOffset;
    private int maxScroll;
    private String searchText = "";
    private boolean rebuildingSearch;

    public ClickGUIScreen(ModuleManager moduleManager) {
        super(Component.literal("Phantom Mod"));
        this.moduleManager = moduleManager;
    }

    @Override
    protected void init() {
        rebuildUI();
    }

    private void rebuildUI() {
        rebuildingSearch = true;
        this.clearWidgets();
        
        int tabWidth = 80;
        int totalTabsWidth = ModuleCategory.values().length * tabWidth;
        int startX = (this.width - totalTabsWidth) / 2;

        for (int i = 0; i < ModuleCategory.values().length; i++) {
            ModuleCategory category = ModuleCategory.values()[i];
            boolean isSelected = category == selectedCategory;

            this.addRenderableWidget(Button.builder(
                            Component.literal((isSelected ? "> " : "") + category.getLabel()),
                            button -> {
                                selectedCategory = category;
                                scrollOffset = 0;
                                rebuildUI();
                            })
                    .bounds(startX + (i * tabWidth), 4, tabWidth - 2, TAB_HEIGHT)
                    .build());
        }

        EditBox search = new EditBox(this.font, 24, 28, this.width - 48, 18, Component.literal("search"));
        search.setMaxLength(64);
        search.setHint(Component.literal("Search modules…"));
        search.setValue(searchText);
        search.setResponder(s -> {
            if (rebuildingSearch) {
                return;
            }
            searchText = s;
            scrollOffset = 0;
            rebuildUI();
        });
        this.addRenderableWidget(search);

        int profileX = 6;
        int profileY = LIST_TOP + 18;
        this.addRenderableWidget(Button.builder(
                        Component.literal("Legit"),
                        button -> {
                            ProfileManager.applyLegit(moduleManager);
                            NotificationManager.push("Loaded Legit profile");
                            rebuildUI();
                        })
                .bounds(profileX, profileY, PROFILE_WIDTH, ROW_HEIGHT)
                .build());
        profileY += ROW_HEIGHT + 4;

        this.addRenderableWidget(Button.builder(
                        Component.literal("Obvious"),
                        button -> {
                            ProfileManager.applyObvious(moduleManager);
                            NotificationManager.push("Loaded Obvious profile");
                            rebuildUI();
                        })
                .bounds(profileX, profileY, PROFILE_WIDTH, ROW_HEIGHT)
                .build());
        profileY += ROW_HEIGHT + 4;

        this.addRenderableWidget(Button.builder(
                        Component.literal("Custom"),
                        button -> {
                            boolean loaded = ProfileManager.loadCustom(moduleManager);
                            NotificationManager.push(loaded ? "Loaded Custom profile" : "No Custom profile saved");
                            rebuildUI();
                        })
                .bounds(profileX, profileY, PROFILE_WIDTH, ROW_HEIGHT)
                .build());
        profileY += ROW_HEIGHT + 4;

        this.addRenderableWidget(Button.builder(
                        Component.literal("Save Custom"),
                        button -> {
                            ProfileManager.saveCustom(moduleManager);
                            NotificationManager.push("Saved Custom profile");
                            rebuildUI();
                        })
                .bounds(profileX, profileY, PROFILE_WIDTH, ROW_HEIGHT)
                .build());

        String q = searchText.trim().toLowerCase(Locale.ROOT);
        List<Module> modules = moduleManager.getModules().stream()
                .filter(m -> q.isEmpty() ? m.getCategory() == selectedCategory : true)
                .filter(m -> q.isEmpty() || m.getName().toLowerCase(Locale.ROOT).contains(q))
                .collect(Collectors.toList());

        updateMaxScroll(modules);

        int rowY = LIST_TOP - scrollOffset;
        for (Module module : modules) {
            if (rowY + ROW_HEIGHT < LIST_TOP - 2 || rowY > this.height - 12) {
                rowY += ROW_HEIGHT + ROW_SPACING;
                continue;
            }

            int left = this.width / 2 - 95;

            this.addRenderableWidget(Button.builder(
                            Component.literal(module.getName() + ": " + getDisplayedState(module)),
                            button -> {
                                module.toggle();
                                rebuildUI();
                            })
                    .bounds(left, rowY, BUTTON_WIDTH, ROW_HEIGHT)
                    .build());

            if (module.hasConfigurableSettings()) {
                this.addRenderableWidget(Button.builder(
                                Component.empty(),
                                button -> this.minecraft.setScreen(module.createSettingsScreen(this)))
                        .bounds(left + BUTTON_WIDTH + 6, rowY, CFG_WIDTH, ROW_HEIGHT)
                        .build());
            }

            rowY += ROW_HEIGHT + ROW_SPACING;
        }

        rebuildingSearch = false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, LIST_TOP, 0xFF202020);
        graphics.fill(0, LIST_TOP, this.width, this.height, 0x90101010);

        // Header Branding
        graphics.drawString(this.font, "PhantomMod", 4, 10, 0xFFA8E6A3);
        graphics.drawString(this.font, "Profiles", 8, LIST_TOP + 6, 0xFFFFFFFF);

        super.render(graphics, mouseX, mouseY, delta);

        int rowY = LIST_TOP - scrollOffset;
        String q = searchText.trim().toLowerCase(Locale.ROOT);
        List<Module> modules = moduleManager.getModules().stream()
                .filter(m -> q.isEmpty() ? m.getCategory() == selectedCategory : true)
                .filter(m -> q.isEmpty() || m.getName().toLowerCase(Locale.ROOT).contains(q))
                .collect(Collectors.toList());

        for (Module module : modules) {
            if (rowY + ROW_HEIGHT < LIST_TOP - 2 || rowY > this.height - 12) {
                rowY += ROW_HEIGHT + ROW_SPACING;
                continue;
            }

            int left = this.width / 2 - 95;
            if (module.hasConfigurableSettings()) {
                int btnX = left + BUTTON_WIDTH + 6;
                int btnY = rowY;
                int textW = this.font.width("≡");
                graphics.drawString(this.font, "≡", btnX + CFG_WIDTH / 2 - textW / 2, btnY + ROW_HEIGHT / 2 - 4, 0xFFFFFFFF, false);
            }

            rowY += ROW_HEIGHT + ROW_SPACING;
        }

        NotificationManager.render(graphics);
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
        rebuildUI();
        return true;
    }

    private void updateMaxScroll(List<Module> modules) {
        int contentHeight = modules.size() * (ROW_HEIGHT + ROW_SPACING);
        int visibleHeight = this.height - LIST_TOP - 20;
        maxScroll = Math.max(0, contentHeight - visibleHeight);
        scrollOffset = clamp(scrollOffset, 0, maxScroll);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String getDisplayedState(Module module) {
        return module.isEnabled() ? "ON" : "OFF";
    }
}
