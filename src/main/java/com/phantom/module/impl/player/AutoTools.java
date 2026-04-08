/*
 * AutoTools.java — Automatically selects the best tool or weapon from the hotbar (Player module).
 *
 * Each tick, reads mc.hitResult. For blocks, compares getDestroySpeed() across all hotbar
 * slots and switches to the fastest. For entities, scores by weapon type
 * (sword > mace > axe > trident).
 * Detectability: Safe — tool swapping is normal player behaviour.
 */
package com.phantom.module.impl.player;

import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import java.util.Locale;

public class AutoTools extends Module {
    public AutoTools() {
        super("AutoTools", "Automatically switches to the best tool for mining a block.\nDetectability: Safe", ModuleCategory.PLAYER, -1);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null || mc.hitResult == null || mc.gameMode == null) return;
        if (mc.screen != null || !mc.options.keyAttack.isDown()) return;

        // Weapon swap logic
        if (mc.hitResult instanceof EntityHitResult entityHitResult &&
                entityHitResult.getEntity() instanceof LivingEntity livingEntity &&
                livingEntity.isAlive()) {
            int bestWeaponSlot = findBestWeaponSlot();
            if (bestWeaponSlot != -1 && bestWeaponSlot != mc.player.getInventory().getSelectedSlot()) {
                mc.player.getInventory().setSelectedSlot(bestWeaponSlot);
            }
            return;
        }

        // Tool swap logic
        if (!(mc.hitResult instanceof BlockHitResult blockHitResult)) return;
        
        BlockState blockState = mc.level.getBlockState(blockHitResult.getBlockPos());
        if (blockState.isAir()) return;

        int bestSlot = mc.player.getInventory().getSelectedSlot();
        float bestSpeed = getToolScore(bestSlot, blockState);

        for (int slot = 0; slot < 9; slot++) {
            float speed = getToolScore(slot, blockState);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = slot;
            }
        }

        if (bestSlot != mc.player.getInventory().getSelectedSlot()) {
            mc.player.getInventory().setSelectedSlot(bestSlot);
        }
    }

    private int findBestWeaponSlot() {
        int bestSlot = -1;
        float bestScore = Float.NEGATIVE_INFINITY;

        for (int slot = 0; slot < 9; slot++) {
            var stack = mc.player.getInventory().getItem(slot);
            if (stack.isEmpty()) continue;

            float score = 0.0F;
            // Using string check for 1.21.11 mapping stability
            String itemName = stack.getItem().toString().toLowerCase(Locale.ROOT);

            if (itemName.contains("sword")) {
                score += 1000.0F;
            } else if (itemName.contains("_axe")) { // Matches iron_axe but not pickaxe
                score += 500.0F;
            } else if (itemName.contains("trident")) {
                score += 300.0F;
            } else if (itemName.contains("mace")) {
                score += 800.0F;
            }

            // Fallback: check for attack damage component
            if (stack.get(DataComponents.ATTRIBUTE_MODIFIERS) != null) {
                score += 10.0F;
            }

            if (score > bestScore) {
                bestScore = score;
                bestSlot = slot;
            }
        }

        return bestSlot;
    }

    private float getToolScore(int slot, BlockState blockState) {
        var stack = mc.player.getInventory().getItem(slot);
        float speed = stack.getDestroySpeed(blockState);

        if (stack.isCorrectToolForDrops(blockState)) {
            speed += 1000.0F;
        }

        return speed;
    }
}
