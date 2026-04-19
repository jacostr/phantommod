package com.phantom.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.player.Inventory;
import java.lang.reflect.Field;

import static com.phantom.util.Logger.*;

public class InventoryUtil {

    private static Field selectedField;

    static {
        try {
            // Support Mojang mappings, common fallbacks, and Fabric Intermediary mappings
            String[] fieldNames = {"selected", "selectedSlot", "field_7545"};
            for (String name : fieldNames) {
                try {
                    selectedField = Inventory.class.getDeclaredField(name);
                    break;
                } catch (NoSuchFieldException ignored) {}
            }
            if (selectedField != null) {
                selectedField.setAccessible(true);
            } else {
                Logger.warn("InventoryUtil: No selected field found, slot switching may not work");
            }
        } catch (Exception e) {
            Logger.error("InventoryUtil: Failed to init", e);
        }
    }

    public static int getSelectedSlot() {
        if (selectedField == null) {
            Logger.warn("InventoryUtil: selectedField is null");
            return 0;
        }
        if (Minecraft.getInstance().player != null) {
            try {
                return (int) selectedField.get(Minecraft.getInstance().player.getInventory());
            } catch (Exception e) {
                Logger.error("InventoryUtil: Failed to get selected slot", e);
            }
        }
        return 0;
    }

    public static void setSelectedSlot(int slot) {
        if (selectedField == null) {
            Logger.warn("InventoryUtil: selectedField is null, cannot set slot " + slot);
            return;
        }
        if (Minecraft.getInstance().player != null) {
            try {
                selectedField.set(Minecraft.getInstance().player.getInventory(), slot);
                Minecraft.getInstance().getConnection().send(new ServerboundSetCarriedItemPacket(slot));
            } catch (Exception e) {
                Logger.error("InventoryUtil: Failed to set selected slot to " + slot, e);
            }
        }
    }
}
