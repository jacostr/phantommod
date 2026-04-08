package com.phantom.gui;

import com.phantom.module.Module;
import com.phantom.module.impl.movement.Scaffold;
import com.phantom.module.impl.movement.SpeedBridge;
import com.phantom.module.impl.render.ESP;
import com.phantom.module.impl.render.Hitboxes;
import com.phantom.module.impl.render.HudModule;
import com.phantom.module.impl.render.Zoom;
import com.phantom.module.impl.combat.AutoBlock;
import com.phantom.module.impl.combat.Reach;
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

        List<FormattedCharSequence> descriptionLines = this.font.split(Component.literal(module.getDescription()),
                PANEL_WIDTH - 20);
        y += descriptionLines.size() * 9 + TEXT_SPACING;

        if (module instanceof ESP esp) {
            addFilterRow(centerX, y, esp::isPlayersEnabled, esp::setPlayersEnabled, "Players");
            y += ROW_HEIGHT + ROW_SPACING;
            addFilterRow(centerX, y, esp::isMobsEnabled, esp::setMobsEnabled, "Mobs");
            y += ROW_HEIGHT + ROW_SPACING;
            addFilterRow(centerX, y, esp::isAnimalsEnabled, esp::setAnimalsEnabled, "Animals");
            y += ROW_HEIGHT + ROW_SPACING;
        }

        if (module instanceof Hitboxes hb) {
            addFilterRow(centerX, y, hb::isPlayersEnabled, hb::setPlayersEnabled, "Players");
            y += ROW_HEIGHT + ROW_SPACING;
            addFilterRow(centerX, y, hb::isMobsEnabled, hb::setMobsEnabled, "Mobs");
            y += ROW_HEIGHT + ROW_SPACING;
            addFilterRow(centerX, y, hb::isAnimalsEnabled, hb::setAnimalsEnabled, "Animals");
            y += ROW_HEIGHT + ROW_SPACING;
        }

        if (module instanceof Reach reach) {
            this.addRenderableWidget(Button.builder(
                            Component.literal("Preset: Legit (~subtle)"),
                            button -> {
                                reach.applyPresetLegit();
                                init();
                            }).bounds(centerX - 120, y, 118, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                            Component.literal("Obvious"),
                            button -> {
                                reach.applyPresetObvious();
                                init();
                            }).bounds(centerX + 4, y, 118, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(Button.builder(
                            Component.literal("Preset: Blatant (very long)"),
                            button -> {
                                reach.applyPresetBlatant();
                                init();
                            }).bounds(centerX - 120, y, 240, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(Button.builder(
                            Component.literal("Entity reach: " + String.format("%.1f", reach.getEntityReach())),
                            button -> {
                                double next = reach.getEntityReach() + 0.5;
                                if (next > 8.0) next = 3.0;
                                reach.setEntityReach(next);
                                init();
                            }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(Button.builder(
                            Component.literal("Block reach: " + String.format("%.1f", reach.getBlockReach())),
                            button -> {
                                double next = reach.getBlockReach() + 0.5;
                                if (next > 10.0) next = 4.5;
                                reach.setBlockReach(next);
                                init();
                            }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
        }

        if (module instanceof AutoBlock autoBlock) {
            this.addRenderableWidget(Button.builder(
                            Component.literal("Strength preset: Legit"),
                            button -> {
                                autoBlock.applyPresetLegit();
                                init();
                            }).bounds(centerX - 120, y, 118, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                            Component.literal("Normal"),
                            button -> {
                                autoBlock.applyPresetNormal();
                                init();
                            }).bounds(centerX + 4, y, 118, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(Button.builder(
                            Component.literal("Strength preset: Obvious"),
                            button -> {
                                autoBlock.applyPresetObvious();
                                init();
                            }).bounds(centerX - 120, y, 240, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(Button.builder(
                            Component.literal("Block strength: " + autoBlock.getStrength() + " (0–100)"),
                            button -> {
                                int next = autoBlock.getStrength() + 5;
                                if (next > 100) next = 0;
                                autoBlock.setStrength(next);
                                init();
                            }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
        }

        if (module instanceof Scaffold scaffold) {
            this.addRenderableWidget(Button.builder(
                            Component.literal("Preset: " + scaffold.getPreset().getLabel()),
                            button -> {
                                scaffold.cyclePreset();
                                init();
                            }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            if (scaffold.getPreset() == Scaffold.Preset.STANDARD) {
                this.addRenderableWidget(Button.builder(
                                Component.literal("Tower: " + onOff(scaffold.isTower())),
                                button -> {
                                    scaffold.setTower(!scaffold.isTower());
                                    init();
                                }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
                y += ROW_HEIGHT + ROW_SPACING;
            }
        }

        if (module instanceof Zoom zoom) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("Zoom Level: " + String.format("%.1f", zoom.getZoomLevel())),
                    button -> {
                        double next = zoom.getZoomLevel() + 1.0;
                        if (next > 10.0) next = 2.0;
                        zoom.setZoomLevel(next);
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

        String keyRowLabel = module instanceof Zoom ? "Hold key: " : "Toggle hotkey: ";
        this.addRenderableWidget(Button.builder(
                Component.literal(keyRowLabel + getKeyName(module.getKey())),
                button -> {
                }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
        y += ROW_HEIGHT + ROW_SPACING;

        this.addRenderableWidget(Button.builder(
                Component.literal(listeningForKey ? "Press a key..." : (module instanceof Zoom ? "Set hold key" : "Set hotkey")),
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

    private void addFilterRow(int centerX, int y, java.util.function.Supplier<Boolean> get,
            java.util.function.Consumer<Boolean> set, String label) {
        this.addRenderableWidget(Button.builder(
                        Component.literal(label + ": " + onOff(get.get())),
                        button -> {
                            set.accept(!get.get());
                            init();
                        }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, 0x90101010);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 22, 0xFFFFFFFF);
        drawDescription(graphics);
        if (listeningForKey) {
            String hint = module instanceof Zoom
                    ? "Press a key to use while held for zoom. ESC clears."
                    : "Press any key to bind toggle. ESC clears.";
            graphics.drawCenteredString(this.font, Component.literal(hint),
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

        if (module instanceof ESP || module instanceof Hitboxes) {
            contentHeight += 3 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof Reach) {
            contentHeight += 5 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof AutoBlock) {
            contentHeight += 4 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof Scaffold scaffold) {
            contentHeight += ROW_HEIGHT + ROW_SPACING;
            if (scaffold.getPreset() == Scaffold.Preset.STANDARD) {
                contentHeight += ROW_HEIGHT + ROW_SPACING;
            }
        }

        if (module instanceof Zoom) {
            contentHeight += (ROW_HEIGHT + ROW_SPACING);
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
