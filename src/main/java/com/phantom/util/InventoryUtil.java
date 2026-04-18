package com.phantom.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.player.Inventory;
import java.lang.reflect.Field;

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
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getSelectedSlot() {
        if (Minecraft.getInstance().player != null && selectedField != null) {
            try {
                return (int) selectedField.get(Minecraft.getInstance().player.getInventory());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    public static void setSelectedSlot(int slot) {
        if (Minecraft.getInstance().player != null && selectedField != null) {
            try {
                selectedField.set(Minecraft.getInstance().player.getInventory(), slot);
                Minecraft.getInstance().getConnection().send(new ServerboundSetCarriedItemPacket(slot));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
