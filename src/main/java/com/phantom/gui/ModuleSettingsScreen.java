/*
 * ModuleSettingsScreen.java — Per-module settings panel.
 *
 * Opened via the ≡ button next to any module in the ClickGUI. Dynamically renders
 * the module's description, "how to use" text, a hotkey binding row, and module-
 * specific widgets (sliders for Reach/Velocity/AimAssist/Criticals, toggle
 * buttons for ESP entity filters, preset buttons for Velocity/Reach).
 *
 * Widget layout uses instanceof pattern matching to detect which module type is
 * open and render the appropriate controls — simpler than an abstract factory
 * pattern for a project of this size.
 */
package com.phantom.gui;

import com.phantom.module.Module;

import com.phantom.module.impl.player.AntiAFK;
import com.phantom.module.impl.player.AutoTotem;
import com.phantom.module.impl.player.AutoTools;
import com.phantom.module.impl.player.AutoXPThrow;
import com.phantom.module.impl.player.FastPlace;

import com.phantom.module.impl.movement.Scaffold;
import com.phantom.module.impl.movement.SpeedBridge;
import com.phantom.module.impl.render.HealthBar;
import com.phantom.module.impl.render.Indicators;
import com.phantom.module.impl.render.ESP;
import com.phantom.module.impl.render.HudModule;
import com.phantom.module.impl.smp.BedESP;
import com.phantom.module.impl.smp.ChestESP;
import com.phantom.module.impl.smp.OreESP;
import com.phantom.module.impl.smp.ShulkerESP;
import com.phantom.gui.widget.PhantomSlider;
import com.phantom.module.impl.combat.AimAssist;
import com.phantom.module.impl.combat.AutoClicker;
import com.phantom.module.impl.combat.BlockHit;
import com.phantom.module.impl.combat.Criticals;
import com.phantom.module.impl.combat.HitSelect;
import com.phantom.module.impl.combat.JumpReset;
import com.phantom.module.impl.combat.NoHitDelay;
import com.phantom.module.impl.combat.Reach;
import com.phantom.module.impl.combat.RightClicker;
import com.phantom.module.impl.combat.SilentAura;
import com.phantom.module.impl.combat.Triggerbot;
import com.phantom.module.impl.combat.Velocity;
import com.phantom.module.impl.combat.WTap;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
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

            this.addRenderableWidget(net.minecraft.client.gui.components.CycleButton.builder(
                    (Boolean val) -> net.minecraft.network.chat.Component.literal(val ? "ON" : "OFF"),
                    esp.isThroughWalls())
                    .withValues(true, false)
                    .create(centerX - 80, y, 160, ROW_HEIGHT,
                            net.minecraft.network.chat.Component.literal("Through Walls"),
                            (btn, val) -> esp.setThroughWalls(val)));
            y += ROW_HEIGHT + ROW_SPACING;
        } else {
            y += 14;
        }

        if (module instanceof Velocity vel) {
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "KB Percent", 0.0, 1.0,
                    vel.getKbPercent(), val -> vel.setKbPercent(val)));
            y += ROW_HEIGHT + ROW_SPACING;
            addFilterRow(centerX, y, vel::isHypixelMode, vel::setHypixelMode, "Hypixel Mode");
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(Button.builder(Component.literal("Legit (90%)"), b -> {
                vel.applyPresetLegit();
                init();
            }).bounds(centerX - 122, y, 118, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(Component.literal("Subtle (75%)"), b -> {
                vel.applyPresetSubtle();
                init();
            }).bounds(centerX + 4, y, 118, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(Button.builder(Component.literal("Blatant (40%)"), b -> {
                vel.applyPresetBlatant();
                init();
            }).bounds(centerX - 122, y, 118, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(Component.literal("None (0%)"), b -> {
                vel.applyPresetNone();
                init();
            }).bounds(centerX + 4, y, 118, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
        }

        if (module instanceof NoHitDelay nhd) {
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Chance", 0.0, 1.0,
                    nhd.getChance(), val -> nhd.setChance(val)));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Delay (ticks)", 0, 10,
                    nhd.getDelayTicks(), val -> nhd.setDelayTicks((int) val)));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(Button.builder(Component.literal("Preset: " + nhd.getPreset().getLabel()), b -> {
                nhd.cyclePreset();
                init();
            }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
        }

        if (module instanceof SilentAura sa) {
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Min CPS", 1.0, 20.0,
                    sa.getMinCps(), val -> sa.setMinCps(val)));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Max CPS", 1.0, 20.0,
                    sa.getMaxCps(), val -> sa.setMaxCps(val)));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Smoothing", 1.0, 10.0,
                    sa.getSmoothing(), val -> sa.setSmoothing(val)));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Snapiness", 1.0, 10.0,
                    sa.getSnapiness(), val -> sa.setSnapiness(val)));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Attack Range", 2.0, 6.0,
                    sa.getAttackRange(), val -> sa.setAttackRange(val)));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Max Angle", 10.0, 360.0,
                    sa.getMaxAngle(), val -> sa.setMaxAngle(val)));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(
                    Button.builder(Component.literal("Target: " + sa.getTargetMode().getLabel()), b -> {
                        sa.cycleTargetMode();
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
            addFilterRow(centerX, y, sa::isAimVertically, sa::setAimVertically, "Aim Vertically");
            y += ROW_HEIGHT + ROW_SPACING;
            addFilterRow(centerX, y, sa::isRequireMouseDown, sa::setRequireMouseDown, "Require Mouse");
            y += ROW_HEIGHT + ROW_SPACING;
            addFilterRow(centerX, y, sa::isBreakBlocksPause, sa::setBreakBlocksPause, "Pause on Break");
            y += ROW_HEIGHT + ROW_SPACING;
            addFilterRow(centerX, y, sa::isLimitToWeapons, sa::setLimitToWeapons, "Weapons Only");
            y += ROW_HEIGHT + ROW_SPACING;
            addFilterRow(centerX, y, sa::isTargetPlayers, sa::setTargetPlayers, "Target Players");
            y += ROW_HEIGHT + ROW_SPACING;
            addFilterRow(centerX, y, sa::isTargetMobs, sa::setTargetMobs, "Target Mobs");
            y += ROW_HEIGHT + ROW_SPACING;
            addFilterRow(centerX, y, sa::isTargetAnimals, sa::setTargetAnimals, "Target Animals");
            y += ROW_HEIGHT + ROW_SPACING;
        }

        if (module instanceof AutoTotem at) {
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Health Threshold", 0, 20,
                    at.getHealthThreshold(), val -> at.setHealthThreshold(val)));
            y += ROW_HEIGHT + ROW_SPACING;
            addFilterRow(centerX, y, at::isAlwaysEquip, at::setAlwaysEquip, "Always Equip");
            y += ROW_HEIGHT + ROW_SPACING;
        }

        if (module instanceof HealthBar hb) {
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Offset X", -200, 200,
                    hb.getOffsetX(), val -> hb.setOffsetX((int) val)));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Offset Y", -200, 200,
                    hb.getOffsetY(), val -> hb.setOffsetY((int) val)));
            y += ROW_HEIGHT + ROW_SPACING;
            addFilterRow(centerX, y, hb::isShowAbsorption, hb::setShowAbsorption, "Show Absorption");
            y += ROW_HEIGHT + ROW_SPACING;
            addFilterRow(centerX, y, hb::isShowSelf, hb::setShowSelf, "Show Self");
            y += ROW_HEIGHT + ROW_SPACING;
            addFilterRow(centerX, y, hb::isOpponentHealthTags, hb::setOpponentHealthTags, "Above Heads");
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(Button.builder(Component.literal("Color: " + hb.getColorMode().getLabel()), b -> {
                hb.cycleColorMode();
                init();
            }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
        }

        if (module instanceof AutoTools at) {
            this.addRenderableWidget(
                    Button.builder(Component.literal("Priority: " + at.getAttackPriority().getLabel()), b -> {
                        at.cycleAttackPriority();
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
        }

        if (module instanceof ChestESP ce) {
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Range", 8.0, 128.0,
                    ce.getRange(), val -> ce.setRange(val)));
            y += ROW_HEIGHT + ROW_SPACING;
            addFilterRow(centerX, y, ce::isChestsEnabled, ce::setChestsEnabled, "Chests");
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(
                    Button.builder(Component.literal("Chest Color: " + ce.getChestsColor().getLabel()), b -> {
                        ce.cycleChestsColor();
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
            addFilterRow(centerX, y, ce::isEnderChestsEnabled, ce::setEnderChestsEnabled, "Ender Chests");
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(
                    Button.builder(Component.literal("Ender Color: " + ce.getEnderChestsColor().getLabel()), b -> {
                        ce.cycleEnderChestsColor();
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(
                    Button.builder(Component.literal("Trapped Color: " + ce.getTrappedChestsColor().getLabel()), b -> {
                        ce.cycleTrappedChestsColor();
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
        }

        if (module instanceof OreESP oe) {
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Range", 8.0, 128.0,
                    oe.getRange(), val -> oe.setRange(val)));
            y += ROW_HEIGHT + ROW_SPACING;

            addFilterRow(centerX, y, oe::isDiamondEnabled, oe::setDiamondEnabled, "Diamond Ore");
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(
                    Button.builder(Component.literal("Diamond Color: " + oe.getDiamondColor().getLabel()), b -> {
                        oe.cycleDiamondColor();
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            addFilterRow(centerX, y, oe::isGoldEnabled, oe::setGoldEnabled, "Gold Ore");
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(
                    Button.builder(Component.literal("Gold Color: " + oe.getGoldColor().getLabel()), b -> {
                        oe.cycleGoldColor();
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            addFilterRow(centerX, y, oe::isEmeraldEnabled, oe::setEmeraldEnabled, "Emerald Ore");
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(
                    Button.builder(Component.literal("Emerald Color: " + oe.getEmeraldColor().getLabel()), b -> {
                        oe.cycleEmeraldColor();
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            addFilterRow(centerX, y, oe::isRedstoneEnabled, oe::setRedstoneEnabled, "Redstone Ore");
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(
                    Button.builder(Component.literal("Redstone Color: " + oe.getRedstoneColor().getLabel()), b -> {
                        oe.cycleRedstoneColor();
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            addFilterRow(centerX, y, oe::isLapisEnabled, oe::setLapisEnabled, "Lapis Ore");
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(
                    Button.builder(Component.literal("Lapis Color: " + oe.getLapisColor().getLabel()), b -> {
                        oe.cycleLapisColor();
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            addFilterRow(centerX, y, oe::isCoalEnabled, oe::setCoalEnabled, "Coal Ore");
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(
                    Button.builder(Component.literal("Coal Color: " + oe.getCoalColor().getLabel()), b -> {
                        oe.cycleCoalColor();
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            addFilterRow(centerX, y, oe::isAncientDebrisEnabled, oe::setAncientDebrisEnabled, "Ancient Debris");
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(
                    Button.builder(Component.literal("Debris Color: " + oe.getAncientDebrisColor().getLabel()), b -> {
                        oe.cycleAncientDebrisColor();
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            addFilterRow(centerX, y, oe::isNetherGoldEnabled, oe::setNetherGoldEnabled, "Nether Gold");
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(
                    Button.builder(Component.literal("N.Gold Color: " + oe.getNetherGoldColor().getLabel()), b -> {
                        oe.cycleNetherGoldColor();
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            addFilterRow(centerX, y, oe::isQuartzEnabled, oe::setQuartzEnabled, "Nether Quartz");
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(
                    Button.builder(Component.literal("Quartz Color: " + oe.getQuartzColor().getLabel()), b -> {
                        oe.cycleQuartzColor();
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            addFilterRow(centerX, y, oe::isOnlyCaves, oe::setOnlyCaves, "Only Caves");
            y += ROW_HEIGHT + ROW_SPACING;
        }

        if (module instanceof BedESP be) {
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Range", 8.0, 128.0,
                    be.getRange(), val -> be.setRange(val)));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(Button.builder(Component.literal("Color: " + be.getColorMode().getLabel()), b -> {
                be.cycleColorMode();
                init();
            }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
        }

        if (module instanceof ShulkerESP se) {
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Range", 8.0, 128.0,
                    se.getRange(), val -> se.setRange(val)));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(Button.builder(Component.literal("Color: " + se.getColorMode().getLabel()), b -> {
                se.cycleColorMode();
                init();
            }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
        }

        // Tracers module removed

        if (module instanceof com.phantom.module.impl.smp.OreFinder of) {
            addFilterRow(centerX, y, of::isDebrisOnly, of::setDebrisOnly, "Debris Only");
            y += ROW_HEIGHT + ROW_SPACING;
            addFilterRow(centerX, y, of::isShowList, of::setShowList, "Show List");
            y += ROW_HEIGHT + ROW_SPACING;
            addFilterRow(centerX, y, of::isShowSelfCoords, of::setShowSelfCoords, "Show Self Coords");
            y += ROW_HEIGHT + ROW_SPACING;
            addFilterRow(centerX, y, of::isShowDirections, of::setShowDirections, "Show Directions");
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Range", 8.0, 128.0,
                    of.getRange(), val -> of.setRange(val)));
            y += ROW_HEIGHT + ROW_SPACING;
        }

        if (module instanceof Reach reach) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("Legit"),
                    button -> {
                        reach.applyPresetLegit();
                        init();
                    }).bounds(centerX - 120, y, 58, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                    Component.literal("Normal"),
                    button -> {
                        reach.applyPresetNormal();
                        init();
                    }).bounds(centerX - 58, y, 58, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                    Component.literal("Obvious"),
                    button -> {
                        reach.applyPresetObvious();
                        init();
                    }).bounds(centerX + 4, y, 58, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                    Component.literal("Blatant"),
                    button -> {
                        reach.applyPresetBlatant();
                        init();
                    }).bounds(centerX + 66, y, 58, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Entity reach", 3.0, 8.0,
                    reach.getEntityReach(), val -> reach.setEntityReach(val)));
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Block reach", 4.5, 10.0,
                    reach.getBlockReach(), val -> reach.setBlockReach(val)));
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(Button.builder(
                    Component.literal("Only while sprinting: " + onOff(reach.isOnlyWhileSprinting())),
                    button -> {
                        reach.setOnlyWhileSprinting(!reach.isOnlyWhileSprinting());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(Button.builder(
                    Component.literal("Moving only: " + onOff(reach.isMovingOnly())),
                    button -> {
                        reach.setMovingOnly(!reach.isMovingOnly());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(Button.builder(
                    Component.literal("Disable in water: " + onOff(reach.isDisableInWater())),
                    button -> {
                        reach.setDisableInWater(!reach.isDisableInWater());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
        }

        if (module instanceof Criticals crit) {
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Crit Chance", 0.0, 1.0,
                    crit.getChance(), val -> crit.setChance(val)));
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(Button.builder(
                    Component.literal("Legit (30%)"),
                    button -> {
                        crit.setChance(0.30);
                        init();
                    })
                    .bounds(centerX - 122, y, 118, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                    Component.literal("Normal (60%)"),
                    button -> {
                        crit.setChance(0.60);
                        init();
                    })
                    .bounds(centerX + 4, y, 118, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(Button.builder(
                    Component.literal("Obvious (85%)"),
                    button -> {
                        crit.setChance(0.85);
                        init();
                    })
                    .bounds(centerX - 122, y, 118, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                    Component.literal("Blatant (100%)"),
                    button -> {
                        crit.setChance(1.0);
                        init();
                    })
                    .bounds(centerX + 4, y, 118, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
        }

        if (module instanceof AimAssist aim) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("Legit"),
                    button -> {
                        aim.applyPresetLegit();
                        init();
                    }).bounds(centerX - 120, y, 58, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                    Component.literal("Normal"),
                    button -> {
                        aim.applyPresetNormal();
                        init();
                    }).bounds(centerX - 58, y, 58, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                    Component.literal("Obvious"),
                    button -> {
                        aim.applyPresetObvious();
                        init();
                    }).bounds(centerX + 4, y, 58, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                    Component.literal("Blatant"),
                    button -> {
                        aim.applyPresetBlatant();
                        init();
                    }).bounds(centerX + 66, y, 58, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Smoothing", 1.0, 10.0,
                    aim.getSmoothing(), val -> aim.setSmoothing(val)));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Snapiness", 1.0, 10.0,
                    aim.getSnapiness(), val -> aim.setSnapiness(val)));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "FOV Limit", 10.0, 360.0,
                    aim.getFov(), val -> aim.setFov(val)));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Distance", 2.5, 6.0,
                    aim.getDistance(), val -> aim.setDistance(val)));
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(Button.builder(
                    Component.literal("Require mouse down: " + onOff(aim.isRequireMouseDown())),
                    button -> {
                        aim.setRequireMouseDown(!aim.isRequireMouseDown());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(Button.builder(
                    Component.literal("Click aim: " + onOff(aim.isClickAim())),
                    button -> {
                        aim.setClickAim(!aim.isClickAim());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(Button.builder(
                    Component.literal("Aim vertically: " + onOff(aim.isAimVertically())),
                    button -> {
                        aim.setAimVertically(!aim.isAimVertically());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(Button.builder(
                    Component.literal("Limit to weapons: " + onOff(aim.isLimitToWeapons())),
                    button -> {
                        aim.setLimitToWeapons(!aim.isLimitToWeapons());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(Button.builder(
                    Component.literal("Visibility check: " + onOff(aim.isVisibilityCheck())),
                    button -> {
                        aim.setVisibilityCheck(!aim.isVisibilityCheck());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(Button.builder(
                    Component.literal("Target players: " + onOff(aim.isTargetPlayers())),
                    button -> {
                        aim.setTargetPlayers(!aim.isTargetPlayers());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(Button.builder(
                    Component.literal("Target mobs: " + onOff(aim.isTargetMobs())),
                    button -> {
                        aim.setTargetMobs(!aim.isTargetMobs());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(Button.builder(
                    Component.literal("Target animals: " + onOff(aim.isTargetAnimals())),
                    button -> {
                        aim.setTargetAnimals(!aim.isTargetAnimals());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(Button.builder(
                    Component.literal("Target area: " + aim.getTargetArea().getLabel()),
                    button -> {
                        aim.cycleTargetArea();
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(Button.builder(
                    Component.literal("Target mode: " + aim.getTargetMode().getLabel()),
                    button -> {
                        aim.cycleTargetMode();
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
        }

        if (module instanceof AutoClicker autoClicker) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("Legit"),
                    button -> {
                        autoClicker.applyPresetLegit();
                        init();
                    }).bounds(centerX - 120, y, 58, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                    Component.literal("Normal"),
                    button -> {
                        autoClicker.applyPresetNormal();
                        init();
                    }).bounds(centerX - 58, y, 58, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                    Component.literal("Obvious"),
                    button -> {
                        autoClicker.applyPresetObvious();
                        init();
                    }).bounds(centerX + 4, y, 58, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                    Component.literal("Blatant"),
                    button -> {
                        autoClicker.applyPresetBlatant();
                        init();
                    }).bounds(centerX + 66, y, 58, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Min CPS", 1.0, 20.0,
                    autoClicker.getMinCps(), autoClicker::setMinCps));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Max CPS", 1.0, 20.0,
                    autoClicker.getMaxCps(), autoClicker::setMaxCps));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(Button.builder(
                    Component.literal("Only with weapon: " + onOff(autoClicker.isOnlyWithWeapon())),
                    button -> {
                        autoClicker.setOnlyWithWeapon(!autoClicker.isOnlyWithWeapon());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(Button.builder(
                    Component.literal("Require mouse down: " + onOff(autoClicker.isRequireMouseDown())),
                    button -> {
                        autoClicker.setRequireMouseDown(!autoClicker.isRequireMouseDown());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(Button.builder(
                    Component.literal("Hit entities only: " + onOff(autoClicker.isHitEntitiesOnly())),
                    button -> {
                        autoClicker.setHitEntitiesOnly(!autoClicker.isHitEntitiesOnly());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(Button.builder(
                    Component.literal("Pause while breaking: " + onOff(autoClicker.isBreakBlockPause())),
                    button -> {
                        autoClicker.setBreakBlockPause(!autoClicker.isBreakBlockPause());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
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

            this.addRenderableWidget(Button.builder(
                    Component.literal("FPS/Ping Side: " + hudModule.getStatsSide().getLabel()),
                    button -> {
                        hudModule.cycleStatsSide();
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
        }

        if (module instanceof SpeedBridge sb) {
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Auto-off (s)", 0.5, 10.0,
                    sb.getAutoOffDelay(), val -> sb.setAutoOffDelay(val)));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(Button.builder(
                    Component.literal("Legit"),
                    button -> {
                        sb.applyPresetLegit();
                        init();
                    }).bounds(centerX - 120, y, 58, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                    Component.literal("Normal"),
                    button -> {
                        sb.applyPresetNormal();
                        init();
                    }).bounds(centerX - 58, y, 58, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                    Component.literal("Obvious"),
                    button -> {
                        sb.applyPresetObvious();
                        init();
                    }).bounds(centerX + 4, y, 58, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                    Component.literal("Blatant"),
                    button -> {
                        sb.applyPresetBlatant();
                        init();
                    }).bounds(centerX + 66, y, 58, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Delay ticks", 0.0, 4.0,
                    sb.getDelayTicks(), val -> sb.setDelayTicks((int) Math.round(val))));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(Button.builder(
                    Component.literal("Blocks only: " + onOff(sb.isBlocksOnly())),
                    button -> {
                        sb.setBlocksOnly(!sb.isBlocksOnly());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
        }

        if (module instanceof Scaffold scaffold) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("SafeWalk: " + onOff(scaffold.isSafeWalk())),
                    button -> {
                        scaffold.setSafeWalk(!scaffold.isSafeWalk());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(Button.builder(
                    Component.literal("Tower: " + onOff(scaffold.isTower())),
                    button -> {
                        scaffold.setTower(!scaffold.isTower());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Auto-Off Delay", 0.5, 10.0,
                    scaffold.getAutoOffDelay(), val -> scaffold.setAutoOffDelay(val)));
            y += ROW_HEIGHT + ROW_SPACING;
        }

        if (module instanceof WTap wTap) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("Legit"),
                    button -> {
                        wTap.applyPresetLegit();
                        init();
                    }).bounds(centerX - 120, y, 58, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                    Component.literal("Normal"),
                    button -> {
                        wTap.applyPresetNormal();
                        init();
                    }).bounds(centerX - 58, y, 58, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                    Component.literal("Obvious"),
                    button -> {
                        wTap.applyPresetObvious();
                        init();
                    }).bounds(centerX + 4, y, 58, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                    Component.literal("Blatant"),
                    button -> {
                        wTap.applyPresetBlatant();
                        init();
                    }).bounds(centerX + 66, y, 58, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Chance", 0.0, 1.0,
                    wTap.getChance(), wTap::setChance));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Release delay", 0.0, 250.0,
                    wTap.getReleaseDelayMs(), val -> wTap.setReleaseDelayMs((int) Math.round(val))));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Re-press delay", 0.0, 250.0,
                    wTap.getRepressDelayMs(), val -> wTap.setRepressDelayMs((int) Math.round(val))));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(Button.builder(
                    Component.literal("Select hits: " + onOff(wTap.isSelectHits())),
                    button -> {
                        wTap.setSelectHits(!wTap.isSelectHits());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
        }

        if (module instanceof com.phantom.module.impl.combat.WeaponCycler wc) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("Weapons: " + wc.getWeaponCombo().getLabel()),
                    button -> {
                        wc.cycleWeaponCombo();
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
        }

        if (module instanceof com.phantom.module.impl.combat.WaterClutch wc) {
            addFilterRow(centerX, y, wc::isClutch, wc::setClutch, "Fall Clutch");
            y += ROW_HEIGHT + ROW_SPACING;
            addFilterRow(centerX, y, wc::isExtinguish, wc::setExtinguish, "Fire Extinguish");
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Trigger Height", 1.0, 64.0,
                    wc.getTriggerHeight(), val -> wc.setTriggerHeight((int) val)));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Pickup Delay", 1.0, 20.0,
                    wc.getPickupDelay(), val -> wc.setPickupDelay((int) val)));
            y += ROW_HEIGHT + ROW_SPACING;
        }

        if (module instanceof Triggerbot triggerbot) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("Legit"),
                    button -> {
                        triggerbot.applyPresetLegit();
                        init();
                    }).bounds(centerX - 120, y, 58, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                    Component.literal("Normal"),
                    button -> {
                        triggerbot.applyPresetNormal();
                        init();
                    }).bounds(centerX - 58, y, 58, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                    Component.literal("Obvious"),
                    button -> {
                        triggerbot.applyPresetObvious();
                        init();
                    }).bounds(centerX + 4, y, 58, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                    Component.literal("Blatant"),
                    button -> {
                        triggerbot.applyPresetBlatant();
                        init();
                    }).bounds(centerX + 66, y, 58, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Extra delay", -4.0, 10.0,
                    triggerbot.getExtraDelayTicks(),
                    val -> triggerbot.setExtraDelayTicks((int) Math.round(val))));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(Button.builder(
                    Component.literal("Require mouse down: " + onOff(triggerbot.isRequireMouseDown())),
                    button -> {
                        triggerbot.setRequireMouseDown(!triggerbot.isRequireMouseDown());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(Button.builder(
                    Component.literal("Air crits: " + onOff(triggerbot.isAirCrits())),
                    button -> {
                        triggerbot.setAirCrits(!triggerbot.isAirCrits());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(Button.builder(
                    Component.literal("Shield check: " + onOff(triggerbot.isShieldCheck())),
                    button -> {
                        triggerbot.setShieldCheck(!triggerbot.isShieldCheck());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Miss chance", 0.0, 1.0,
                    triggerbot.getTargetMissChance(), triggerbot::setTargetMissChance));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Early hit chance", 0.0, 1.0,
                    triggerbot.getEarlyHitChance(), triggerbot::setEarlyHitChance));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(Button.builder(
                    Component.literal("Limit items: " + onOff(triggerbot.isLimitItems())),
                    button -> {
                        triggerbot.setLimitItems(!triggerbot.isLimitItems());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
        }

        if (module instanceof BlockHit blockHit) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("Legit"),
                    button -> {
                        blockHit.applyPresetLegit();
                        init();
                    }).bounds(centerX - 120, y, 58, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                    Component.literal("Normal"),
                    button -> {
                        blockHit.applyPresetNormal();
                        init();
                    }).bounds(centerX - 58, y, 58, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                    Component.literal("Obvious"),
                    button -> {
                        blockHit.applyPresetObvious();
                        init();
                    }).bounds(centerX + 4, y, 58, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                    Component.literal("Blatant"),
                    button -> {
                        blockHit.applyPresetBlatant();
                        init();
                    }).bounds(centerX + 66, y, 58, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Chance", 0.0, 1.0,
                    blockHit.getChance(), blockHit::setChance));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(Button.builder(
                    Component.literal("Require mouse down: " + onOff(blockHit.isRequireMouseDown())),
                    button -> {
                        blockHit.setRequireMouseDown(!blockHit.isRequireMouseDown());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(Button.builder(
                    Component.literal("Mode: " + blockHit.getMode().getLabel()),
                    button -> {
                        blockHit.cycleMode();
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(net.minecraft.client.gui.components.CycleButton.builder(
                    (Boolean val) -> net.minecraft.network.chat.Component.literal(val ? "ON" : "OFF"),
                    blockHit.isVisualAnimation())
                    .withValues(true, false)
                    .create(centerX - 80, y, 160, ROW_HEIGHT,
                            net.minecraft.network.chat.Component.literal("Visual Animation"),
                            (btn, val) -> blockHit.setVisualAnimation(val)));
            y += ROW_HEIGHT + ROW_SPACING;
        }

        if (module instanceof HitSelect hitSelect) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("Legit"),
                    button -> {
                        hitSelect.applyPresetLegit();
                        init();
                    }).bounds(centerX - 120, y, 58, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                    Component.literal("Normal"),
                    button -> {
                        hitSelect.applyPresetNormal();
                        init();
                    }).bounds(centerX - 58, y, 58, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                    Component.literal("Obvious"),
                    button -> {
                        hitSelect.applyPresetObvious();
                        init();
                    }).bounds(centerX + 4, y, 58, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                    Component.literal("Blatant"),
                    button -> {
                        hitSelect.applyPresetBlatant();
                        init();
                    }).bounds(centerX + 66, y, 58, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Chance", 0.0, 1.0,
                    hitSelect.getChance(), hitSelect::setChance));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(Button.builder(
                    Component.literal("Mode: " + hitSelect.getMode().getLabel()),
                    button -> {
                        hitSelect.cycleMode();
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(Button.builder(
                    Component.literal("Preference: " + hitSelect.getPreference().getLabel()),
                    button -> {
                        hitSelect.cyclePreference();
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
        }

        if (module instanceof RightClicker rightClicker) {
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Min CPS", 1.0, 20.0,
                    rightClicker.getMinCps(), rightClicker::setMinCps));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Max CPS", 1.0, 20.0,
                    rightClicker.getMaxCps(), rightClicker::setMaxCps));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Start delay", 0.0, 1000.0,
                    rightClicker.getStartDelayMs(),
                    val -> rightClicker.setStartDelayMs((int) Math.round(val))));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Block place delay", 0.0,
                    1000.0, rightClicker.getBlockPlaceDelayMs(),
                    val -> rightClicker.setBlockPlaceDelayMs((int) Math.round(val))));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(Button.builder(
                    Component.literal("Randomization: " + rightClicker.getRandomization().getLabel()),
                    button -> {
                        rightClicker.cycleRandomization();
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Jitter", 0.0, 3.0,
                    rightClicker.getJitter(), rightClicker::setJitter));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(Button.builder(
                    Component.literal("Use item whitelist: " + onOff(rightClicker.isUseItemWhitelist())),
                    button -> {
                        rightClicker.setUseItemWhitelist(!rightClicker.isUseItemWhitelist());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
        }

        if (module instanceof JumpReset jumpReset) {
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Hit Trigger %", 0.0, 1.0,
                    jumpReset.getChance(), jumpReset::setChance));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Jump execution %", 0.0, 100.0,
                    jumpReset.getJumpChancePercent(), jumpReset::setJumpChancePercent));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Accuracy timing", 0.0, 1.0,
                    jumpReset.getAccuracy(), jumpReset::setAccuracy));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(Button.builder(
                    Component.literal("Only when targeting: " + onOff(jumpReset.isOnlyWhenTargeting())),
                    button -> {
                        jumpReset.setOnlyWhenTargeting(!jumpReset.isOnlyWhenTargeting());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(Button.builder(
                    Component.literal("Water check: " + onOff(jumpReset.isWaterCheck())),
                    button -> {
                        jumpReset.setWaterCheck(!jumpReset.isWaterCheck());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
        }

        if (module instanceof FastPlace fastPlace) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("Legit"),
                    button -> {
                        fastPlace.applyPresetLegit();
                        init();
                    }).bounds(centerX - 120, y, 58, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                    Component.literal("Normal"),
                    button -> {
                        fastPlace.applyPresetNormal();
                        init();
                    }).bounds(centerX - 58, y, 58, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                    Component.literal("Obvious"),
                    button -> {
                        fastPlace.applyPresetObvious();
                        init();
                    }).bounds(centerX + 4, y, 58, ROW_HEIGHT).build());
            this.addRenderableWidget(Button.builder(
                    Component.literal("Blatant"),
                    button -> {
                        fastPlace.applyPresetBlatant();
                        init();
                    }).bounds(centerX + 66, y, 58, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Delay ticks", 0.0, 4.0,
                    fastPlace.getDelayTicks(), val -> fastPlace.setDelayTicks((int) Math.round(val))));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(Button.builder(
                    Component.literal("Blocks only: " + onOff(fastPlace.isBlocksOnly())),
                    button -> {
                        fastPlace.setBlocksOnly(!fastPlace.isBlocksOnly());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
        }

        if (module instanceof AutoXPThrow autoXP) {
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Throws per tick", 1.0, 20.0,
                    autoXP.getThrowsPerTick(), val -> autoXP.setThrowsPerTick((int) Math.round(val))));
            y += ROW_HEIGHT + ROW_SPACING;
        }

        if (module instanceof AntiAFK antiAFK) {
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Start delay min", 5.0, 300.0,
                    antiAFK.getMinStartDelaySeconds(), antiAFK::setMinStartDelaySeconds));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Start delay max", 5.0, 300.0,
                    antiAFK.getMaxStartDelaySeconds(), antiAFK::setMaxStartDelaySeconds));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Frequency", 0.1, 1.0,
                    antiAFK.getFrequency(), antiAFK::setFrequency));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(Button.builder(
                    Component.literal("Keep close: " + onOff(antiAFK.isKeepClose())),
                    button -> {
                        antiAFK.setKeepClose(!antiAFK.isKeepClose());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(Button.builder(
                    Component.literal("Rotation: " + onOff(antiAFK.isRotation())),
                    button -> {
                        antiAFK.setRotation(!antiAFK.isRotation());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(Button.builder(
                    Component.literal("Silent Aim: " + onOff(antiAFK.isSilentAim())),
                    button -> {
                        antiAFK.setSilentAim(!antiAFK.isSilentAim());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Max yaw change", 1.0, 90.0,
                    antiAFK.getMaxYawChange(), antiAFK::setMaxYawChange));
            y += ROW_HEIGHT + ROW_SPACING;
            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Max pitch change", 1.0, 90.0,
                    antiAFK.getMaxPitchChange(), antiAFK::setMaxPitchChange));
            y += ROW_HEIGHT + ROW_SPACING;
        }

        if (module instanceof Indicators indicators) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("Alert type: " + indicators.getAlertType().getLabel()),
                    button -> {
                        indicators.cycleAlertType();
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(Button.builder(
                    Component.literal("Uncommon color: " + onOff(indicators.isUncommonProjectileColor())),
                    button -> {
                        indicators.setUncommonProjectileColor(!indicators.isUncommonProjectileColor());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(Button.builder(
                    Component.literal("Show arrows: " + onOff(indicators.isShowArrows())),
                    button -> {
                        indicators.setShowArrows(!indicators.isShowArrows());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(Button.builder(
                    Component.literal("Show pearls: " + onOff(indicators.isShowPearls())),
                    button -> {
                        indicators.setShowPearls(!indicators.isShowPearls());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(Button.builder(
                    Component.literal("Show potions: " + onOff(indicators.isShowPotions())),
                    button -> {
                        indicators.setShowPotions(!indicators.isShowPotions());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(Button.builder(
                    Component.literal("Show eggs: " + onOff(indicators.isShowEggs())),
                    button -> {
                        indicators.setShowEggs(!indicators.isShowEggs());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(Button.builder(
                    Component.literal("Show snowballs: " + onOff(indicators.isShowSnowballs())),
                    button -> {
                        indicators.setShowSnowballs(!indicators.isShowSnowballs());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(Button.builder(
                    Component.literal("Show fireballs: " + onOff(indicators.isShowFireballs())),
                    button -> {
                        indicators.setShowFireballs(!indicators.isShowFireballs());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(new PhantomSlider(centerX - 80, y, 160, ROW_HEIGHT, "Radius scale", 0.5, 2.0,
                    indicators.getRadiusScale(), indicators::setRadiusScale));
            y += ROW_HEIGHT + ROW_SPACING;

            this.addRenderableWidget(Button.builder(
                    Component.literal("Show distance: " + onOff(indicators.isShowDistance())),
                    button -> {
                        indicators.setShowDistance(!indicators.isShowDistance());
                        init();
                    }).bounds(centerX - 80, y, 160, ROW_HEIGHT).build());
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
            contentHeight += 4 * (ROW_HEIGHT + ROW_SPACING) + 40;
        }

        if (module instanceof Reach) {
            contentHeight += 6 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof Criticals) {
            contentHeight += 3 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof AimAssist) {
            contentHeight += 17 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof AutoClicker) {
            contentHeight += 7 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof HudModule) {
            contentHeight += 5 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof SpeedBridge) {
            contentHeight += 4 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof Scaffold)
            contentHeight += 3 * (ROW_HEIGHT + ROW_SPACING);

        if (module instanceof WTap) {
            contentHeight += 5 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof FastPlace) {
            contentHeight += 3 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof Indicators) {
            contentHeight += 10 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof Triggerbot) {
            contentHeight += 8 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof BlockHit) {
            contentHeight += 5 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof HitSelect) {
            contentHeight += 4 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof RightClicker) {
            contentHeight += 7 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof JumpReset) {
            contentHeight += 5 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof AutoXPThrow) {
            contentHeight += 1 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof AntiAFK) {
            contentHeight += 8 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof Velocity) {
            contentHeight += 4 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof NoHitDelay) {
            contentHeight += 3 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof SilentAura) {
            contentHeight += 15 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof AutoTotem) {
            contentHeight += 2 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof HealthBar) {
            contentHeight += 6 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof AutoTools) {
            contentHeight += 1 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof ChestESP) {
            contentHeight += 6 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof OreESP) {
            contentHeight += 20 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof BedESP) {
            contentHeight += 2 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof ShulkerESP) {
            contentHeight += 2 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof com.phantom.module.impl.combat.WeaponCycler) {
            contentHeight += 1 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof com.phantom.module.impl.combat.WaterClutch) {
            contentHeight += 4 * (ROW_HEIGHT + ROW_SPACING);
        }

        if (module instanceof com.phantom.module.impl.smp.OreFinder) {
            contentHeight += 5 * (ROW_HEIGHT + ROW_SPACING);
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
        if (module.getDescription().equals(module.getUsageGuide()))
            return 0;

        int height = 8;
        for (String line : module.getDescription().split("\n")) {
            if (line.isEmpty())
                continue;
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