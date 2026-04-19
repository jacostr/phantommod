/*
 * Module.java — Abstract base class that every PhantomMod feature extends.
 *
 * Provides the lifecycle hooks (onEnable/onDisable/onTick/onRender/onHudRender),
 * hotkey management, enable/disable toggling with notification toasts, and config
 * persistence (loadConfig/saveConfig via Properties). ModuleManager calls these
 * methods each tick to drive all module logic.
 *
 * Subclasses only need to override the hooks they care about — everything else
 * has sensible defaults (no-op methods, toggle-style keybind, etc.).
 */
package com.phantom.module;

import com.phantom.PhantomMod;
import com.phantom.gui.NotificationManager;
import com.phantom.gui.ModuleSettingsScreen;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

public abstract class Module {
    private final String name;
    private final String description;
    private final ModuleCategory category;
    private int key;
    private boolean enabled;
    private boolean keyWasDown;
    protected static final Minecraft mc = Minecraft.getInstance();

    public Module(String name, String description, ModuleCategory category, int key) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.key = key;
        this.enabled = false;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /** Longer usage text for the all-modules help screen; defaults to the short description. */
    public String getUsageGuide() {
        return description;
    }

    public ModuleCategory getCategory() {
        return category;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
        saveConfig();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean wasKeyDown() {
        return keyWasDown;
    }

    public void setKeyWasDown(boolean keyWasDown) {
        this.keyWasDown = keyWasDown;
    }

    public void initializeEnabledSilently(boolean enabled) {
        this.enabled = enabled;
    }

    public void setEnabledSilently(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }

        this.enabled = enabled;
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }

        this.enabled = enabled;
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }

        NotificationManager.push(name + (enabled ? " enabled" : " disabled"));
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    /** Disable without toast or {@link #saveConfig()}. */
    public void disableSilently() {
        if (!enabled) {
            return;
        }
        enabled = false;
        onDisable();
    }

    /**
     * When false, the bound key is hold-to-use instead of edge-toggle in {@link ModuleManager#handleKeybinds()}.
     */
    public boolean usesToggleKeybind() {
        return true;
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public void onTick() {
    }

    public void onEntityEvent(ClientboundEntityEventPacket packet) {
    }

    public void onRender(WorldRenderContext context) {
    }

    public void onHudRender(GuiGraphics graphics) {
    }

    public void onChat(String message) {
    }

    public boolean hasSettings() {
        return true;
    }

    public boolean hasConfigurableSettings() {
        return true;
    }

    public String getSettingsButtonLabel() {
        return "Opt";
    }

    public Screen createSettingsScreen(Screen parent) {
        return new ModuleSettingsScreen(parent, this);
    }

    public void loadConfig(Properties properties) {
        String id = getConfigId();
        
        String savedKey = properties.getProperty(id + ".key");
        if (savedKey != null) {
            try {
                this.key = Integer.parseInt(savedKey);
            } catch (NumberFormatException ignored) {
            }
        }

        String savedEnabled = properties.getProperty(id + ".enabled");
        if (savedEnabled != null) {
            this.enabled = Boolean.parseBoolean(savedEnabled);
        }
    }

    public void saveConfig(Properties properties) {
        String id = getConfigId();
        properties.setProperty(id + ".key", Integer.toString(key));
        properties.setProperty(id + ".enabled", Boolean.toString(enabled));
    }

    protected void saveConfig() {
        PhantomMod.saveConfig();
    }

    private String getConfigId() {
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
    }

    /**
     * After config load, sync runtime side effects with the loaded {@code enabled} flag
     * (load does not call {@link #onEnable()} or {@link #onDisable()}).
     */
    public void applyLoadedEnableState() {
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    protected boolean shouldPauseForBedMining() {
        if (mc.level == null || mc.player == null || mc.options == null || !mc.options.keyAttack.isDown()) {
            return false;
        }
        if (!(mc.hitResult instanceof BlockHitResult blockHitResult)) {
            return false;
        }
        return mc.level.getBlockState(blockHitResult.getBlockPos()).getBlock() instanceof BedBlock;
    }

    protected boolean isTeammateTarget(Entity entity) {
        if (!(entity instanceof Player other) || mc.player == null || entity == mc.player) {
            return false;
        }

        if (mc.player.isAlliedTo(other) || other.isAlliedTo(mc.player)) {
            return true;
        }

        if (mc.player.getTeam() != null && other.getTeam() != null
                && Objects.equals(mc.player.getTeam().getName(), other.getTeam().getName())) {
            return true;
        }

        Integer selfArmor = getPrimaryArmorColor(mc.player);
        Integer otherArmor = getPrimaryArmorColor(other);
        return selfArmor != null && selfArmor.equals(otherArmor);
    }

    private Integer getPrimaryArmorColor(Player player) {
        int[] armorSlots = {36, 37, 38, 39};
        for (int slot : armorSlots) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            DyedItemColor dyed = stack.get(DataComponents.DYED_COLOR);
            if (dyed != null) {
                return dyed.rgb();
            }
        }
        return null;
    }
}
