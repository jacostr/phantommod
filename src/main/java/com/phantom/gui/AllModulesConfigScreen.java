package com.phantom.gui;

import com.phantom.module.Module;
import com.phantom.module.ModuleManager;
import com.phantom.module.impl.render.Zoom;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * One place to read how each module works and set its hotkey (opens CFG for sliders).
 */
public class AllModulesConfigScreen extends Screen {
    private static final int ROW = 92;
    private static final int LIST_TOP = 44;
    private static final int BTN_W = 120;
    private static final int BTN_H = 18;

    private final Screen parent;
    private final ModuleManager moduleManager;
    private int scrollOffset;
    private int maxScroll;
    private Module listeningModule;

    public AllModulesConfigScreen(Screen parent, ModuleManager moduleManager) {
        super(Component.literal("Phantom — all modules"));
        this.parent = parent;
        this.moduleManager = moduleManager;
    }

    @Override
    protected void init() {
        clearWidgets();
        List<Module> modules = moduleManager.getModules();
        int content = modules.size() * ROW;
        int visible = Math.max(40, this.height - LIST_TOP - 36);
        maxScroll = Math.max(0, content - visible);
        scrollOffset = clamp(scrollOffset, 0, maxScroll);

        addRenderableWidget(Button.builder(Component.literal("Back"), b -> minecraft.setScreen(parent))
                .bounds(this.width / 2 - 50, this.height - 28, 100, 20)
                .build());

        int baseY = LIST_TOP - scrollOffset;
        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            int rowY = baseY + i * ROW;
            if (rowY + ROW < LIST_TOP - 4 || rowY > this.height - 32) {
                continue;
            }
            int left = 24;
            int bx = this.width - 24 - BTN_W;
            if (m.hasConfigurableSettings()) {
                bx -= BTN_W + 6;
            }

            final Module mod = m;
            addRenderableWidget(Button.builder(
                            Component.literal(listeningModule == mod ? "Press a key…" : keyButtonLabel(mod)),
                            b -> listeningModule = mod)
                    .bounds(bx, rowY + 56, BTN_W, BTN_H)
                    .build());

            if (m.hasConfigurableSettings()) {
                addRenderableWidget(Button.builder(Component.literal("CFG"),
                                b -> minecraft.setScreen(mod.createSettingsScreen(this)))
                        .bounds(this.width - 24 - BTN_W, rowY + 56, BTN_W, BTN_H)
                        .build());
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, 0xC0101010);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
        graphics.drawCenteredString(this.font,
                Component.literal("How to use each feature and hotkeys. CFG opens sliders and toggles."),
                this.width / 2, 26, 0xFFAAAAAA);

        List<Module> modules = moduleManager.getModules();
        int baseY = LIST_TOP - scrollOffset;
        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            int rowY = baseY + i * ROW;
            if (rowY + ROW < LIST_TOP - 4 || rowY > this.height - 28) {
                continue;
            }
            graphics.drawString(this.font,
                    Component.literal(m.getName() + " — " + m.getCategory().getLabel()),
                    24, rowY + 2, 0xFFA8E6A3, false);

            int y = rowY + 14;
            for (var line : this.font.split(Component.literal(m.getUsageGuide()), this.width - 48)) {
                if (y > rowY + 52) {
                    break;
                }
                graphics.drawString(this.font, line, 24, y, 0xFFD0D0D0, false);
                y += 9;
            }
        }

        if (listeningModule != null) {
            graphics.drawCenteredString(this.font,
                    Component.literal("Press a key to bind. ESC clears. Zoom uses hold-to-use."),
                    this.width / 2, this.height - 44, 0xFFE6C278);
        }

        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (listeningModule != null) {
            listeningModule.setKey(event.key() == GLFW.GLFW_KEY_ESCAPE ? -1 : event.key());
            listeningModule = null;
            init();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            minecraft.setScreen(parent);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (maxScroll <= 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        int next = clamp(scrollOffset - (int) (scrollY * 18.0D), 0, maxScroll);
        if (next != scrollOffset) {
            scrollOffset = next;
            init();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private String keyButtonLabel(Module module) {
        String mode = module instanceof Zoom ? "Hold: " : "Toggle: ";
        return mode + getKeyName(module.getKey());
    }

    private String getKeyName(int key) {
        if (key == -1) {
            return "NONE";
        }
        return InputConstants.getKey(new KeyEvent(key, 0, 0)).getDisplayName().getString().toUpperCase();
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
