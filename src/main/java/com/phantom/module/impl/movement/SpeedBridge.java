/*
 * SpeedBridge.java — Edge-detection bridging assist with auto block refill (Movement module).
 *
 * Captures bridge direction on enable. Each tick, checks if the player is hanging over
 * air and auto-sneaks to prevent falling. When the current block stack is depleted,
 * scans the hotbar for the next BlockItem and auto-swaps to it.
 * Detectability: Safe/Subtle — only sneak timing is automated; placement is manual.
 */
package com.phantom.module.impl.movement;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.mixin.MinecraftClientAccessor;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Properties;

public class SpeedBridge extends Module {
    private static final double EDGE_CHECK_OFFSET = 0.32D;
    private static final int JUMP_SNEAK_TICKS = 4;
    private static final int UNSNEAK_GRACE_TICKS = 2;

    private long lastPlaceTime;
    private double autoOffDelay = 3.0;
    private boolean sneakingFromModule;
    private int delayTicks = 3;
    private boolean blocksOnly = true;
    private boolean sneakOnJump = true;
    private int jumpSneakTicks;
    private int unsneakGraceTicks;
    private boolean wasOnGround;

    public SpeedBridge() {
        super(
                "SpeedBridge Assist",
                "Re-sneaks at the edge of blocks automatically. Face away from the void, hold right click and place buttons, then hold s, it will sneak for you. Combine with SafeWalk for best results!\nDetectability: Safe/Subtle",
                ModuleCategory.MOVEMENT,
                -1);
    }

    @Override
    public void onEnable() {
        if (mc.player == null) {
            setEnabled(false);
            return;
        }

        lastPlaceTime = System.currentTimeMillis();
        jumpSneakTicks = 0;
        unsneakGraceTicks = 0;
        wasOnGround = mc.player.onGround();
    }

    @Override
    public void onDisable() {
        releaseSneak();
    }

    @Override
    public void onTick() {
        if (mc.level == null || mc.player == null || mc.options == null)
            return;

        if (!(mc.player.getMainHandItem().getItem() instanceof BlockItem)) {
            int nextSlot = findNextBlockSlot();
            if (nextSlot != -1) {
                mc.player.getInventory().setSelectedSlot(nextSlot);
            } else {
                releaseSneak();
                checkAutoDisable();
                return;
            }
        }

        if (mc.player.getMainHandItem().isEmpty()) {
            int nextSlot = findNextBlockSlot();
            if (nextSlot != -1) {
                mc.player.getInventory().setSelectedSlot(nextSlot);
            } else {
                releaseSneak();
                checkAutoDisable();
                return;
            }
        }

        if (mc.options.keyUse.isDown()) {
            lastPlaceTime = System.currentTimeMillis();
            detectJumpSneakWindow();
            applyFastPlace();
        }

        if (checkAutoDisable()) return;

        boolean onGround = mc.player.onGround();
        if (!onGround && jumpSneakTicks > 0) {
            jumpSneakTicks--;
        }

        boolean shouldSneak = (onGround && isAtEdge()) || shouldSneakOnJump();
        if (shouldSneak) {
            unsneakGraceTicks = UNSNEAK_GRACE_TICKS;
        } else if (unsneakGraceTicks > 0) {
            unsneakGraceTicks--;
        }

        updateSneakState(shouldSneak || unsneakGraceTicks > 0);
        wasOnGround = onGround;
    }

    private boolean checkAutoDisable() {
        if (System.currentTimeMillis() - lastPlaceTime > autoOffDelay * 1000) {
            setEnabled(false);
            return true;
        }
        return false;
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
        String v = properties.getProperty("speedbridge.auto_off_delay");
        if (v != null) {
            try {
                autoOffDelay = Double.parseDouble(v);
            } catch (Exception ignored) {}
        }
        String delay = properties.getProperty("speedbridge.delay_ticks");
        if (delay != null) {
            try {
                delayTicks = Math.max(0, Math.min(4, Integer.parseInt(delay.trim())));
            } catch (NumberFormatException ignored) {
            }
        }
        blocksOnly = Boolean.parseBoolean(properties.getProperty("speedbridge.blocks_only", Boolean.toString(blocksOnly)));
        sneakOnJump = Boolean.parseBoolean(properties.getProperty("speedbridge.sneak_on_jump", Boolean.toString(sneakOnJump)));
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("speedbridge.auto_off_delay", Double.toString(autoOffDelay));
        properties.setProperty("speedbridge.delay_ticks", Integer.toString(delayTicks));
        properties.setProperty("speedbridge.blocks_only", Boolean.toString(blocksOnly));
        properties.setProperty("speedbridge.sneak_on_jump", Boolean.toString(sneakOnJump));
    }

