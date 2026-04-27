package com.phantom.gui;

import com.phantom.gui.framework.*;
import com.phantom.module.Module;
import com.phantom.module.impl.combat.*;
import com.phantom.module.impl.movement.*;
import com.phantom.module.impl.player.*;
import com.phantom.module.impl.render.*;
import com.phantom.module.impl.smp.*;
import com.phantom.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Modernized ModuleSettingsScreen with a liquid glassy aesthetic.
 * Replaces vanilla widgets with premium custom components.
 */
public class ModuleSettingsScreen extends Screen {
    private int panelWidth;
    private int panelHeight;
    
    private final Screen parent;
    private final Module module;
    private final List<BaseComponent> components = new ArrayList<>();
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private boolean listeningForKey = false;

    public ModuleSettingsScreen(Screen parent, Module module) {
        super(Component.literal(module.getName() + " Settings"));
        this.parent = parent;
        this.module = module;
    }

    @Override
    protected void init() {
        this.panelWidth = Math.min(this.width - 40, (int)(this.width * 0.7));
        this.panelHeight = Math.min(this.height - 40, (int)(this.height * 0.85));
        rebuildUI();
    }

    private void rebuildUI() {
        components.clear();
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int startX = centerX - panelWidth / 2 + 20;
        
        // Calculate description height to avoid overlap
        List<FormattedCharSequence> descLines = this.font.split(Component.literal(module.getDescription()), panelWidth - 40);
        int descriptionHeight = descLines.size() * 10;
        int startY = centerY - panelHeight / 2 + 65 + descriptionHeight;
        int y = startY - scrollOffset;

        // Back Button
        components.add(new ModernButton(centerX - panelWidth / 2 + 10, centerY - panelHeight / 2 + 10, 50, 20, 
            Component.literal("← Back"), btn -> this.minecraft.setScreen(parent)));

        // Hotkey Binder
        components.add(new ModernButton(startX, y, (panelWidth - 45) / 2, 20, 
            Component.literal(listeningForKey ? "..." : "Bind: " + (module.getKey() == -1 ? "NONE" : org.lwjgl.glfw.GLFW.glfwGetKeyName(module.getKey(), 0))), 
            btn -> {
                listeningForKey = true;
                rebuildUI();
            }));
        components.add(new ModernButton(startX + (panelWidth - 45) / 2 + 5, y, (panelWidth - 45) / 2, 20, 
            Component.literal("Clear"), 
            btn -> {
                module.setKey(-1);
                listeningForKey = false;
                rebuildUI();
            }));
        y += 30;

        // --- Module Specific Settings ---
        // We replicate the logic from the original file but using our new widgets.
        
        if (module instanceof Reach reach) {
            addPresetCycler(startX, y, reach.getCurrentPreset().getName(), reach::cyclePreset);
            y += 30;
            addSlider(startX, y, "Entity Reach", 3.0, 6.0, reach.getEntityReach(), reach::setEntityReach);
            y += 35;
            addSlider(startX, y, "Block Reach", 3.0, 6.0, reach.getBlockReach(), reach::setBlockReach);
            y += 35;
            addToggle(startX, y, "Only while sprinting", reach.isOnlyWhileSprinting(), reach::setOnlyWhileSprinting);
            y += 25;
            addToggle(startX, y, "Moving only", reach.isMovingOnly(), reach::setMovingOnly);
            y += 25;
            addToggle(startX, y, "Disable in water", reach.isDisableInWater(), reach::setDisableInWater);
            y += 25;
        } else if (module instanceof Velocity vel) {
            addSlider(startX, y, "Horizontal KB", 0.0, 1.0, vel.getHorizontalPercent(), vel::setHorizontalPercent);
            y += 35;
            addSlider(startX, y, "Vertical KB", 0.0, 1.0, vel.getVerticalPercent(), vel::setVerticalPercent);
            y += 35;
            addSlider(startX, y, "Chance", 0.0, 1.0, vel.getChance(), vel::setChance);
            y += 35;
            addToggle(startX, y, "Only While Targeting", vel.isOnlyWhileTargeting(), vel::setOnlyWhileTargeting);
            y += 25;
            addToggle(startX, y, "Disable while holding S", vel.isDisableWhileHoldingS(), vel::setDisableWhileHoldingS);
            y += 25;
            addToggle(startX, y, "Pulse Mode", vel.isPulseMode(), vel::setPulseMode);
            y += 25;
            addSlider(startX, y, "Pulse Interval", 1, 100, (double)vel.getPulseInterval(), v -> vel.setPulseInterval(v.intValue()));
            y += 35;
            components.add(new ModernButton(startX, y, panelWidth - 40, 20, Component.literal("Mode: " + vel.getMode().getLabel()), btn -> {
                vel.setMode(Velocity.Mode.values()[(vel.getMode().ordinal() + 1) % Velocity.Mode.values().length]);
                rebuildUI();
            }));
            y += 25;
        } else if (module instanceof SilentAura sa) {
            addSlider(startX, y, "Min CPS", 1.0, 20.0, sa.getMinCps(), sa::setMinCps);
            y += 35;
            addSlider(startX, y, "Max CPS", 1.0, 20.0, sa.getMaxCps(), sa::setMaxCps);
            y += 35;
            addSlider(startX, y, "Smoothing", 1.0, 10.0, sa.getSmoothing(), sa::setSmoothing);
            y += 35;
            addSlider(startX, y, "Snapiness", 1.0, 10.0, sa.getSnapiness(), sa::setSnapiness);
            y += 35;
            addSlider(startX, y, "Max Angle", 10.0, 360.0, sa.getMaxAngle(), sa::setMaxAngle);
            y += 35;
            addSlider(startX, y, "Attack Range", 2.0, 6.0, sa.getAttackRange(), sa::setAttackRange);
            y += 35;
            addToggle(startX, y, "Require Mouse Down", sa.isRequireMouseDown(), sa::setRequireMouseDown);
            y += 25;
            addToggle(startX, y, "Aim Vertically", sa.isAimVertically(), sa::setAimVertically);
            y += 25;
            addToggle(startX, y, "Break Blocks Pause", sa.isBreakBlocksPause(), sa::setBreakBlocksPause);
            y += 25;
            addToggle(startX, y, "Limit To Weapons", sa.isLimitToWeapons(), sa::setLimitToWeapons);
            y += 25;
            addToggle(startX, y, "Target Players", sa.isTargetPlayers(), sa::setTargetPlayers);
            y += 25;
            addToggle(startX, y, "Target Mobs", sa.isTargetMobs(), sa::setTargetMobs);
            y += 25;
            addToggle(startX, y, "Target Animals", sa.isTargetAnimals(), sa::setTargetAnimals);
            y += 25;
            addToggle(startX, y, "Hypixel Mode", sa.isHypixelMode(), sa::setHypixelMode);
            y += 25;
        } else if (module instanceof ESP esp) {
            addToggle(startX, y, "Players", esp.isPlayersEnabled(), esp::setPlayersEnabled);
            y += 25;
            addToggle(startX, y, "Mobs", esp.isMobsEnabled(), esp::setMobsEnabled);
            y += 25;
            addToggle(startX, y, "Animals", esp.isAnimalsEnabled(), esp::setAnimalsEnabled);
            y += 25;
            addToggle(startX, y, "Through Walls", esp.isThroughWalls(), esp::setThroughWalls);
            y += 25;
        } else if (module instanceof HudModule hud) {
            addToggle(startX, y, "Show Active Features", hud.isShowModuleList(), hud::setShowModuleList);
            y += 25;
            addToggle(startX, y, "Show FPS", hud.isShowFps(), hud::setShowFps);
            y += 25;
            addToggle(startX, y, "Show Ping", hud.isShowPing(), hud::setShowPing);
            y += 25;
            addToggle(startX, y, "Show CPS", hud.isShowCps(), hud::setShowCps);
            y += 25;
            addToggle(startX, y, "Debug Logger", hud.isDebugLogger(), hud::setDebugLogger);
            y += 25;
            addToggle(startX, y, "File Logger", hud.isFileLogger(), hud::setFileLogger);
            y += 25;
            // Mode cycler for alignment and side
            components.add(new ModernButton(startX, y, panelWidth - 40, 20, Component.literal("HUD Side: " + (hud.isAlignLeft() ? "Left" : "Right")), btn -> {
                hud.setAlignLeft(!hud.isAlignLeft());
                rebuildUI();
            }));
            y += 25;
            components.add(new ModernButton(startX, y, panelWidth - 40, 20, Component.literal("Stats Side: " + hud.getStatsSide().getLabel()), btn -> {
                hud.cycleStatsSide();
                rebuildUI();
            }));
            y += 25;
        } else if (module instanceof ShulkerESP shulker) {
            addSlider(startX, y, "Range", 8.0, 128.0, shulker.getRange(), shulker::setRange);
            y += 35;
        } else if (module instanceof ChestESP chest) {
            addSlider(startX, y, "Range", 8.0, 128.0, chest.getRange(), chest::setRange);
            y += 35;
            addToggle(startX, y, "Chests", chest.isChestsEnabled(), chest::setChestsEnabled);
            y += 25;
        } else if (module instanceof BedESP bed) {
            addSlider(startX, y, "Range", 8.0, 128.0, bed.getRange(), bed::setRange);
            y += 35;
        } else if (module instanceof AutoClicker ac) {
            addPresetCycler(startX, y, ac.getCurrentPreset().getName(), ac::cyclePreset);
            y += 30;
            addSlider(startX, y, "Min CPS", 1.0, 20.0, ac.getMinCps(), ac::setMinCps);
            y += 35;
            addSlider(startX, y, "Max CPS", 1.0, 20.0, ac.getMaxCps(), ac::setMaxCps);
            y += 35;
            addToggle(startX, y, "Require Mouse Down", ac.isRequireMouseDown(), ac::setRequireMouseDown);
            y += 25;
            addToggle(startX, y, "Break Blocks Pause", ac.isBreakBlockPause(), ac::setBreakBlockPause);
            y += 25;
            addToggle(startX, y, "Hit Entities Only", ac.isHitEntitiesOnly(), ac::setHitEntitiesOnly);
            y += 25;
            addToggle(startX, y, "Weapon Only", ac.isOnlyWithWeapon(), ac::setOnlyWithWeapon);
            y += 25;
        } else if (module instanceof AimAssist aim) {
            addPresetCycler(startX, y, aim.getCurrentPreset().getName(), aim::cyclePreset);
            y += 25;
            addToggle(startX, y, "Show FOV", aim.isShowFovCircle(), aim::setShowFovCircle);
            y += 30;
            addSlider(startX, y, "Distance", 2.0, 8.0, aim.getDistance(), aim::setDistance);
            y += 35;
            addSlider(startX, y, "Smoothing", 0.1, 10.0, aim.getSmoothing(), aim::setSmoothing);
            y += 35;
            addSlider(startX, y, "Snapiness", 1.0, 10.0, aim.getSnapiness(), aim::setSnapiness);
            y += 35;
            addSlider(startX, y, "FOV", 10.0, 360.0, aim.getFov(), aim::setFov);
            y += 35;
            addToggle(startX, y, "Require Mouse Down", aim.isRequireMouseDown(), aim::setRequireMouseDown);
            y += 25;
            addToggle(startX, y, "Click Aim", aim.isClickAim(), aim::setClickAim);
            y += 25;
            addToggle(startX, y, "Aim Vertically", aim.isAimVertically(), aim::setAimVertically);
            y += 25;
            addToggle(startX, y, "Limit To Weapons", aim.isLimitToWeapons(), aim::setLimitToWeapons);
            y += 25;
            addToggle(startX, y, "Visibility Check", aim.isVisibilityCheck(), aim::setVisibilityCheck);
            y += 25;
            addToggle(startX, y, "Target Players", aim.isTargetPlayers(), aim::setTargetPlayers);
            y += 25;
            addToggle(startX, y, "Target Mobs", aim.isTargetMobs(), aim::setTargetMobs);
            y += 25;
            addToggle(startX, y, "Target Animals", aim.isTargetAnimals(), aim::setTargetAnimals);
            y += 25;
            addToggle(startX, y, "Show FOV Circle", aim.isShowFovCircle(), aim::setShowFovCircle);
            y += 25;
            components.add(new ModernButton(startX, y, panelWidth - 40, 20, Component.literal("Target: " + aim.getTargetArea().getLabel()), btn -> {
                aim.cycleTargetArea();
                rebuildUI();
            }));
            y += 25;
            components.add(new ModernButton(startX, y, panelWidth - 40, 20, Component.literal("Mode: " + aim.getTargetMode().getLabel()), btn -> {
                aim.cycleTargetMode();
                rebuildUI();
            }));
            y += 25;
        } else if (module instanceof Scaffold scaffold) {
            addToggle(startX, y, "Safe Walk", scaffold.isSafeWalk(), scaffold::setSafeWalk);
            y += 25;
            addToggle(startX, y, "Swing Arm", scaffold.isSwingArm(), scaffold::setSwingArm);
            y += 25;
            addSlider(startX, y, "Place Delay", 0, 10, (double)scaffold.getPlaceDelay(), v -> scaffold.setPlaceDelay(v.intValue()));
            y += 35;
        } else if (module instanceof WTap wtap) {
            addPresetCycler(startX, y, wtap.getCurrentPreset().getName(), wtap::cyclePreset);
            y += 30;
            addSlider(startX, y, "Chance", 0.0, 1.0, wtap.getChance(), wtap::setChance);
            y += 35;
            addSlider(startX, y, "Trigger Delay (ms)", 0, 250, (double) wtap.getTriggerDelayMs(), v -> wtap.setTriggerDelayMs(v.intValue()));
            y += 35;
            addSlider(startX, y, "Release Delay (ms)", 0, 250, (double) wtap.getReleaseDelayMs(), v -> wtap.setReleaseDelayMs(v.intValue()));
            y += 35;
            addSlider(startX, y, "Repress Delay (ms)", 0, 250, (double) wtap.getRepressDelayMs(), v -> wtap.setRepressDelayMs(v.intValue()));
            y += 35;
            addSlider(startX, y, "Cooldown (ms)", 0, 1000, (double) wtap.getCooldownMs(), v -> wtap.setCooldownMs(v.intValue()));
            y += 35;
            addToggle(startX, y, "Select Hits", wtap.isSelectHits(), wtap::setSelectHits);
            y += 25;
            addToggle(startX, y, "Players Only", wtap.isPlayersOnly(), wtap::setPlayersOnly);
            y += 25;
        } else if (module instanceof JumpReset jr) {
            addSlider(startX, y, "Chance", 0.0, 1.0, jr.getChance(), jr::setChance);
            y += 35;
            addSlider(startX, y, "Accuracy", 0.0, 1.0, jr.getAccuracy(), jr::setAccuracy);
            y += 35;
            addSlider(startX, y, "Jump Chance %", 0.0, 100.0, jr.getJumpChancePercent(), jr::setJumpChancePercent);
            y += 35;
            addSlider(startX, y, "Max Delay (Ticks)", 0, 6, (double) jr.getMaxDelayTicks(), v -> jr.setMaxDelayTicks(v.intValue()));
            y += 35;
            addSlider(startX, y, "Cooldown (Ticks)", 0, 20, (double) jr.getCooldownTicks(), v -> jr.setCooldownTicks(v.intValue()));
            y += 35;
            addToggle(startX, y, "Only When Targeting", jr.isOnlyWhenTargeting(), jr::setOnlyWhenTargeting);
            y += 25;
            addToggle(startX, y, "Water Check", jr.isWaterCheck(), jr::setWaterCheck);
            y += 25;
            addToggle(startX, y, "Require Mouse Down", jr.isRequireMouseDown(), jr::setRequireMouseDown);
            y += 25;
            addToggle(startX, y, "Moving Forward Only", jr.isRequireMovingForward(), jr::setRequireMovingForward);
            y += 25;
            addToggle(startX, y, "Check FOV", jr.isCheckFOV(), jr::setCheckFOV);
            y += 25;
        } else if (module instanceof WaterClutch wc) {
            addToggle(startX, y, "Clutch", wc.isClutch(), wc::setClutch);
            y += 25;
            addToggle(startX, y, "Extinguish", wc.isExtinguish(), wc::setExtinguish);
            y += 25;
            addSlider(startX, y, "Trigger Height", 1, 64, (double) wc.getTriggerHeight(), v -> wc.setTriggerHeight(v.intValue()));
            y += 35;
            addSlider(startX, y, "Fire Threshold (Ticks)", 1, 100, (double) wc.getFireDurationThreshold(), v -> wc.setFireDurationThreshold(v.intValue()));
            y += 35;
        } else if (module instanceof AntiAFK afk) {
            addSlider(startX, y, "Min Start Delay (s)", 5, 300, afk.getMinStartDelaySeconds(), afk::setMinStartDelaySeconds);
            y += 35;
            addSlider(startX, y, "Max Start Delay (s)", 5, 300, afk.getMaxStartDelaySeconds(), afk::setMaxStartDelaySeconds);
            y += 35;
            addSlider(startX, y, "Frequency", 0.1, 1.0, afk.getFrequency(), afk::setFrequency);
            y += 35;
            addSlider(startX, y, "Max Yaw Change", 1, 90, afk.getMaxYawChange(), afk::setMaxYawChange);
            y += 35;
            addSlider(startX, y, "Max Pitch Change", 1, 90, afk.getMaxPitchChange(), afk::setMaxPitchChange);
            y += 35;
            addToggle(startX, y, "Keep Close", afk.isKeepClose(), afk::setKeepClose);
            y += 25;
            addToggle(startX, y, "Rotation", afk.isRotation(), afk::setRotation);
            y += 25;
            addToggle(startX, y, "Silent Aim", afk.isSilentAim(), afk::setSilentAim);
            y += 25;
        } else if (module instanceof AutoGG gg) {
            addSlider(startX, y, "Delay (ms)", 0, 5000, (double) gg.getDelayMs(), v -> gg.setDelayMs(v.intValue()));
            y += 35;
            // Message editing would need a text field, which we don't have yet.
            // For now, we'll just show the current message in a button that cycles presets?
            // Actually, let's just leave it as is.
        } else if (module instanceof WeaponCycler cycler) {
            components.add(new ModernButton(startX, y, panelWidth - 40, 20, Component.literal("Combo: " + cycler.getWeaponCombo().getLabel()), btn -> {
                cycler.cycleWeaponCombo();
                rebuildUI();
            }));
            y += 25;
        } else if (module instanceof LatencyAlerts la) {
            addSlider(startX, y, "High Ping (ms)", 50, 1000, (double) la.getHighPingMs(), v -> la.setHighPingMs(v.intValue()));
            y += 35;
            addSlider(startX, y, "Spike Increase (ms)", 10, 500, (double) la.getSpikeIncreaseMs(), v -> la.setSpikeIncreaseMs(v.intValue()));
            y += 35;
            addSlider(startX, y, "Alert Cooldown (s)", 1, 60, (double) la.getAlertCooldownSeconds(), v -> la.setAlertCooldownSeconds(v.intValue()));
            y += 35;
        } else if (module instanceof BlockHit bh) {
            addPresetCycler(startX, y, bh.getCurrentPreset().getName(), bh::cyclePreset);
            y += 30;
            addSlider(startX, y, "Chance", 0.0, 1.0, bh.getChance(), bh::setChance);
            y += 35;
            addToggle(startX, y, "Require Mouse Down", bh.isRequireMouseDown(), bh::setRequireMouseDown);
            y += 25;
            addToggle(startX, y, "Visual Animation", bh.isVisualAnimation(), bh::setVisualAnimation);
            y += 25;
            components.add(new ModernButton(startX, y, panelWidth - 40, 20, Component.literal("Mode: " + bh.getMode().getLabel()), btn -> {
                bh.cycleMode();
                rebuildUI();
            }));
            y += 25;
        } else if (module instanceof HitSelect hs) {
            addSlider(startX, y, "Pause Duration (ms)", 0, 1000, (double) hs.getPauseDurationMs(), v -> hs.setPauseDurationMs(v.intValue()));
            y += 35;
            addSlider(startX, y, "Wait For First Hit (ms)", 0, 1000, (double) hs.getWaitForFirstHitMs(), v -> hs.setWaitForFirstHitMs(v.intValue()));
            y += 35;
            addSlider(startX, y, "In Combat Cancel %", 0.0, 100.0, hs.getInCombatCancelRate(), hs::setInCombatCancelRate);
            y += 35;
            addSlider(startX, y, "Missed Swing Cancel %", 0.0, 100.0, hs.getMissedSwingsCancelRate(), hs::setMissedSwingsCancelRate);
            y += 35;
            addToggle(startX, y, "Disable During KB", hs.isDisableDuringKnockback(), hs::setDisableDuringKnockback);
            y += 25;
            addToggle(startX, y, "Only While Damaged", hs.isOnlyWhileDamaged(), hs::setOnlyWhileDamaged);
            y += 25;
            components.add(new ModernButton(startX, y, panelWidth - 40, 20, Component.literal("Mode: " + hs.getMode().getLabel()), btn -> {
                hs.cycleMode();
                rebuildUI();
            }));
            y += 25;
        } else if (module instanceof BowAimbot ba) {
            components.add(new ModernButton(startX, y, panelWidth - 40, 20, Component.literal("Preset: " + ba.getPreset().getName()), btn -> {
                ba.cyclePreset();
                rebuildUI();
            }));
            y += 25;
            addSlider(startX, y, "FOV", 1.0, 360.0, ba.getFov(), ba::setFov);
            y += 35;
            addSlider(startX, y, "Smoothing", 1.0, 10.0, ba.getSmoothing(), ba::setSmoothing);
            y += 35;
            addSlider(startX, y, "Max Distance", 10.0, 100.0, ba.getMaxDistance(), ba::setMaxDistance);
            y += 35;
            addToggle(startX, y, "Predict Movement", ba.isPredictMovement(), ba::setPredictMovement);
            y += 25;
            addToggle(startX, y, "Vertical Correction", ba.isVerticalCorrection(), ba::setVerticalCorrection);
            y += 25;
            addToggle(startX, y, "Players Only", ba.isPlayersOnly(), ba::setPlayersOnly);
            y += 25;
            addToggle(startX, y, "Visibility Check", ba.isVisibilityCheck(), ba::setVisibilityCheck);
            y += 25;
            addToggle(startX, y, "Require Mouse Down", ba.isRequireMouseDown(), ba::setRequireMouseDown);
            y += 25;
            addToggle(startX, y, "Team Check", ba.isTeamCheck(), ba::setTeamCheck);
            y += 25;
            addToggle(startX, y, "Only Full Draw", ba.isOnlyFullDraw(), ba::setOnlyFullDraw);
            y += 25;
            addToggle(startX, y, "Legit Jitter", ba.isLegitJitter(), ba::setLegitJitter);
            y += 25;
        } else if (module instanceof TNTTimer tnt) {
            addSlider(startX, y, "Scale", 0.5, 3.0, tnt.getScale(), tnt::setScale);
            y += 35;
        } else if (module instanceof AutoXPThrow axp) {
            addSlider(startX, y, "Throws Per Tick", 1, 20, (double) axp.getThrowsPerTick(), v -> axp.setThrowsPerTick(v.intValue()));
            y += 35;
        } else if (module instanceof Triggerbot trigger) {
            addPresetCycler(startX, y, trigger.getCurrentPreset().getName(), trigger::cyclePreset);
            y += 30;
            addSlider(startX, y, "Extra Delay", 0, 20, (double)trigger.getExtraDelayTicks(), v -> trigger.setExtraDelayTicks(v.intValue()));
            y += 35;
            addSlider(startX, y, "Miss Chance", 0.0, 1.0, trigger.getTargetMissChance(), trigger::setTargetMissChance);
            y += 35;
            addSlider(startX, y, "Early Hit Chance", 0.0, 1.0, trigger.getEarlyHitChance(), trigger::setEarlyHitChance);
            y += 35;
            addToggle(startX, y, "Require Mouse Down", trigger.isRequireMouseDown(), trigger::setRequireMouseDown);
            y += 25;
            addToggle(startX, y, "Air Crits", trigger.isAirCrits(), trigger::setAirCrits);
            y += 25;
            addToggle(startX, y, "Shield Check", trigger.isShieldCheck(), trigger::setShieldCheck);
            y += 25;
            addToggle(startX, y, "Limit Items", trigger.isLimitItems(), trigger::setLimitItems);
            y += 25;
            components.add(new ModernButton(startX, y, panelWidth - 40, 20, Component.literal("Target: " + trigger.getTargetMode().getLabel()), btn -> {
                trigger.setTargetMode(Triggerbot.TargetMode.values()[(trigger.getTargetMode().ordinal() + 1) % Triggerbot.TargetMode.values().length]);
                rebuildUI();
            }));
            y += 25;
        } else if (module instanceof Criticals crit) {
            addSlider(startX, y, "Chance", 0.0, 1.0, crit.getChance(), crit::setChance);
            y += 35;
        } else if (module instanceof Indicators ind) {
            addSlider(startX, y, "Scale", 0.5, 5.0, ind.getRadiusScale(), ind::setRadiusScale);
            y += 35;
            addToggle(startX, y, "Arrows", ind.isShowArrows(), ind::setShowArrows);
            y += 25;
            addToggle(startX, y, "Pearls", ind.isShowPearls(), ind::setShowPearls);
            y += 25;
            addToggle(startX, y, "Potions", ind.isShowPotions(), ind::setShowPotions);
            y += 25;
            addToggle(startX, y, "Eggs", ind.isShowEggs(), ind::setShowEggs);
            y += 25;
            addToggle(startX, y, "Snowballs", ind.isShowSnowballs(), ind::setShowSnowballs);
            y += 25;
            addToggle(startX, y, "Fireballs", ind.isShowFireballs(), ind::setShowFireballs);
            y += 25;
            addToggle(startX, y, "Show Distance", ind.isShowDistance(), ind::setShowDistance);
            y += 25;
            addToggle(startX, y, "Only Approaching", ind.isOnlyWhenApproaching(), ind::setOnlyWhenApproaching);
            y += 25;
            addToggle(startX, y, "Render Item", ind.isRenderItem(), ind::setRenderItem);
            y += 25;
        } else if (module instanceof Trajectories traj) {
            addToggle(startX, y, "Only When Drawing", traj.isOnlyWhenDrawing(), traj::setOnlyWhenDrawing);
            y += 25;
            addSlider(startX, y, "Max Ticks", 20, 200, traj.getMaxTicks(), traj::setMaxTicks);
            y += 35;
            addSlider(startX, y, "Thickness", 0.5, 3.0, (double)traj.getThickness(), v -> traj.setThickness(v.floatValue()));
            y += 35;
        } else if (module instanceof SpeedBridge sb) {
            addPresetCycler(startX, y, sb.getCurrentPreset().getName(), sb::cyclePreset);
            y += 30;
            addSlider(startX, y, "Auto Off Delay (s)", 0.5, 10.0, sb.getAutoOffDelay(), sb::setAutoOffDelay);
            y += 35;
            addSlider(startX, y, "Delay Ticks", 0, 4, (double)sb.getDelayTicks(), v -> sb.setDelayTicks(v.intValue()));
            y += 35;
            addToggle(startX, y, "Blocks Only", sb.isBlocksOnly(), sb::setBlocksOnly);
            y += 25;
            addToggle(startX, y, "Sneak On Jump", sb.isSneakOnJump(), sb::setSneakOnJump);
            y += 25;
            addToggle(startX, y, "Bridge Assist", sb.isBridgeAssistEnabled(), sb::setBridgeAssistEnabled);
            y += 25;
            addToggle(startX, y, "Tower Mode", sb.isTowerModeEnabled(), sb::setTowerModeEnabled);
            y += 25;
            addSlider(startX, y, "Scaffold Delay", 0, 10, (double)sb.getScaffoldPlaceDelay(), v -> sb.setScaffoldPlaceDelay(v.intValue()));
            y += 35;
            addToggle(startX, y, "Swing Arm", sb.isSwingArm(), sb::setSwingArm);
            y += 25;
        } else if (module instanceof FastPlace fp) {
            addPresetCycler(startX, y, fp.getCurrentPreset().getName(), fp::cyclePreset);
            y += 30;
            addSlider(startX, y, "Delay Ticks", 0, 4, (double)fp.getDelayTicks(), v -> fp.setDelayTicks(v.intValue()));
            y += 35;
            addToggle(startX, y, "Blocks Only", fp.isBlocksOnly(), fp::setBlocksOnly);
            y += 25;
        } else if (module instanceof TimeChanger tc) {
            addSlider(startX, y, "Time", 0, 24000, tc.getTargetTime(), tc::setTargetTime);
            y += 35;
            addToggle(startX, y, "Freeze Time", tc.isFreezeTime(), tc::setFreezeTime);
            y += 25;
            components.add(new ModernButton(startX, y, panelWidth - 40, 20, Component.literal("Weather: " + tc.getWeatherMode().getLabel()), btn -> {
                tc.cycleWeather();
                rebuildUI();
            }));
            y += 25;
            components.add(new ModernButton(startX, y, panelWidth - 40, 20, Component.literal("Time Preset: " + tc.getCurrentPreset().getLabel()), btn -> {
                tc.cycleTimePreset();
                rebuildUI();
            }));
            y += 25;
        } else if (module instanceof AutoTotem at) {
            addSlider(startX, y, "Health Threshold", 0, 20, at.getHealthThreshold(), at::setHealthThreshold);
            y += 35;
            addToggle(startX, y, "Always Equip", at.isAlwaysEquip(), at::setAlwaysEquip);
            y += 25;
            addSlider(startX, y, "Min Delay", 0, 500, (double)at.getDelayMin(), v -> at.setDelayMin(v.intValue()));
            y += 35;
            addSlider(startX, y, "Max Delay", 0, 500, (double)at.getDelayMax(), v -> at.setDelayMax(v.intValue()));
            y += 35;
        } else if (module instanceof AutoTools at) {
            components.add(new ModernButton(startX, y, panelWidth - 40, 20, Component.literal("Attack Priority: " + at.getAttackPriority().getLabel()), btn -> {
                at.cycleAttackPriority();
                rebuildUI();
            }));
            y += 25;
        }
        
        maxScroll = Math.max(0, y - (centerY + panelHeight / 2 - 20));
    }

