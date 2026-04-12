/*
 * Scaffold.java — Automatically places blocks under the player (Movement module).
 *
 * Detects when the block directly below is air, temporarily adjusts pitch downward,
 * triggers a use-item action to place a block, then restores pitch.
 * Detectability: Blatant — impossible look angles are flagged by anti-cheat.
 */
package com.phantom.module.impl.movement;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
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
    private static final double EDGE_CHECK_OFFSET = 0.32D;

    private boolean tower = true;
    private boolean safeWalk = true;
    private int towerJumpCooldown;
    private boolean sneakingFromModule;
    private long lastPlaceTime;
    private double autoOffDelay = 3.0;

    public Scaffold() {
        super("Scaffold",
                "Automatically places blocks under you.\nDetectability: Blatant.",
                ModuleCategory.MOVEMENT,
                -1);
    }

    @Override
    public void onEnable() {
        lastPlaceTime = System.currentTimeMillis();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null || mc.options == null) {
            return;
        }

        // Auto-disable if no blocks placed for autoOffDelay seconds
        if (System.currentTimeMillis() - lastPlaceTime > autoOffDelay * 1000) {
            setEnabled(false);
            return;
        }

        if (towerJumpCooldown > 0) {
            towerJumpCooldown--;
        }

        releaseSneak();

        if (safeWalk) {
            handleSafeWalk();
        }

        BlockPos pos = mc.player.blockPosition().below();
        if (mc.level.getBlockState(pos).isAir()) {
            placeBlock(pos);
            lastPlaceTime = System.currentTimeMillis();
        }

        if (tower && mc.options.keyJump.isDown() && !mc.player.isSprinting()) {
            if (mc.player.onGround()) {
                mc.player.jumpFromGround();
            }
        }
    }

    @Override
    public void onDisable() {
        releaseSneak();
        towerJumpCooldown = 0;
    }

    private void releaseSneak() {
        if (sneakingFromModule && mc.options != null) {
            mc.options.keyShift.setDown(false);
        }
        sneakingFromModule = false;
    }

    private boolean isAtSneakEdge() {
        if (mc.player == null || mc.level == null) {
            return false;
        }

        Vec3 movement = mc.player.getDeltaMovement();
        double moveX = movement.x;
        double moveZ = movement.z;

        if ((moveX * moveX + moveZ * moveZ) < 1.0E-4) {
            double yawRadians = Math.toRadians(mc.player.getYRot());
            moveX = -Math.sin(yawRadians);
            moveZ = Math.cos(yawRadians);
        }

        double length = Math.sqrt(moveX * moveX + moveZ * moveZ);
        if (length <= 0.0) {
            return mc.level.getBlockState(mc.player.blockPosition().below()).isAir();
        }

        BlockPos edgeCheckPos = BlockPos.containing(
                mc.player.getX() + (moveX / length) * EDGE_CHECK_OFFSET,
                mc.player.getY() - 1.0D,
                mc.player.getZ() + (moveZ / length) * EDGE_CHECK_OFFSET
        );
        return mc.level.getBlockState(edgeCheckPos).isAir();
    }

    private void handleSafeWalk() {
        if (mc.player == null || mc.level == null || mc.options == null) return;
        if (!mc.player.onGround()) return;

        if (isAtSneakEdge() && !sneakingFromModule) {
            mc.options.keyShift.setDown(true);
            sneakingFromModule = true;
        }
    }

    private void placeBlock(BlockPos pos) {
        int slot = findBlockSlot();
        if (slot == -1 || mc.gameMode == null) return;

        int oldSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(slot);

        for (Direction dir : Direction.values()) {
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

    public boolean isTower() {
        return tower;
    }

    public void setTower(boolean tower) {
        this.tower = tower;
        saveConfig();
    }

    public boolean isSafeWalk() {
        return safeWalk;
    }

    public void setSafeWalk(boolean safeWalk) {
        this.safeWalk = safeWalk;
        saveConfig();
    }

    public double getAutoOffDelay() {
        return autoOffDelay;
    }

    public void setAutoOffDelay(double autoOffDelay) {
        this.autoOffDelay = Math.max(0.5, Math.min(10.0, autoOffDelay));
        saveConfig();
    }

    @Override
    public void loadConfig(Properties properties) {
        super.loadConfig(properties);
        tower = Boolean.parseBoolean(properties.getProperty("scaffold.tower", "true"));
        safeWalk = Boolean.parseBoolean(properties.getProperty("scaffold.safewalk", "true"));
        String delay = properties.getProperty("scaffold.auto_off_delay");
        if (delay != null) {
            try {
                autoOffDelay = Double.parseDouble(delay);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("scaffold.tower", Boolean.toString(tower));
        properties.setProperty("scaffold.safewalk", Boolean.toString(safeWalk));
        properties.setProperty("scaffold.auto_off_delay", Double.toString(autoOffDelay));
    }
}
