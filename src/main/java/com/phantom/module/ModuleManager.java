/*
 * ModuleManager.java — Central registry for all modules.
 *
 * Constructs every module instance at startup, loads saved config from disk,
 * and provides dispatch methods (onTick, onRender, onHudRender, handleKeybinds)
 * that PhantomMod.java calls from its event hooks. Also exposes lookup methods
 * (getModuleByName, getModuleByClass) used by mixins and the GUI.
 */
package com.phantom.module;

import com.phantom.config.ConfigManager;
import com.phantom.module.impl.movement.AlwaysSprint;
import com.phantom.module.impl.combat.AutoClicker;
import com.phantom.module.impl.combat.BlockHit;
import com.phantom.module.impl.combat.HitSelect;
import com.phantom.module.impl.movement.SpeedBridge;
import com.phantom.module.impl.combat.JumpReset;
import com.phantom.module.impl.combat.RightClicker;
import com.phantom.module.impl.combat.Velocity;
import com.phantom.module.impl.combat.WTap;
import com.phantom.module.impl.player.AutoTools;
import com.phantom.module.impl.player.AntiAFK;
import com.phantom.module.impl.player.FastPlace;
import com.phantom.module.impl.player.NoFall;
import com.phantom.module.impl.render.ESP;
import com.phantom.module.impl.player.AntiBot;
import com.phantom.module.impl.movement.SafeWalk;
import com.phantom.module.impl.render.FullBright;
import com.phantom.module.impl.render.HudModule;
import com.phantom.module.impl.render.Indicators;
import com.phantom.module.impl.combat.Reach;
import com.phantom.module.impl.combat.Triggerbot;
import com.phantom.module.impl.combat.Criticals;
import com.phantom.module.impl.combat.AimAssist;
import com.phantom.module.impl.movement.NoJumpDelay;
import com.phantom.module.impl.movement.Scaffold;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.gui.GuiGraphics;
import com.phantom.module.impl.player.AutoXPThrow;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();

    public ModuleManager() {
        HudModule hudModule = new HudModule();
        modules.add(new AlwaysSprint());
        modules.add(new FullBright());
        modules.add(new ESP());
        modules.add(new Indicators());
        modules.add(new SpeedBridge());
        modules.add(new AutoTools());
        modules.add(new NoFall());
        modules.add(new Reach());
        modules.add(new Criticals());
        modules.add(new AimAssist());
        modules.add(new AutoClicker());
        modules.add(new BlockHit());
        modules.add(new HitSelect());
        modules.add(new RightClicker());
        modules.add(new JumpReset());
        modules.add(new Triggerbot());
        modules.add(new WTap());
        modules.add(new Velocity());
        modules.add(new Scaffold());
        modules.add(new NoJumpDelay());
        modules.add(new AntiAFK());
        modules.add(new FastPlace());
        modules.add(new AntiBot());
        modules.add(new SafeWalk());
        modules.add(new AutoXPThrow());
        modules.add(hudModule);

        modules.sort(Comparator.comparing((Module module) -> module.getCategory().ordinal())
                .thenComparing(Module::getName));
        
        hudModule.initializeEnabledSilently(true);
        ConfigManager.load(this);
        for (Module module : modules) {
            module.applyLoadedEnableState();
        }
    }

    public List<Module> getModules() { return modules; }

    public Module getModuleByName(String name) {
        return modules.stream()
                .filter(m -> m.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public void handleKeybinds() {
        if (Module.mc.getWindow() == null) return;

        for (Module module : modules) {
            int key = module.getKey();
            if (key == -1) continue;

            boolean isDown = InputConstants.isKeyDown(Module.mc.getWindow(), key);
            if (module.usesToggleKeybind()) {
                if (isDown && !module.wasKeyDown()) {
                    module.toggle();
                }
            }
            module.setKeyWasDown(isDown);
        }
    }

    public void onTick() {
        modules.stream().filter(Module::isEnabled).forEach(Module::onTick);
    }

    public void onRender(WorldRenderContext context) {
        modules.stream().filter(Module::isEnabled).forEach(module -> module.onRender(context));
    }

    public void onHudRender(GuiGraphics graphics) {
        modules.stream().filter(Module::isEnabled).forEach(module -> module.onHudRender(graphics));
    }

    public void saveConfig() {
        ConfigManager.save(this);
    }

    /** Helper for Mixins to get a typed module reference. */
    @SuppressWarnings("unchecked")
    public <T extends Module> T getModuleByClass(Class<T> clazz) {
        return (T) modules.stream()
                .filter(m -> m.getClass().equals(clazz))
                .findFirst()
                .orElse(null);
    }
}