    private void addSlider(int x, int y, String label, double min, double max, double value, java.util.function.Consumer<Double> consumer) {
        components.add(new ModernSlider(x, y, panelWidth - 40, 20, label, min, max, value, consumer));
    }

    private void addToggle(int x, int y, String label, boolean enabled, java.util.function.Consumer<Boolean> consumer) {
        components.add(new ModernToggle(x, y, panelWidth - 40, 20, label, enabled, consumer));
    }

    private void addPresetCycler(int x, int y, String current, Runnable cycler) {
        components.add(new ModernButton(x, y, panelWidth - 40, 20, Component.literal("Preset: " + current), btn -> {
            cycler.run();
            rebuildUI();
        }));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Draw parent screen as background
        this.parent.render(graphics, -1, -1, delta);
        
        // Draw Main Glass Panel
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        RenderUtil.drawGlassPanel(graphics, centerX - panelWidth / 2, centerY - panelHeight / 2, 
            panelWidth, panelHeight, 0xE0050505, 0x40FFFFFF);

        // Header
        FontDescription cleanFont = new FontDescription.Resource(Identifier.fromNamespaceAndPath("minecraft", "uniform"));
        graphics.drawCenteredString(this.font, Component.literal(module.getName().toUpperCase()).withStyle(s -> s.withFont(cleanFont)), centerX, centerY - panelHeight / 2 + 15, 0xFFA8E6A3);
        
        // Description
        List<FormattedCharSequence> descLines = this.font.split(Component.literal(module.getDescription()).withStyle(s -> s.withFont(cleanFont)), panelWidth - 40);
        int descY = centerY - panelHeight / 2 + 35;
        for (FormattedCharSequence line : descLines) {
            graphics.drawCenteredString(this.font, line, centerX, descY, 0xFFBBBBBB);
            descY += 10;
        }

        // Render Components
        for (BaseComponent component : components) {
            // Always render back button
            if (component instanceof ModernButton && ((ModernButton)component).getMessage().getString().contains("Back")) {
                component.render(graphics, mouseX, mouseY, delta);
                continue;
            }
            // Clip other components
            if (component.getY() + component.getHeight() > centerY - panelHeight / 2 + 60 &&
                component.getY() < centerY + panelHeight / 2 - 20) {
                component.render(graphics, mouseX, mouseY, delta);
            }
        }

        // Draw Scroll Bar
        if (maxScroll > 0) {
            int scrollBarX = centerX + panelWidth / 2 - 8;
            int scrollBarY = centerY - panelHeight / 2 + 60;
            int scrollBarHeight = panelHeight - 80;
            
            // Track
            graphics.fill(scrollBarX, scrollBarY, scrollBarX + 4, scrollBarY + scrollBarHeight, 0x40000000);
            
            // Thumb
            int thumbHeight = Math.max(20, (int) ((double) (panelHeight - 80) * (panelHeight - 80) / (maxScroll + panelHeight - 80)));
            int thumbY = scrollBarY + (int) ((double) scrollOffset / maxScroll * (scrollBarHeight - thumbHeight));
            graphics.fill(scrollBarX, thumbY, scrollBarX + 4, thumbY + thumbHeight, 0xFFA8E6A3);
        }
        
        if (listeningForKey) {
            graphics.drawCenteredString(this.font, "PRESS ANY KEY TO BIND", centerX, centerY + panelHeight / 2 - 40, 0xFFA8E6A3);
            graphics.drawCenteredString(this.font, "ESC TO CLEAR", centerX, centerY + panelHeight / 2 - 30, 0xFF888888);
        }
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key();
        if (listeningForKey) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                module.setKey(-1);
            } else {
                module.setKey(keyCode);
            }
            listeningForKey = false;
            rebuildUI();
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            this.minecraft.setScreen(parent);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean isFocused) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        
        int centerY = this.height / 2;
        int panelMinY = centerY - panelHeight / 2 + 60;
        int panelMaxY = centerY + panelHeight / 2 - 20;

        for (BaseComponent component : components) {
            if (component instanceof ModernButton && ((ModernButton)component).getMessage().getString().contains("Back")) {
                if (component.mouseClicked(mouseX, mouseY, button)) return true;
                continue;
            }
            if (component.getY() + component.getHeight() > panelMinY && component.getY() < panelMaxY) {
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
    public boolean isPauseScreen() {
        return false;
    }
}
