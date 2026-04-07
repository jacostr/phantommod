package com.phantom.module;

import com.phantom.module.impl.movement.AlwaysSprint;
import com.phantom.module.impl.movement.SpeedBridge;
import com.phantom.module.impl.player.AutoTools;
import com.phantom.module.impl.render.ESP;
import com.phantom.module.impl.render.FullBright;
import com.phantom.module.impl.render.HudModule;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.gui.GuiGraphics;

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
        modules.add(new SpeedBridge());
        modules.add(new AutoTools());
        modules.add(hudModule);
        modules.sort(Comparator.comparing((Module module) -> module.getCategory().ordinal())
                .thenComparing(Module::getName));
        hudModule.initializeEnabledSilently(true);
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
            if (isDown && !module.wasKeyDown()) {
                module.toggle();
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
}
