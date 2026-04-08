/*
 * Ghost: while holding attack, swaps hotbar slot to best weapon vs entities or best tool vs targeted block.
 */
package com.phantom.module.impl.player;

import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.lwjgl.glfw.GLFW;

public class AutoTools extends Module {
    public AutoTools() {
        super("AutoTools", "Swaps to the best hotbar tool for mining and prefers weapon slots when you attack an entity.", ModuleCategory.GHOST, -1);
    }

    // Main routing:
    // - attacking a living entity -> prefer a weapon slot
    // - mining a block -> prefer the best matching tool slot
    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null || mc.hitResult == null) return;
        if (mc.screen != null || !mc.options.keyAttack.isDown()) return;

        if (mc.hitResult instanceof EntityHitResult entityHitResult &&
                entityHitResult.getEntity() instanceof LivingEntity livingEntity &&
                livingEntity.isAlive()) {
            int bestWeaponSlot = findBestWeaponSlot();
            if (bestWeaponSlot != -1 && bestWeaponSlot != mc.player.getInventory().getSelectedSlot()) {
                mc.player.getInventory().setSelectedSlot(bestWeaponSlot);
            }
            return;
        }

        if (!(mc.hitResult instanceof BlockHitResult blockHitResult)) return;

        BlockState blockState = mc.level.getBlockState(blockHitResult.getBlockPos());
        int bestSlot = mc.player.getInventory().getSelectedSlot();
        float bestSpeed = getToolScore(bestSlot, blockState);

        // Only scan the hotbar so tool swaps stay subtle and easy to follow.
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

    // Score only the hotbar so swaps stay subtle. Weapon data components are a clean way
    // to prefer swords and other combat items over pickaxes while fighting.
    private int findBestWeaponSlot() {
        int bestSlot = -1;
        float bestScore = Float.NEGATIVE_INFINITY;

        for (int slot = 0; slot < 9; slot++) {
            var stack = mc.player.getInventory().getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            float score = 0.0F;

            if (stack.has(DataComponents.WEAPON)) {
                score += 1000.0F;
            }

            if (stack.has(DataComponents.PIERCING_WEAPON)) {
                score += 50.0F;
            }

            if (stack.has(DataComponents.KINETIC_WEAPON)) {
                score += 25.0F;
            }

            if (score > bestScore) {
                bestScore = score;
                bestSlot = slot;
            }
        }

        return bestSlot;
    }

    // Tool selection is mostly destroy speed, but correct-drop tools get a huge bonus so
    // the module does not choose a "fast enough" wrong item over the proper one.
    private float getToolScore(int slot, BlockState blockState) {
        float speed = mc.player.getInventory().getItem(slot).getDestroySpeed(blockState);

        // Prefer the correct tool, then use destroy speed as the secondary signal.
        if (mc.player.getInventory().getItem(slot).isCorrectToolForDrops(blockState)) {
            speed += 1000.0F;
        }

        return speed;
    }
}
