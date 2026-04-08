/*
 * ModuleSettingsScreen.java — Per-module settings panel.
 *
 * Opened via the ≡ button next to any module in the ClickGUI. Dynamically renders
 * the module's description, "how to use" text, a hotkey binding row, and module-
 * specific widgets (sliders for Reach/Velocity/AimAssist/Criticals, toggle
 * buttons for ESP entity filters, preset buttons for AutoBlock/Velocity/Reach).
 *
 * Widget layout uses instanceof pattern matching to detect which module type is
 * open and render the appropriate controls — simpler than an abstract factory
 * pattern for a project of this size.
 */
package com.phantom.gui;

import com.phantom.module.Module;

import com.phantom.module.impl.movement.SpeedBridge;
import com.phantom.module.impl.render.ESP;
import com.phantom.module.impl.render.HudModule;
import com.phantom.gui.widget.PhantomSlider;
import com.phantom.module.impl.combat.AimAssist;
import com.phantom.module.impl.combat.AutoBlock;
import com.phantom.module.impl.combat.Criticals;
import com.phantom.module.impl.combat.Reach;
import com.phantom.module.impl.combat.Velocity;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

public class ModuleSettingsScreen extends Screen {
    private static final int PANEL_WIDTH = 260;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_SPACING = 4;
    private static final int TEXT_SPACING = 10;
    private static final int DESCRIPTION_COLOR = 0xFFD9D9D9;
    private static final int DETECTABILITY_COLOR = 0xFFE6C278;

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

        y += 10;
        List<FormattedCharSequence> usageLines = this.font.split(Component.literal(module.getUsageGuide()),
                PANEL_WIDTH - 20);
        y += usageLines.size() * 9;

        if (!module.getUsageGuide().equals(module.getDescription())) {
            y += 4;
            List<FormattedCharSequence> descriptionLines = this.font.split(Component.literal(module.getDescription()),
                    PANEL_WIDTH - 20);
            y += descriptionLines.size() * 9;
        }

        if (module instanceof ESP esp) {
            y += 26;
            addFilterRow(centerX, y, esp::isPlayersEnabled, esp::setPlayersEnabled, "Players");
            y += ROW_HEIGHT + ROW_SPACING;
            addFilterRow(centerX, y, esp::isMobsEnabled, esp::setMobsEnabled, "Mobs");
            y += ROW_HEIGHT + ROW_SPACING;
            addFilterRow(centerX, y, esp::isAnimalsEnabled, esp::setAnimalsEnabled, "Animals");
            y += ROW_HEIGHT + ROW_SPACING;
        } else {
            y += 14;
        }

