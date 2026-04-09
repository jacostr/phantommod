package com.phantom.config;

import com.phantom.PhantomMod;
import com.phantom.module.Module;
import com.phantom.module.ModuleManager;
import com.phantom.module.impl.combat.AimAssist;
import com.phantom.module.impl.combat.AutoClicker;
import com.phantom.module.impl.combat.BlockHit;
import com.phantom.module.impl.combat.Criticals;
import com.phantom.module.impl.combat.HitSelect;
import com.phantom.module.impl.combat.JumpReset;
import com.phantom.module.impl.combat.Reach;
import com.phantom.module.impl.combat.Triggerbot;
import com.phantom.module.impl.combat.Velocity;
import com.phantom.module.impl.combat.WTap;
import com.phantom.module.impl.movement.AlwaysSprint;
import com.phantom.module.impl.movement.NoJumpDelay;
import com.phantom.module.impl.movement.Scaffold;
import com.phantom.module.impl.movement.SpeedBridge;
import com.phantom.module.impl.player.AutoTools;
import com.phantom.module.impl.player.FastPlace;
import com.phantom.module.impl.render.ESP;
import com.phantom.module.impl.render.FullBright;
import com.phantom.module.impl.render.HudModule;
import org.lwjgl.glfw.GLFW;

import java.util.Properties;

public final class ProfileManager {
    public static final String CUSTOM_PROFILE_NAME = "custom";

    private ProfileManager() {
    }

    public static void applyLegit(ModuleManager moduleManager) {
        resetModules(moduleManager);

        AimAssist aimAssist = moduleManager.getModuleByClass(AimAssist.class);
        if (aimAssist != null) {
            aimAssist.setSmoothing(8.0);
            aimAssist.setFov(60.0);
            aimAssist.setDistance(4.0);
            aimAssist.setRequireMouseDown(true);
            aimAssist.setAimVertically(false);
            aimAssist.setLimitToWeapons(true);
            aimAssist.setTargetPlayers(true);
            aimAssist.setTargetMobs(false);
            aimAssist.setTargetAnimals(false);
            aimAssist.setEnabledSilently(true);
        }

        AutoClicker autoClicker = moduleManager.getModuleByClass(AutoClicker.class);
        if (autoClicker != null) {
            autoClicker.applyPresetLegit();
            autoClicker.setEnabledSilently(true);
        }

        Reach reach = moduleManager.getModuleByClass(Reach.class);
        if (reach != null) {
            reach.applyPresetLegit();
            reach.setOnlyWhileSprinting(true);
            reach.setDisableInWater(true);
            reach.setEnabledSilently(false);
        }

        Velocity velocity = moduleManager.getModuleByClass(Velocity.class);
        if (velocity != null) {
            velocity.applyPresetLegit();
            velocity.setEnabledSilently(false);
        }

        Triggerbot triggerbot = moduleManager.getModuleByClass(Triggerbot.class);
        if (triggerbot != null) {
            triggerbot.applyPresetLegit();
            triggerbot.setEnabledSilently(true);
        }

        WTap wTap = moduleManager.getModuleByClass(WTap.class);
        if (wTap != null) {
            wTap.applyPresetLegit();
            wTap.setEnabledSilently(true);
        }

        JumpReset jumpReset = moduleManager.getModuleByClass(JumpReset.class);
        if (jumpReset != null) {
            jumpReset.setEnabledSilently(true);
        }

        FastPlace fastPlace = moduleManager.getModuleByClass(FastPlace.class);
        if (fastPlace != null) {
            fastPlace.applyPresetLegit();
            fastPlace.setEnabledSilently(true);
        }

        HudModule hud = moduleManager.getModuleByClass(HudModule.class);
        if (hud != null) {
            hud.setEnabledSilently(true);
            hud.setAlignLeft(true);
            while (hud.getStatsSide() != HudModule.CornerSide.RIGHT) {
                hud.cycleStatsSide();
            }
        }

        AlwaysSprint alwaysSprint = moduleManager.getModuleByClass(AlwaysSprint.class);
        if (alwaysSprint != null) {
            alwaysSprint.setEnabledSilently(true);
        }

        NoJumpDelay noJumpDelay = moduleManager.getModuleByClass(NoJumpDelay.class);
        if (noJumpDelay != null) {
            noJumpDelay.setEnabledSilently(true);
        }

        ESP esp = moduleManager.getModuleByClass(ESP.class);
        if (esp != null) {
            esp.setEnabledSilently(true);
        }

        FullBright fullBright = moduleManager.getModuleByClass(FullBright.class);
        if (fullBright != null) {
            fullBright.setEnabledSilently(true);
        }

        SpeedBridge speedBridge = moduleManager.getModuleByClass(SpeedBridge.class);
        if (speedBridge != null) {
            speedBridge.setKey(GLFW.GLFW_KEY_X);
            speedBridge.setEnabledSilently(false);
        }

        AutoTools autoTools = moduleManager.getModuleByClass(AutoTools.class);
        if (autoTools != null) {
            autoTools.setEnabledSilently(true);
        }

        PhantomMod.saveConfig();
    }

