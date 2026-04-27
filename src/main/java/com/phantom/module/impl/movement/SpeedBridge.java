/* Copyright (c) 2026 PhantomMod. All rights reserved. */
package com.phantom.module.impl.movement;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.mixin.MinecraftClientAccessor;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Properties;


public class SpeedBridge extends Module {
    private static final double EDGE_CHECK_OFFSET = 0.32D;
    private static final int JUMP_SNEAK_TICKS = 4;
    private static final int UNSNEAK_GRACE_TICKS = 2;
    private static final double STATIONARY_THRESHOLD = 0.08D;

    public enum Preset {
        LEGIT("Legit"), NORMAL("Normal"), OBVIOUS("Obvious"), BLATANT("Blatant");
        private final String name;
        Preset(String name) { this.name = name; }
        public String getName() { return name; }
        public Preset next() { return values()[(this.ordinal() + 1) % values().length]; }
        public static Preset fromInt(int i) { return values()[Math.max(0, Math.min(3, i))]; }
    }

    private long lastPlaceTime;
    private double autoOffDelay = 3.0;
    private boolean sneakingFromModule;
    private int delayTicks = 3;
    private boolean blocksOnly = true;
    private boolean sneakOnJump = true;
    private int jumpSneakTicks;
    private int unsneakGraceTicks;
    private boolean wasOnGround;

    private boolean bridgeAssistEnabled = true;
    private boolean towerModeEnabled = true;
    private int scaffoldPlaceDelay = 1;

    private boolean isTowerMode = false;
    private int towerHoldTicks = 0;
    private int scaffoldCooldown = 0;
    private float originalPitch = 0f;
    private boolean swingArm = true;

    private int activeIndicatorTimer = 0;

    private int preset = 1;
    private Preset currentPreset = Preset.NORMAL;