        if (module instanceof Reach reach) {
            this.addRenderableWidget(Button.builder(
                            Component.literal("Legit"),
                            button -> { reach.applyPresetLegit(); init(); }).bounds(centerX - 120, y, 58, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                            Component.literal("Normal"),
                            button -> { reach.applyPresetNormal(); init(); }).bounds(centerX - 58, y, 58, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                            Component.literal("Obvious"),
                            button -> { reach.applyPresetObvious(); init(); }).bounds(centerX + 4, y, 58, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                            Component.literal("Blatant"),
                            button -> { reach.applyPresetBlatant(); init(); }).bounds(centerX + 66, y, 58, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Entity reach", 3.0, 8.0, reach.getEntityReach(), val -> reach.setEntityReach(val)));
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Block reach", 4.5, 10.0, reach.getBlockReach(), val -> reach.setBlockReach(val)));
            y += ROW_HEIGHT + ROW_SPACING;
        }

        if (module instanceof Velocity velocity) {
            this.addRenderableWidget(Button.builder(
                            Component.literal("Legit"),
                            button -> { velocity.applyPresetLegit(); init(); }).bounds(centerX - 120, y, 58, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                            Component.literal("Subtle"),
                            button -> { velocity.applyPresetSubtle(); init(); }).bounds(centerX - 58, y, 58, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                            Component.literal("Blatant"),
                            button -> { velocity.applyPresetBlatant(); init(); }).bounds(centerX + 4, y, 58, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                            Component.literal("None"),
                            button -> { velocity.applyPresetNone(); init(); }).bounds(centerX + 66, y, 58, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Knockback %", 0.0, 1.0, velocity.getKbPercent(), val -> velocity.setKbPercent(val)));
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

            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Strength", 0, 100, autoBlock.getStrength(), val -> autoBlock.setStrength((int)val)));
            y += ROW_HEIGHT + ROW_SPACING;
        }

        if (module instanceof Criticals crit) {
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Crit Chance", 0.0, 1.0, crit.getChance(), val -> crit.setChance(val)));
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(Button.builder(
                            Component.literal("Legit (30%)"),
                            button -> { crit.setChance(0.30); init(); })
                    .bounds(centerX - 122, y, 118, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                            Component.literal("Normal (60%)"),
                            button -> { crit.setChance(0.60); init(); })
                    .bounds(centerX + 4, y, 118, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(Button.builder(
                            Component.literal("Obvious (85%)"),
                            button -> { crit.setChance(0.85); init(); })
                    .bounds(centerX - 122, y, 118, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                            Component.literal("Blatant (100%)"),
                            button -> { crit.setChance(1.0); init(); })
                    .bounds(centerX + 4, y, 118, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
        }

        if (module instanceof AimAssist aim) {
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Smoothing", 1.0, 10.0, aim.getSmoothing(), val -> aim.setSmoothing(val)));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "FOV Limit", 10.0, 360.0, aim.getFov(), val -> aim.setFov(val)));
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(Button.builder(
                            Component.literal("Legit"),
                            button -> { aim.setSmoothing(8.0); aim.setFov(60.0); init(); })
                    .bounds(centerX - 122, y, 118, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                            Component.literal("Normal"),
                            button -> { aim.setSmoothing(5.0); aim.setFov(90.0); init(); })
                    .bounds(centerX + 4, y, 118, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(Button.builder(
                            Component.literal("Obvious"),
                            button -> { aim.setSmoothing(3.0); aim.setFov(180.0); init(); })
                    .bounds(centerX - 122, y, 118, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                            Component.literal("Blatant"),
                            button -> { aim.setSmoothing(1.0); aim.setFov(360.0); init(); })
                    .bounds(centerX + 4, y, 118, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
        }

        if (module instanceof HudModule hudModule) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("Show Active Features: " + onOff(hudModule.isShowModuleList())),
                    button -> {
                        hudModule.setShowModuleList(!hudModule.isShowModuleList());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(Button.builder(
                    Component.literal("Show FPS: " + onOff(hudModule.isShowFps())),
                    button -> {
                        hudModule.setShowFps(!hudModule.isShowFps());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(Button.builder(
                    Component.literal("HUD Side: " + (hudModule.isAlignLeft() ? "Left" : "Right")),
                    button -> {
                        hudModule.setAlignLeft(!hudModule.isAlignLeft());
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

        if (module instanceof SpeedBridge sb) {
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Auto-off (s)", 0.5, 10.0, sb.getAutoOffDelay(), val -> sb.setAutoOffDelay(val)));
            y += ROW_HEIGHT + ROW_SPACING;
        }

        String keyRowLabel = "Toggle hotkey: ";
        this.addRenderableWidget(Button.builder(
                Component.literal(keyRowLabel + getKeyName(module.getKey())),
                button -> {
                }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
        y += ROW_HEIGHT + ROW_SPACING;

        this.addRenderableWidget(Button.builder(
                Component.literal(listeningForKey ? "Press a key..." : "Set hotkey"),
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
        drawTextInfo(graphics);
        if (listeningForKey) {
            String hint = "Press any key to bind toggle. ESC clears.";
            graphics.drawCenteredString(this.font, Component.literal(hint),
                    this.width / 2, this.height - 64, 0xFFE6C278);
        }
        graphics.drawCenteredString(this.font, Component.literal("ESC to go back"),
                this.width / 2, this.height - 24, 0xFFAAAAAA);

        super.render(graphics, mouseX, mouseY, delta);
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
        int contentHeight = getUsageHeight() + getDescriptionHeight() + TEXT_SPACING + 4;

        if (module instanceof ESP) {
            contentHeight += 3 * (ROW_HEIGHT + ROW_SPACING) + 15;
        }

        if (module instanceof Reach) {
            contentHeight += 3 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof Velocity) {
            contentHeight += 2 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof AutoBlock) {
            contentHeight += 3 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof Criticals) {
            contentHeight += 3 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof AimAssist) {
            contentHeight += 4 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof HudModule) {
            contentHeight += 4 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof SpeedBridge) {
            contentHeight += ROW_HEIGHT + ROW_SPACING;
        }

        contentHeight += 3 * (ROW_HEIGHT + ROW_SPACING);

        int visibleHeight = Math.max(40, this.height - 104);
        maxScroll = Math.max(0, contentHeight - visibleHeight);
        scrollOffset = clamp(scrollOffset, 0, maxScroll);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void drawTextInfo(GuiGraphics graphics) {
        int left = this.width / 2 - PANEL_WIDTH / 2 + 10;
        int y = 54 - scrollOffset;

        String usage = module.getUsageGuide();
        String desc = module.getDescription();

        graphics.drawString(this.font, "How to use:", left, y, 0xFFA8E6A3);
        y += 10;

        for (String rawLine : usage.split("\n")) {
            int color = isDetectabilityLine(rawLine) ? DETECTABILITY_COLOR : 0xFFFFFFFF;
            for (FormattedCharSequence subLine : this.font.split(Component.literal(rawLine), PANEL_WIDTH - 20)) {
                graphics.drawString(this.font, subLine, left, y, color);
                y += 9;
            }
        }

        if (!usage.equals(desc)) {
            y += 4;
            for (String rawLine : desc.split("\n")) {
                for (FormattedCharSequence subLine : this.font.split(Component.literal(rawLine), PANEL_WIDTH - 20)) {
                    int color = isDetectabilityLine(rawLine) ? DETECTABILITY_COLOR : DESCRIPTION_COLOR;
                    graphics.drawString(this.font, subLine, left, y, color);
                    y += 9;
                }
            }
        }

        if (module instanceof ESP) {
            y += 6;
            graphics.drawString(this.font, "Wall visibility can vary by renderer", left, y, 0xFFFF5555);
        }
    }

    private int getUsageHeight() {
        return (this.font.split(Component.literal(module.getUsageGuide()), PANEL_WIDTH - 20).size() * 9) + 12;
    }

    private int getDescriptionHeight() {
        if (module.getDescription().equals(module.getUsageGuide())) return 0;
        
        int height = 8;
        for (String line : module.getDescription().split("\n")) {
            if (line.isEmpty()) continue;
            height += this.font.split(Component.literal(line), PANEL_WIDTH - 20).size() * 9;
        }
        return height;
    }

    private boolean isDetectabilityLine(String line) {
        return line.toLowerCase(Locale.ROOT).startsWith("detectability:");
    }

    private String getKeyName(int key) {
        if (key == -1) {
            return "NONE";
        }

        return InputConstants.getKey(new KeyEvent(key, 0, 0)).getDisplayName().getString().toUpperCase();
    }
}
