package com.phantom;

import com.phantom.gui.ClickGUIScreen;
import com.phantom.gui.NotificationManager;
import com.phantom.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * PhantomMod — Core entrypoint and event dispatcher.
 *
 * <p>Initializes the {@link ModuleManager} which handles individual feature logic,
 * registers the global toggle keys (ClickGUI on Right Shift), and hooks into
 * Fabric API's render and tick events to drive the mod's lifecycle.</p>
 */
public final class PhantomMod implements ClientModInitializer {
    public static final String MOD_ID = "phantom";
    private static final KeyMapping.Category KEY_CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
    private static ModuleManager moduleManager;
    private static KeyMapping guiKey;

    @Override
    public void onInitializeClient() {
        moduleManager = new ModuleManager();

        guiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.phantom.gui",
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                KEY_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            moduleManager.handleKeybinds();
            moduleManager.onTick();

            if (guiKey.consumeClick()) {
                client.setScreen(new ClickGUIScreen(moduleManager));
            }
        });

        WorldRenderEvents.AFTER_ENTITIES.register(moduleManager::onRender);

        HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
            moduleManager.onHudRender(graphics);
            NotificationManager.render(graphics);
        });
    }

    /**
     * Entry point for modules to trigger a global configuration save.
     */
    public static void saveConfig() {
        if (moduleManager != null) {
            moduleManager.saveConfig();
        }
    }

    public static ModuleManager getModuleManager() {
        return moduleManager;
    }
}