    public static void applyObvious(ModuleManager moduleManager) {
        resetModules(moduleManager);

        AimAssist aimAssist = moduleManager.getModuleByClass(AimAssist.class);
        if (aimAssist != null) {
            aimAssist.setSmoothing(3.0);
            aimAssist.setFov(180.0);
            aimAssist.setDistance(5.5);
            aimAssist.setRequireMouseDown(true);
            aimAssist.setAimVertically(true);
            aimAssist.setLimitToWeapons(true);
            aimAssist.setTargetPlayers(true);
            aimAssist.setTargetMobs(true);
            aimAssist.setTargetAnimals(false);
            aimAssist.setEnabledSilently(true);
        }

        AutoClicker autoClicker = moduleManager.getModuleByClass(AutoClicker.class);
        if (autoClicker != null) {
            autoClicker.applyPresetObvious();
            autoClicker.setEnabledSilently(true);
        }

        Reach reach = moduleManager.getModuleByClass(Reach.class);
        if (reach != null) {
            reach.applyPresetLegit();
            reach.setOnlyWhileSprinting(true);
            reach.setDisableInWater(true);
            reach.setEnabledSilently(true);
        }

        Velocity velocity = moduleManager.getModuleByClass(Velocity.class);
        if (velocity != null) {
            velocity.applyPresetSubtle();
            velocity.setEnabledSilently(true);
        }

        Triggerbot triggerbot = moduleManager.getModuleByClass(Triggerbot.class);
        if (triggerbot != null) {
            triggerbot.applyPresetObvious();
            triggerbot.setEnabledSilently(true);
        }

        WTap wTap = moduleManager.getModuleByClass(WTap.class);
        if (wTap != null) {
            wTap.applyPresetObvious();
            wTap.setEnabledSilently(true);
        }

        FastPlace fastPlace = moduleManager.getModuleByClass(FastPlace.class);
        if (fastPlace != null) {
            fastPlace.applyPresetObvious();
            fastPlace.setEnabledSilently(true);
        }

        HudModule hud = moduleManager.getModuleByClass(HudModule.class);
        if (hud != null) {
            hud.setEnabledSilently(true);
            hud.setAlignLeft(true);
            while (hud.getStatsSide() != HudModule.CornerSide.RIGHT) {
                hud.cycleStatsSide();
            }
        }

        Criticals criticals = moduleManager.getModuleByClass(Criticals.class);
        if (criticals != null) {
            criticals.setEnabledSilently(true);
        }

        BlockHit blockHit = moduleManager.getModuleByClass(BlockHit.class);
        if (blockHit != null) {
            blockHit.applyPresetObvious();
            blockHit.setEnabledSilently(true);
        }

        HitSelect hitSelect = moduleManager.getModuleByClass(HitSelect.class);
        if (hitSelect != null) {
            hitSelect.applyPresetObvious();
            hitSelect.setEnabledSilently(true);
        }

        Scaffold scaffold = moduleManager.getModuleByClass(Scaffold.class);
        if (scaffold != null) {
            scaffold.setKey(GLFW.GLFW_KEY_V);
            scaffold.setEnabledSilently(false);
        }

        PhantomMod.saveConfig();
    }

    public static void saveCustom(ModuleManager moduleManager) {
        Properties properties = ConfigManager.capture(moduleManager);
        ConfigManager.writeProfile(CUSTOM_PROFILE_NAME, properties);
    }

    public static boolean loadCustom(ModuleManager moduleManager) {
        Properties properties = ConfigManager.readProfile(CUSTOM_PROFILE_NAME);
        if (properties.isEmpty()) {
            return false;
        }

        ConfigManager.apply(moduleManager, properties, true);
        return true;
    }

    private static void resetModules(ModuleManager moduleManager) {
        for (Module module : moduleManager.getModules()) {
            boolean keepOn = module instanceof HudModule;
            module.setEnabledSilently(keepOn);
        }
    }
}
