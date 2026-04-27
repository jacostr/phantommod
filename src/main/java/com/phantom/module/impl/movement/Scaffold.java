package com.phantom.module.impl.movement;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import com.phantom.util.InventoryUtil;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Properties;

public class Scaffold extends Module {
    private boolean safeWalk = true;
    private boolean swingArm = true;
    private int placeDelay = 0;
    private int delayTimer = 0;

    public Scaffold() {
        super("Scaffold",
                "Automatically places blocks under you while you move.\nDetectability: Blatant.",
                ModuleCategory.MOVEMENT,
                -1);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            return;
        }

        if (mc.player.isSpectator()) {
            return;
        }

        if (delayTimer > 0) {
            delayTimer--;
            return;
        }

        BlockPos pos = mc.player.blockPosition().below();
        if (mc.level.getBlockState(pos).isAir()) {
            placeBlock(pos);
            delayTimer = placeDelay;
        }
    }

    private void placeBlock(BlockPos pos) {
        int slot = findBlockSlot();
        if (slot == -1) return;

        int oldSlot = InventoryUtil.getSelectedSlot();
        InventoryUtil.setSelectedSlot(slot);

        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            if (!mc.level.getBlockState(neighbor).isAir()) {
                Direction face = dir.getOpposite();
                Vec3 hitVec = Vec3.atCenterOf(neighbor).add(
                        new Vec3(face.getStepX(), face.getStepY(), face.getStepZ()).scale(0.5));
                BlockHitResult hitResult = new BlockHitResult(hitVec, face, neighbor, false);

                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);
                if (swingArm) mc.player.swing(InteractionHand.MAIN_HAND);
                break;
            }
        }

        InventoryUtil.setSelectedSlot(oldSlot);
    }

    private int findBlockSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean hasConfigurableSettings() {
        return true;
    }

    @Override
    public Screen createSettingsScreen(Screen parent) {
        return new ModuleSettingsScreen(parent, this);
    }

    public boolean isSafeWalk() { return safeWalk; }
    public void setSafeWalk(boolean safeWalk) { this.safeWalk = safeWalk; saveConfig(); }

    public boolean isSwingArm() { return swingArm; }
    public void setSwingArm(boolean swingArm) { this.swingArm = swingArm; saveConfig(); }

    public int getPlaceDelay() { return placeDelay; }
    public void setPlaceDelay(int placeDelay) { this.placeDelay = placeDelay; saveConfig(); }

    @Override
    public void loadConfig(Properties p) {
        super.loadConfig(p);
        safeWalk = Boolean.parseBoolean(p.getProperty("scaffold.safewalk", "true"));
        swingArm = Boolean.parseBoolean(p.getProperty("scaffold.swingarm", "true"));
        placeDelay = Integer.parseInt(p.getProperty("scaffold.placedelay", "0"));
    }

    @Override
    public void saveConfig(Properties p) {
        super.saveConfig(p);
        p.setProperty("scaffold.safewalk", Boolean.toString(safeWalk));
        p.setProperty("scaffold.swingarm", Boolean.toString(swingArm));
        p.setProperty("scaffold.placedelay", Integer.toString(placeDelay));
    }
}
