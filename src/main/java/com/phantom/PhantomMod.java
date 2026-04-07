package com.phantom;

import com.phantom.gui.ClickGUIScreen;
import com.phantom.gui.NotificationManager;
import com.phantom.module.ModuleManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class PhantomMod implements ClientModInitializer {
    public static final String MOD_ID = "phantom";
    private static ModuleManager moduleManager;
    private static KeyMapping guiKey;

    @Override
    public void onInitializeClient() {
        moduleManager = new ModuleManager();

        KeyMapping.Category phantomCategory = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "general")
        );

        guiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.phantom.gui",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                phantomCategory
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            if (guiKey.consumeClick()) {
                client.setScreen(new ClickGUIScreen(moduleManager));
            }

            moduleManager.handleKeybinds();
            moduleManager.onTick();
        });

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            moduleManager.onRender(context);
        });

        HudRenderCallback.EVENT.register((graphics, tickCounter) -> {
            moduleManager.onHudRender(graphics);
            NotificationManager.render(graphics);
        });
    }

    public static ModuleManager getModuleManager() {
        return moduleManager;
    }

    public static void saveConfig() {
        if (moduleManager != null) {
            moduleManager.saveConfig();
        }
    }
}