    public int getDelayTicks() {
        return delayTicks;
    }

    public void setDelayTicks(int delayTicks) {
        this.delayTicks = Math.max(0, Math.min(4, delayTicks));
        saveConfig();
    }

    public boolean isBlocksOnly() {
        return blocksOnly;
    }

    public void setBlocksOnly(boolean blocksOnly) {
        this.blocksOnly = blocksOnly;
        saveConfig();
    }

    public boolean isSneakOnJump() {
        return sneakOnJump;
    }

    public void setSneakOnJump(boolean sneakOnJump) {
        this.sneakOnJump = sneakOnJump;
        saveConfig();
    }

    public void applyPresetLegit() {
        setDelayTicks(3);
        setBlocksOnly(true);
    }

    public void applyPresetNormal() {
        setDelayTicks(2);
        setBlocksOnly(true);
    }

    public void applyPresetObvious() {
        setDelayTicks(1);
        setBlocksOnly(true);
    }

    public void applyPresetBlatant() {
        setDelayTicks(0);
        setBlocksOnly(false);
    }

    /**
     * Scans hotbar slots (0-8) for the first slot that has a block item with at
     * least 1 count. Returns -1 if no blocks are found.
     */
    private int findNextBlockSlot() {
        if (mc.player == null) return -1;
        int current = mc.player.getInventory().getSelectedSlot();
        for (int offset = 1; offset <= 9; offset++) {
            int slot = (current + offset) % 9;
            ItemStack stack = mc.player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
                return slot;
            }
        }
        return -1;
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    public Screen createSettingsScreen(Screen parent) {
        return new ModuleSettingsScreen(parent, this);
    }

    private boolean isAtEdge() {
        Vec3 movement = mc.player.getDeltaMovement();
        double moveX = movement.x;
        double moveZ = movement.z;

        if ((moveX * moveX + moveZ * moveZ) < 1.0E-4) {
            double yawRadians = Math.toRadians(mc.player.getYRot());
            moveX = -Math.sin(yawRadians);
            moveZ = Math.cos(yawRadians);
        }

        double length = Math.sqrt(moveX * moveX + moveZ * moveZ);
        if (length <= 0.0D) {
            return false;
        }

        BlockPos edgeCheckPos = BlockPos.containing(
                mc.player.getX() + (moveX / length) * EDGE_CHECK_OFFSET,
                mc.player.getY() - 1.0D,
                mc.player.getZ() + (moveZ / length) * EDGE_CHECK_OFFSET);
        return mc.level.getBlockState(edgeCheckPos).isAir();
    }

    private void applyFastPlace() {
        if (mc.player == null || mc.options == null || mc.screen != null) {
            return;
        }

        if (blocksOnly && !(mc.player.getMainHandItem().getItem() instanceof BlockItem)
                && !(mc.player.getOffhandItem().getItem() instanceof BlockItem)) {
            return;
        }

        if (shouldDelayPlacementForJump()) {
            return;
        }

        MinecraftClientAccessor accessor = (MinecraftClientAccessor) mc;
        if (accessor.phantom$getRightClickDelay() > delayTicks) {
            accessor.phantom$setRightClickDelay(delayTicks);
        }
    }

    private void detectJumpSneakWindow() {
        if (!sneakOnJump || mc.player == null) {
            return;
        }
        boolean jumpedThisTick = wasOnGround
                && !mc.player.onGround()
                && mc.player.getDeltaMovement().y > 0.15D;
        if (jumpedThisTick) {
            jumpSneakTicks = JUMP_SNEAK_TICKS;
            unsneakGraceTicks = Math.max(unsneakGraceTicks, UNSNEAK_GRACE_TICKS);
        }
    }

    private boolean shouldSneakOnJump() {
        return sneakOnJump
                && jumpSneakTicks > 0
                && !mc.player.onGround()
                && mc.options.keyUse.isDown();
    }

    private boolean shouldDelayPlacementForJump() {
        if (!sneakOnJump || mc.player == null || !mc.options.keyUse.isDown()) {
            return false;
        }
        if (mc.player.onGround() || jumpSneakTicks <= 0) {
            return false;
        }
        return mc.player.getDeltaMovement().y > -0.02D;
    }

    private void updateSneakState(boolean overEdge) {
        mc.options.keyShift.setDown(overEdge);
        sneakingFromModule = overEdge;
    }

    private void releaseSneak() {
        if (sneakingFromModule && mc.options != null) {
            mc.options.keyShift.setDown(false);
        }
        sneakingFromModule = false;
    }
}
