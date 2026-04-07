/* 
 * Ported from Meteor Client concept 
 * Source: https://github.com/MeteorDevelopment/meteor-client/blob/master/src/main/java/meteordevelopment/meteorclient/systems/modules/movement/Scaffold.java
 */
package com.phantom.module.impl.movement;

import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.gui.screens.Screen;
import com.phantom.gui.ModuleSettingsScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class Scaffold extends Module {
    private boolean tower = true;

    public Scaffold() {
        super("Scaffold", "Automatically places blocks under your feet as you move.", ModuleCategory.BLATANT, -1);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) return;

        BlockPos pos = mc.player.blockPosition().below();
        if (mc.level.getBlockState(pos).isAir()) {
            placeBlock(pos);
        }

        if (tower && mc.options.keyJump.isDown() && !mc.player.isSprinting()) {
            // Basic tower logic
            if (mc.player.onGround()) {
                mc.player.jumpFromGround();
            }
        }
    }

    private void placeBlock(BlockPos pos) {
        int slot = findBlockSlot();
        if (slot == -1) return;

        int oldSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(slot);

        // Find a neighbor to place against
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            if (!mc.level.getBlockState(neighbor).isAir()) {
                Direction face = dir.getOpposite();
                Vec3 hitVec = Vec3.atCenterOf(neighbor).add(
                        new Vec3(face.getStepX(), face.getStepY(), face.getStepZ()).scale(0.5));
                BlockHitResult hitResult = new BlockHitResult(hitVec, face, neighbor, false);
                
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);
                mc.player.swing(InteractionHand.MAIN_HAND);
                break;
            }
        }

        mc.player.getInventory().setSelectedSlot(oldSlot);
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

    public boolean isTower() { return tower; }
    public void setTower(boolean tower) { this.tower = tower; saveConfig(); }
}
