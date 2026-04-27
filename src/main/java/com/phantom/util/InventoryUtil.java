package com.phantom.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.player.Inventory;
import java.lang.reflect.Field;

public class InventoryUtil {

    private static Field selectedField;

    static {
        try {
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
        } catch (Exception ignored) {}
    }

    public static int getSelectedSlot() {
        if (selectedField == null) {
            return 0;
        }
        if (Minecraft.getInstance().player != null) {
            try {
                return (int) selectedField.get(Minecraft.getInstance().player.getInventory());
            } catch (Exception ignored) {}
        }
        return 0;
    }

    public static void setSelectedSlot(int slot) {
        if (selectedField == null) {
            return;
        }
        if (Minecraft.getInstance().player != null) {
            try {
                selectedField.set(Minecraft.getInstance().player.getInventory(), slot);
                Minecraft.getInstance().getConnection().send(new ServerboundSetCarriedItemPacket(slot));
            } catch (Exception ignored) {}
        }
    }
}
