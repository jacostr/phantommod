/*
 * AutoXPThrow.java — Throws all Experience Bottles from your inventory.
 *
 * Throws XP bottles at a configurable speed (bottles per tick).
 * Uses reflection to access the private selected field in Inventory.
 * Detectability: Moderate — rapid slot switching is visible in packets.
 */
package com.phantom.module.impl.player;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class AutoXPThrow extends Module {

    private int throwsPerTick = 5;
    private final List<Integer> pendingSlots = new ArrayList<>();
    private int previousSlot = 0;

    // Cached reflection field for Inventory.selected
    private static Field selectedField = null;

    static {
        try {
            for (Field f : net.minecraft.world.entity.player.Inventory.class.getDeclaredFields()) {
                if (f.getType() == int.class && f.getName().equals("selected")) {
                    f.setAccessible(true);
                    selectedField = f;
                    break;
                }
            }
            // Fallback: try obfuscated field name used in some mappings
            if (selectedField == null) {
                for (Field f : net.minecraft.world.entity.player.Inventory.class.getDeclaredFields()) {
                    // The selected slot field is the only int that starts at 0 and is hotbar-related
                    // Try all int fields and pick the first accessible one named with common patterns
                    if (f.getType() == int.class) {
                        f.setAccessible(true);
                        selectedField = f;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            selectedField = null;
        }
    }

    public AutoXPThrow() {
        super("AutoXPThrow", "Throws all Experience Bottles from your inventory at a configurable speed.\nDetectability: Moderate",
                ModuleCategory.PLAYER, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public String getUsageGuide() {
        return " Only works by binding a key and press it to start throwing all XP bottles in your\n"
             + "inventory. Adjust the speed slider — lower is slower and less\n"
             + "suspicious, higher throws faster. Automatically stops when all\n"
             + "bottles are thrown.\n"
             + "Detectability: Moderate";
    }

    @Override
    public boolean hasConfigurableSettings() {
        return true;
    }

    @Override
    public Screen createSettingsScreen(Screen parent) {
        return new ModuleSettingsScreen(parent, this);
    }

    @Override
    public void onEnable() {
        if (mc.player == null) return;
        previousSlot = getSelected();
        pendingSlots.clear();
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = mc.player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.getItem() == Items.EXPERIENCE_BOTTLE) {
                pendingSlots.add(slot);
            }
        }
        if (pendingSlots.isEmpty()) {
            setEnabledSilently(false);
        }
    }

    @Override
    public void onDisable() {
        if (mc.player == null) return;
        setSelected(previousSlot);
        pendingSlots.clear();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.gameMode == null) return;

        int thrown = 0;
        while (thrown < throwsPerTick && !pendingSlots.isEmpty()) {
            int slot = pendingSlots.get(0);
            ItemStack stack = mc.player.getInventory().getItem(slot);

            if (stack.isEmpty() || stack.getItem() != Items.EXPERIENCE_BOTTLE) {
                pendingSlots.remove(0);
                continue;
            }

            if (slot < 9) {
                setSelected(slot);
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            } else {
                setSelected(8);
                mc.gameMode.handleInventoryMouseClick(
                        mc.player.inventoryMenu.containerId,
                        slot,
                        8,
                        ClickType.SWAP,
                        mc.player);
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            }

            thrown++;

            int checkSlot = slot < 9 ? slot : 8;
            ItemStack after = mc.player.getInventory().getItem(checkSlot);
            if (after.isEmpty() || after.getItem() != Items.EXPERIENCE_BOTTLE) {
                pendingSlots.remove(0);
            }
        }

        if (pendingSlots.isEmpty()) {
            setSelected(previousSlot);
            setEnabledSilently(false);
        }
    }

    private int getSelected() {
        if (mc.player == null) return 0;
        if (selectedField != null) {
            try {
                return selectedField.getInt(mc.player.getInventory());
            } catch (Exception ignored) {}
        }
        return 0;
    }

    private void setSelected(int slot) {
        if (mc.player == null) return;
        if (selectedField != null) {
            try {
                selectedField.setInt(mc.player.getInventory(), slot);
            } catch (Exception ignored) {}
        }
    }

    public int getThrowsPerTick()       { return throwsPerTick; }
    public void setThrowsPerTick(int v) { throwsPerTick = Math.max(1, Math.min(20, v)); saveConfig(); }

    @Override
    public void loadConfig(Properties properties) {
        super.loadConfig(properties);
        try {
            throwsPerTick = Integer.parseInt(properties.getProperty("autoxpthrow.speed", Integer.toString(throwsPerTick)));
        } catch (NumberFormatException ignored) {}
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("autoxpthrow.speed", Integer.toString(throwsPerTick));
    }
}