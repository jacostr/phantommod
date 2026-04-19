package com.phantom;

import com.phantom.gui.ClickGUIScreen;
import com.phantom.gui.NotificationManager;
import com.phantom.module.ModuleManager;
import com.phantom.module.impl.movement.SpeedBridge;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * PhantomMod — Core entrypoint and event dispatcher.
 *
 * <p>Initializes the {@link ModuleManager} which handles individual feature logic,
 * registers the global toggle keys (ClickGUI on M), and hooks into
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
                client.setScreen(new ClickGUIScreen());
            }
        });

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (moduleManager != null) moduleManager.onRender(context);
        });

        HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
            if (moduleManager != null) {
                moduleManager.onHudRender(graphics);
            }
            NotificationManager.render(graphics);
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, cmdEnv) -> {
            dispatcher.register(ClientCommandManager.literal("bridge")
                .then(ClientCommandManager.literal("legit")
                    .executes(ctx -> {
                        SpeedBridge bridge = moduleManager.getModuleByClass(SpeedBridge.class);
                        if (bridge != null) {
                            bridge.applyPresetLegit();
                            ctx.getSource().sendFeedback(Component.literal("[SpeedBridge] Legit preset applied"));
                        }
                        return 1;
                    }))
                .then(ClientCommandManager.literal("normal")
                    .executes(ctx -> {
                        SpeedBridge bridge = moduleManager.getModuleByClass(SpeedBridge.class);
                        if (bridge != null) {
                            bridge.applyPresetNormal();
                            ctx.getSource().sendFeedback(Component.literal("[SpeedBridge] Normal preset applied"));
                        }
                        return 1;
                    }))
                .then(ClientCommandManager.literal("obvious")
                    .executes(ctx -> {
                        SpeedBridge bridge = moduleManager.getModuleByClass(SpeedBridge.class);
                        if (bridge != null) {
                            bridge.applyPresetObvious();
                            ctx.getSource().sendFeedback(Component.literal("[SpeedBridge] Obvious preset applied"));
                        }
                        return 1;
                    }))
                .then(ClientCommandManager.literal("blatant")
                    .executes(ctx -> {
                        SpeedBridge bridge = moduleManager.getModuleByClass(SpeedBridge.class);
                        if (bridge != null) {
                            bridge.applyPresetBlatant();
                            ctx.getSource().sendFeedback(Component.literal("[SpeedBridge] Blatant preset applied"));
                        }
                        return 1;
                    }))
                .then(ClientCommandManager.literal("tower")
                    .executes(ctx -> {
                        SpeedBridge bridge = moduleManager.getModuleByClass(SpeedBridge.class);
                        if (bridge != null) {
                            bridge.setTowerModeEnabled(true);
                            ctx.getSource().sendFeedback(Component.literal("[SpeedBridge] Tower mode enabled"));
                        }
                        return 1;
                    }))
                .then(ClientCommandManager.literal("flat")
                    .executes(ctx -> {
                        SpeedBridge bridge = moduleManager.getModuleByClass(SpeedBridge.class);
                        if (bridge != null) {
                            bridge.setTowerModeEnabled(false);
                            ctx.getSource().sendFeedback(Component.literal("[SpeedBridge] Flat bridge mode enabled"));
                        }
                        return 1;
                    }))
                .then(ClientCommandManager.literal("toggle")
                    .executes(ctx -> {
                        SpeedBridge bridge = moduleManager.getModuleByClass(SpeedBridge.class);
                        if (bridge != null) {
                            bridge.toggle();
                            boolean enabled = bridge.isEnabled();
                            ctx.getSource().sendFeedback(Component.literal("[SpeedBridge] " + (enabled ? "Enabled" : "Disabled")));
                        }
                        return 1;
                    }))
                .then(ClientCommandManager.literal("status")
                    .executes(ctx -> {
                        SpeedBridge bridge = moduleManager.getModuleByClass(SpeedBridge.class);
                        if (bridge != null) {
                            String status = String.format("[SpeedBridge] Preset: %d | Tower: %s | PlaceDelay: %d",
                                bridge.getPreset(),
                                bridge.isTowerModeEnabled() ? "ON" : "OFF",
                                bridge.getScaffoldPlaceDelay());
                            ctx.getSource().sendFeedback(Component.literal(status));
                        }
                        return 1;
                    }))
            );
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

    public static void resetConfig() {
        if (moduleManager != null) {
            moduleManager.getModules().forEach(m -> {
                if (m.isEnabled()) m.disableSilently();
            });
        }
        java.io.File file = new java.io.File(net.minecraft.client.Minecraft.getInstance().gameDirectory, "config/phantom-memory.properties");
        if (file.exists()) {
            file.delete();
        }
        moduleManager = new ModuleManager();
    }
}
