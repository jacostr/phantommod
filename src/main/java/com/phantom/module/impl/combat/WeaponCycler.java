/*
 * WeaponCycler.java — Automatically switches weapons after attacking.
 */
package com.phantom.module.impl.combat;

import com.phantom.PhantomMod;
import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import com.phantom.module.impl.player.AutoTools;
import com.phantom.util.InventoryUtil;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import java.util.Properties;
import java.util.Locale;

public class WeaponCycler extends Module {

    public enum WeaponCombo {
        SWORD_AXE("Sword + Axe"),
        SWORD_MACE("Sword + Mace"),
        AXE_MACE("Axe + Mace"),
        ALL("All Weapons");

        private final String label;

        WeaponCombo(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private long lastSwapTime = 0;
    private int pendingSwapTicks = 0;
    private boolean hasPendingSwap = false;
    private int targetSlot = -1;
    private int enforceSlotTicks = 0;
    private WeaponCombo weaponCombo = WeaponCombo.SWORD_AXE;

    public WeaponCycler() {
        super("WeaponCycler",
                "Automatically cycles to your next weapon after attacking.\nDisables AutoTools while active.\nDetectability: Blatant",
                ModuleCategory.COMBAT, -1);
    }

    @Override
    public void onEnable() {
        try {
            AutoTools autoTools = PhantomMod.getModuleManager().getModuleByClass(AutoTools.class);
            if (autoTools != null && autoTools.isEnabled()) {
                autoTools.setEnabled(false);
            }
        } catch (Exception ignored) {
        }
    }

    public void onAttack(Entity target) {
        if (!isEnabled())
            return;
        if (mc.player == null)
            return;

        long now = System.currentTimeMillis();
        if (now - lastSwapTime < 200)
            return;

        hasPendingSwap = true;
        pendingSwapTicks = 3;
        lastSwapTime = now;
    }

    @Override
    public void onTick() {
        if (!isEnabled() || mc.player == null)
            return;

        if (enforceSlotTicks > 0) {
            enforceSlotTicks--;
            if (InventoryUtil.getSelectedSlot() != targetSlot) {
                InventoryUtil.setSelectedSlot(targetSlot);
            }
            return;
        }

        if (!hasPendingSwap)
            return;

        if (pendingSwapTicks > 0) {
            pendingSwapTicks--;
            return;
        }

        int slot = findNextWeaponSlot();
        if (slot != -1) {
            targetSlot = slot;
            enforceSlotTicks = 6;
            InventoryUtil.setSelectedSlot(slot);
        }
        hasPendingSwap = false;
    }

    private int findNextWeaponSlot() {
        if (mc.player == null)
            return -1;
        int currentSlot = InventoryUtil.getSelectedSlot();
        for (int i = 1; i < 9; i++) {
            int checkSlot = (currentSlot + i) % 9;
            ItemStack stack = mc.player.getInventory().getItem(checkSlot);
            if (!stack.isEmpty() && isValidWeapon(stack))
                return checkSlot;
        }
        return -1;
    }

    private boolean isValidWeapon(ItemStack stack) {
        String id = stack.getItem().getDescriptionId().toLowerCase(Locale.ROOT);
        boolean isSword = id.contains("sword");
        boolean isAxe = id.contains("_axe");
        boolean isMace = id.contains("mace");
        switch (weaponCombo) {
            case SWORD_AXE:
                return isSword || isAxe;
            case SWORD_MACE:
                return isSword || isMace;
            case AXE_MACE:
                return isAxe || isMace;
            case ALL:
                return isSword || isAxe || isMace;
            default:
                return false;
        }
    }

    public void cycleWeaponCombo() {
        WeaponCombo[] values = WeaponCombo.values();
        weaponCombo = values[(weaponCombo.ordinal() + 1) % values.length];
        saveConfig();
    }

    public WeaponCombo getWeaponCombo() {
        return weaponCombo;
    }

    public void setWeaponCombo(WeaponCombo v) {
        weaponCombo = v;
        saveConfig();
    }

    @Override
    public void onDisable() {
        hasPendingSwap = false;
        pendingSwapTicks = 0;
        targetSlot = -1;
        enforceSlotTicks = 0;
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
    public void loadConfig(Properties p) {
        super.loadConfig(p);
        try {
            weaponCombo = WeaponCombo.valueOf(p.getProperty("weaponcycler.combo", "SWORD_AXE"));
        } catch (Exception ignored) {
        }
    }

    @Override
    public void saveConfig(Properties p) {
        super.saveConfig(p);
        p.setProperty("weaponcycler.combo", weaponCombo.name());
    }
}