    public SpeedBridge() {
        super(
                "SpeedBridge Assist",
                "Auto bridge + tower assist. Hold jump + right-click while standing still to tower. Move normally to flat bridge.\nDetectability: Safe/Subtle",
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
        isTowerMode = false;
        towerHoldTicks = 0;
        scaffoldCooldown = 0;
    }

    @Override
    public void onDisable() {
        releaseSneak();
        isTowerMode = false;
        towerHoldTicks = 0;
        scaffoldCooldown = 0;
    }

    @Override
    public void onTick() {
        if (mc.level == null || mc.player == null || mc.options == null)
            return;

        if (scaffoldCooldown > 0) {
            scaffoldCooldown--;
        }

        if (activeIndicatorTimer > 0) {
            activeIndicatorTimer--;
        }

        boolean isHoldingBlock = false;
        double horizontalSpeed = 0.0;

        try {
            isHoldingBlock = mc.player.getMainHandItem().getItem() instanceof BlockItem ||
                    mc.player.getOffhandItem().getItem() instanceof BlockItem;

            horizontalSpeed = Math.sqrt(
                    mc.player.getDeltaMovement().x() * mc.player.getDeltaMovement().x() +
                    mc.player.getDeltaMovement().z() * mc.player.getDeltaMovement().z());
        } catch (Exception ignored) {}

        if (bridgeAssistEnabled && towerModeEnabled && isHoldingBlock &&
                mc.options.keyJump.isDown() && mc.options.keyUse.isDown() &&
                horizontalSpeed < STATIONARY_THRESHOLD && mc.player.onGround()) {
            isTowerMode = true;
            towerHoldTicks = 8;
        } else if (horizontalSpeed > STATIONARY_THRESHOLD) {
            isTowerMode = false;
        }

        if (towerHoldTicks > 0) {
            towerHoldTicks--;
            if (towerHoldTicks == 0) isTowerMode = false;
        }

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

        if (isTowerMode) {
            runTowerScaffold();
            return;
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

    private void runTowerScaffold() {
        if (scaffoldCooldown > 0) return;
        if (!mc.player.onGround()) return;

        BlockPos feet = mc.player.blockPosition();
        if (!mc.level.getBlockState(feet.below()).isAir()) return;

        int slot = findNextBlockSlot();
        if (slot == -1) return;

        int prevSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(slot);

        BlockPos target = feet.below();
        placeBlockAt(target);

        mc.player.getInventory().setSelectedSlot(prevSlot);
        scaffoldCooldown = scaffoldPlaceDelay;
        activeIndicatorTimer = 20;
    }

    private void placeBlockAt(BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            if (!mc.level.getBlockState(neighbor).isAir()) {
                Direction face = dir.getOpposite();
                Vec3 hitVec = Vec3.atCenterOf(neighbor).add(
                        new Vec3(face.getStepX(), face.getStepY(), face.getStepZ()).scale(0.5));
                BlockHitResult hit = new BlockHitResult(hitVec, face, neighbor, false);
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
                if (swingArm) mc.player.swing(InteractionHand.MAIN_HAND);
                break;
            }
        }
    }

    private boolean checkAutoDisable() {
        if (System.currentTimeMillis() - lastPlaceTime > autoOffDelay * 1000) {
            setEnabled(false);
            return true;
        }
        return false;
    }

    @Override
    public void onHudRender(GuiGraphics graphics) {
        if (!isEnabled()) return;

        String towerStatus = isTowerMode ? "TOWER" : "FLAT";
        String text = "[SpeedBridge] " + towerStatus;

        int color = 0xFFFFFF;
        if (activeIndicatorTimer > 0) {
            color = 0x00FF00;
        }

        int w = graphics.guiWidth();
        int h = graphics.guiHeight();

        graphics.drawString(mc.font, net.minecraft.network.chat.Component.literal(text), w - 120, h - 20, color);
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

        String delayVal = properties.getProperty("speedbridge.delay_ticks");
        if (delayVal != null) {
            try {
                delayTicks = Math.max(0, Math.min(4, Integer.parseInt(delayVal.trim())));
            } catch (NumberFormatException ignored) {}
        }
        blocksOnly = Boolean.parseBoolean(properties.getProperty("speedbridge.blocks_only", Boolean.toString(blocksOnly)));
        sneakOnJump = Boolean.parseBoolean(properties.getProperty("speedbridge.sneak_on_jump", Boolean.toString(sneakOnJump)));

        bridgeAssistEnabled = Boolean.parseBoolean(properties.getProperty("speedbridge.bridge_assist_enabled", "true"));
        towerModeEnabled = Boolean.parseBoolean(properties.getProperty("speedbridge.tower_mode_enabled", "true"));
        scaffoldPlaceDelay = Integer.parseInt(properties.getProperty("speedbridge.scaffold_place_delay", "1"));
        swingArm = Boolean.parseBoolean(properties.getProperty("speedbridge.swing_arm", "true"));
        preset = Integer.parseInt(properties.getProperty("speedbridge.preset", "1"));
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("speedbridge.auto_off_delay", Double.toString(autoOffDelay));
        properties.setProperty("speedbridge.delay_ticks", Integer.toString(delayTicks));
        properties.setProperty("speedbridge.blocks_only", Boolean.toString(blocksOnly));
        properties.setProperty("speedbridge.sneak_on_jump", Boolean.toString(sneakOnJump));

        properties.setProperty("speedbridge.bridge_assist_enabled", String.valueOf(bridgeAssistEnabled));
        properties.setProperty("speedbridge.tower_mode_enabled", String.valueOf(towerModeEnabled));
        properties.setProperty("speedbridge.scaffold_place_delay", String.valueOf(scaffoldPlaceDelay));
        properties.setProperty("speedbridge.swing_arm", String.valueOf(swingArm));
        properties.setProperty("speedbridge.preset", String.valueOf(preset));
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

    public boolean isBridgeAssistEnabled() {
        return bridgeAssistEnabled;
    }

    public void setBridgeAssistEnabled(boolean enabled) {
        this.bridgeAssistEnabled = enabled;
        saveConfig();
    }

    public boolean isTowerModeEnabled() {
        return towerModeEnabled;
    }

    public void setTowerModeEnabled(boolean enabled) {
        this.towerModeEnabled = enabled;
        saveConfig();
    }


    public int getScaffoldPlaceDelay() {
        return scaffoldPlaceDelay;
    }

    public void setScaffoldPlaceDelay(int delay) {
        this.scaffoldPlaceDelay = Math.max(0, Math.min(10, delay));
        saveConfig();
    }

    public boolean isSwingArm() {
        return swingArm;
    }

    public void setSwingArm(boolean swingArm) {
        this.swingArm = swingArm;
        saveConfig();
    }

    public void applyPresetLegit() {
        preset = 0;
        setDelayTicks(3);
        setBlocksOnly(true);
        scaffoldPlaceDelay = 4;
        saveConfig();
    }

    public void applyPresetNormal() {
        preset = 1;
        setDelayTicks(2);
        setBlocksOnly(true);
        scaffoldPlaceDelay = 2;
        saveConfig();
    }

    public void applyPresetObvious() {
        preset = 2;
        setDelayTicks(1);
        setBlocksOnly(true);
        scaffoldPlaceDelay = 1;
        saveConfig();
    }

    public void applyPresetBlatant() {
        preset = 3;
        setDelayTicks(0);
        setBlocksOnly(false);
        scaffoldPlaceDelay = 0;
        saveConfig();
    }

    public Preset getCurrentPreset() {
        return currentPreset;
    }

    public int getPreset() {
        return preset;
    }

    public void cyclePreset() {
        currentPreset = currentPreset.next();
        switch (currentPreset) {
            case LEGIT -> applyPresetLegit();
            case NORMAL -> applyPresetNormal();
            case OBVIOUS -> applyPresetObvious();
            case BLATANT -> applyPresetBlatant();
        }
    }

    public void setPreset(int preset) {
        this.currentPreset = Preset.fromInt(preset);
        switch (this.currentPreset) {
            case LEGIT -> applyPresetLegit();
            case NORMAL -> applyPresetNormal();
            case OBVIOUS -> applyPresetObvious();
            case BLATANT -> applyPresetBlatant();
        }
    }

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
        if (isTowerMode) return false;
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