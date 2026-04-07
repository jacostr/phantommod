package com.phantom.gui;

import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import com.phantom.module.ModuleManager;
import com.phantom.module.impl.render.FullBright;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.List;

public class ClickGUIScreen extends Screen {
    private static final int BUTTON_WIDTH = 150;
    private static final int CFG_WIDTH = 34;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_SPACING = 6;

    private final ModuleManager moduleManager;
    private int scrollOffset;
    private int maxScroll;

    public ClickGUIScreen(ModuleManager moduleManager) {
        super(Component.literal("Phantom Mod"));
        this.moduleManager = moduleManager;
    }

    @Override
    protected void init() {
        rebuildModuleButtons();
    }

    private void rebuildModuleButtons() {
        this.clearWidgets();
        updateMaxScroll();

        int rowY = 42 - scrollOffset;
        List<Module> modules = moduleManager.getModules();
        ModuleCategory currentCategory = null;
        for (Module module : modules) {
            if (module.getCategory() != currentCategory) {
                currentCategory = module.getCategory();
                rowY += 12;
            }

            int left = this.width / 2 - 95;

            this.addRenderableWidget(Button.builder(
                    Component.literal(module.getName() + ": " + getDisplayedState(module)),
                    button -> {
                        module.toggle();
                        rebuildModuleButtons();
                    })
                    .bounds(left, rowY, BUTTON_WIDTH, ROW_HEIGHT)
                    .build());

            if (module.hasSettings()) {
                this.addRenderableWidget(Button.builder(
                        Component.literal(module.getSettingsButtonLabel()),
                        button -> this.minecraft.setScreen(module.createSettingsScreen(this)))
                        .bounds(left + BUTTON_WIDTH + 6, rowY, CFG_WIDTH, ROW_HEIGHT).build());
            }

            rowY += ROW_HEIGHT + ROW_SPACING;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, 0x90101010);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 18, 0xFFFFFFFF);

        int rowY = 42 - scrollOffset;
        int left = this.width / 2 - 95;
        ModuleCategory currentCategory = null;
        for (Module module : moduleManager.getModules()) {
            if (module.getCategory() != currentCategory) {
                currentCategory = module.getCategory();
                rowY += 12;
                graphics.drawString(this.font, currentCategory.getLabel(), left, rowY - 10, 0xFFA8E6A3, true);
            }

            rowY += ROW_HEIGHT + ROW_SPACING;
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
        rebuildModuleButtons();
        return true;
    }

    private void updateMaxScroll() {
        int contentHeight = 0;
        ModuleCategory currentCategory = null;
        for (Module module : moduleManager.getModules()) {
            if (module.getCategory() != currentCategory) {
                currentCategory = module.getCategory();
                contentHeight += 12;
            }
            contentHeight += ROW_HEIGHT + ROW_SPACING;
        }

        int visibleHeight = Math.max(40, this.height - 56);
        maxScroll = Math.max(0, contentHeight - visibleHeight);
        scrollOffset = clamp(scrollOffset, 0, maxScroll);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String getDisplayedState(Module module) {
        if (module instanceof FullBright fullBright) {
            return fullBright.isActuallyActive() ? "ON" : "OFF";
        }

        return module.isEnabled() ? "ON" : "OFF";
    }
}
