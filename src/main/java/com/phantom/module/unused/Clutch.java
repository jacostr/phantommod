/* Copyright (c) 2026 PhantomMod. All rights reserved. */
package com.phantom.module.impl.movement;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Properties;

public class Clutch extends Module {
    public enum PlacementMode {
        SINGLE,
        PLATFORM_2x2,
        PLATFORM_3x3
    }

    public enum HPosition {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    public enum SafetyProfile {
        CONSERVATIVE(4, 3, true),
        BALANCED(8, 2, false),
        AGGRESSIVE(14, 1, false),
        BLATANT(20, 0, false);

        public final int maxPPS;
        public final int delay;
        public final boolean rotate;

        SafetyProfile(int maxPPS, int delay, boolean rotate) {
            this.maxPPS = maxPPS;
            this.delay = delay;
            this.rotate = rotate;
        }
    }

    private boolean voidClutchEnabled = true;
    private boolean scaffoldEnabled = true;
    private boolean bridgeAssistEnabled = true;

    private double voidYThreshold = 5.0;
    private double voidVelocityThreshold = -0.5;
    private int voidRaycastDistance = 10;

    private int voidPlaceDelay = 1;
    private int scaffoldPlaceDelay = 1;
    private boolean useOffhand = true;
    private boolean swingArm = true;

    private boolean predictivePlacement = true;
    private int predictionSteps = 1;
    private boolean towerMode = true;
    private boolean requireSprint = false;

    private boolean blacklistMode = true;
    private String blockBlacklist = "minecraft:tnt,minecraft:gravel,minecraft:sand";
    private String blockWhitelist = "";

    private double maxPlacementsPerSecond = 8.0;
    private int cooldownAfterClutch = 10;
    private boolean disableInLiquid = true;
    private boolean disableWhileFlying = true;
    private boolean onlyWhenHoldingBlock = false;

    private PlacementMode placementMode = PlacementMode.SINGLE;
    private boolean rotateToPlace = false;
    private int[] slotPriority = {0,1,2,3,4,5,6,7,8};
    private boolean preferCurrentSlot = true;

    private boolean hudEnabled = true;
    private HPosition hudPosition = HPosition.BOTTOM_RIGHT;
    private float hudScale = 1.0f;
    private int hudTextColor = 0xFFFFFF;
    private boolean showActiveIndicator = true;
    private int indicatorDurationTicks = 20;
    private int activeIndicatorTimer = 0;

    private boolean diagonalClutch = true;
    private int diagonalLookAhead = 2;
    private SafetyProfile activeProfile = SafetyProfile.BALANCED;
    private boolean autoThrottle = true;

    private int preset = 1;

    private int voidCooldown = 0;
    private int scaffoldCooldown = 0;
    private boolean isTowerMode = false;
    private int towerHoldTicks = 0;

    private long lastPlaceTime = 0;
    private int placesThisSecond = 0;
    private int secondCounter = 0;

    private float originalPitch = 0f;
    private final Deque<BlockPos> platformQueue = new ArrayDeque<>();

    public Clutch() {
        super("Clutch", "Void clutch + scaffold + bridge assist.\nDetectability: Blatant",
                ModuleCategory.MOVEMENT, -1);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

        if (onlyWhenHoldingBlock) {
            ItemStack held = mc.player.getMainHandItem();
            ItemStack offhand = mc.player.getOffhandItem();
            boolean holdingBlock = held.getItem() instanceof BlockItem || offhand.getItem() instanceof BlockItem;
            if (!holdingBlock) return;
        }

        tickPlaceCounter();
        tickCooldowns();

        if (disableInLiquid && (mc.player.isInWater() || mc.player.isInLava())) return;
        if (disableWhileFlying && mc.player.isFallFlying()) return;

        double horizontalSpeed = Math.sqrt(
                mc.player.getDeltaMovement().x() * mc.player.getDeltaMovement().x() +
                mc.player.getDeltaMovement().z() * mc.player.getDeltaMovement().z());

        boolean isHoldingBlock = mc.player.getMainHandItem().getItem() instanceof BlockItem ||
                (useOffhand && mc.player.getOffhandItem().getItem() instanceof BlockItem);

        if (bridgeAssistEnabled && isHoldingBlock &&
                mc.options.keyJump.isDown() && mc.options.keyUse.isDown() &&
                horizontalSpeed < 0.08 && mc.player.onGround()) {
            isTowerMode = true;
            towerHoldTicks = 8;
        } else if (horizontalSpeed > 0.08) {
            isTowerMode = false;
        }

        if (towerHoldTicks > 0) {
            towerHoldTicks--;
            if (towerHoldTicks == 0) isTowerMode = false;
        }

        if (requireSprint && !mc.player.isSprinting()) return;

        processPlatformQueue();

        if (voidClutchEnabled && shouldTriggerVoidClutch()) {
            runVoidClutch();
        }

        if ((scaffoldEnabled || bridgeAssistEnabled) && shouldTriggerScaffold(horizontalSpeed)) {
            runScaffold();
        }

        if (activeIndicatorTimer > 0) {
            activeIndicatorTimer--;
        }
    }

    private void processPlatformQueue() {
        if (platformQueue.isEmpty()) return;

        if (voidCooldown > 0 || scaffoldCooldown > 0) return;
        if (!checkRateLimit()) return;

        int slot = findBlockSlot();
        if (slot == -1) return;

        BlockPos pos = platformQueue.poll();
        if (pos == null) return;

        int prevSlot = mc.player.getInventory().getSelectedSlot();
        if (slot >= 0) {
            mc.player.getInventory().setSelectedSlot(slot);
        }

        if (rotateToPlace) {
            originalPitch = mc.player.getXRot();
            float targetPitch = calculateTargetPitch(pos);
            mc.player.setXRot(targetPitch);
        }

        placeBlockAt(pos);

        if (rotateToPlace) {
            mc.player.setXRot(originalPitch);
        }

        if (slot >= 0) {
            mc.player.getInventory().setSelectedSlot(prevSlot);
        }

        recordPlace();
        activeIndicatorTimer = indicatorDurationTicks;
    }

    private float calculateTargetPitch(BlockPos pos) {
        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 blockCenter = Vec3.atCenterOf(pos);
        double dy = blockCenter.y - eyePos.y;
        double dz = Math.sqrt(
                (blockCenter.x - eyePos.x) * (blockCenter.x - eyePos.x) +
                (blockCenter.z - eyePos.z) * (blockCenter.z - eyePos.z));
        float angle = (float) Math.toDegrees(Math.atan2(dy, dz));
        return Math.max(-90f, Math.min(90f, angle));
    }

    private boolean shouldTriggerVoidClutch() {
        if (mc.player.onGround()) return false;
        if (mc.player.getY() > voidYThreshold) return false;
        if (mc.player.getDeltaMovement().y() > voidVelocityThreshold) return false;
        if (voidCooldown > 0) return false;
        if (!canPlace()) return false;

        BlockPos feet = mc.player.blockPosition();
        for (int i = 1; i <= voidRaycastDistance; i++) {
            BlockPos check = feet.below(i);
            if (!mc.level.getBlockState(check).isAir()) return true;
        }
        return false;
    }

    private void runVoidClutch() {
        if (voidCooldown > 0) return;
        if (!checkRateLimit()) return;

        buildPlatformQueue();
        voidCooldown = cooldownAfterClutch;
    }

    private void buildPlatformQueue() {
        platformQueue.clear();

        BlockPos basePos;
        if (diagonalClutch) {
            double vx = mc.player.getDeltaMovement().x();
            double vz = mc.player.getDeltaMovement().z();
            int offsetX = (int) Math.signum(vx);
            int offsetZ = (int) Math.signum(vz);

            if (offsetX != 0 || offsetZ != 0) {
                basePos = mc.player.blockPosition().below().offset(offsetX, 0, offsetZ);

                if (mc.level.getBlockState(basePos).isAir()) {
                    platformQueue.add(basePos);
                }
                basePos = basePos.below();
            } else {
                basePos = mc.player.blockPosition().below();
            }
        } else {
            basePos = mc.player.blockPosition().below();
        }

        switch (placementMode) {
            case SINGLE -> platformQueue.add(basePos);
            case PLATFORM_2x2 -> {
                for (int x = -1; x <= 0; x++) {
                    for (int z = -1; z <= 0; z++) {
                        platformQueue.add(basePos.offset(x, 0, z));
                    }
                }
            }
            case PLATFORM_3x3 -> {
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        platformQueue.add(basePos.offset(x, 0, z));
                    }
                }
            }
        }
    }

    private boolean shouldTriggerScaffold(double horizontalSpeed) {
        if (!mc.player.onGround()) return false;
        if (horizontalSpeed < 0.05 && !isTowerMode) return false;
        if (!canPlace()) return false;

        if (isTowerMode) {
            BlockPos feet = mc.player.blockPosition();
            if (mc.level.getBlockState(feet.below()).isAir()) return true;
            return false;
        }

        BlockPos current = mc.player.blockPosition().below();
        if (mc.level.getBlockState(current).isAir()) return true;

        if (predictivePlacement) {
            Direction moveDir = getMoveDirection();
            for (int step = 1; step <= predictionSteps; step++) {
                BlockPos predicted = current.relative(moveDir, step);
                if (mc.level.getBlockState(predicted).isAir()) return true;
            }
        }

        return false;
    }

    private void runScaffold() {
        if (scaffoldCooldown > 0) return;
        if (!canPlace()) return;
        if (!checkRateLimit()) return;

        int slot = findBlockSlot();
        if (slot == -1) return;

        int prevSlot = mc.player.getInventory().getSelectedSlot();
        if (slot >= 0) {
            mc.player.getInventory().setSelectedSlot(slot);
        }

        if (isTowerMode) {
            BlockPos below = mc.player.blockPosition().below();
            if (rotateToPlace) {
                originalPitch = mc.player.getXRot();
                mc.player.setXRot(calculateTargetPitch(below));
            }
            placeBlockAt(below);
            if (rotateToPlace) {
                mc.player.setXRot(originalPitch);
            }
        } else {
            BlockPos target = mc.player.blockPosition().below();
            if (mc.level.getBlockState(target).isAir()) {
                if (rotateToPlace) {
                    originalPitch = mc.player.getXRot();
                    mc.player.setXRot(calculateTargetPitch(target));
                }
                placeBlockAt(target);
                if (rotateToPlace) {
                    mc.player.setXRot(originalPitch);
                }
            } else if (predictivePlacement) {
                Direction moveDir = getMoveDirection();
                for (int step = 1; step <= predictionSteps; step++) {
                    BlockPos predicted = target.relative(moveDir, step);
                    if (mc.level.getBlockState(predicted).isAir()) {
                        if (rotateToPlace) {
                            originalPitch = mc.player.getXRot();
                            mc.player.setXRot(calculateTargetPitch(predicted));
                        }
                        placeBlockAt(predicted);
                        if (rotateToPlace) {
                            mc.player.setXRot(originalPitch);
                        }
                        break;
                    }
                }
            }
        }

        if (slot >= 0) {
            mc.player.getInventory().setSelectedSlot(prevSlot);
        }
        scaffoldCooldown = scaffoldPlaceDelay;
        recordPlace();
        activeIndicatorTimer = indicatorDurationTicks;
    }

    private Direction getMoveDirection() {
        float yaw = mc.player.getYRot();
        yaw = yaw % 360;
        if (yaw < 0) yaw += 360;
        if (yaw >= 315 || yaw < 45) return Direction.SOUTH;
        if (yaw >= 45 && yaw < 135) return Direction.WEST;
        if (yaw >= 135 && yaw < 225) return Direction.NORTH;
        return Direction.EAST;
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

    private int findBlockSlot() {
        int current = mc.player.getInventory().getSelectedSlot();
        if (preferCurrentSlot) {
            ItemStack currentStack = mc.player.getInventory().getItem(current);
            if (isValidBlock(currentStack.getItem())) return current;
        }

        for (int priorityIndex = 0; priorityIndex < slotPriority.length; priorityIndex++) {
            int slot = slotPriority[priorityIndex];
            if (slot < 0 || slot > 8) continue;
            if (preferCurrentSlot && slot == current) continue;

            ItemStack stack = mc.player.getInventory().getItem(slot);
            if (isValidBlock(stack.getItem())) return slot;
        }

        if (useOffhand) {
            ItemStack off = mc.player.getOffhandItem();
            if (isValidBlock(off.getItem())) return -1;
        }
        return -1;
    }

    private boolean isValidBlock(Item item) {
        if (!(item instanceof BlockItem)) return false;
        if (blockWhitelist.isEmpty() && blacklistMode) {
            String id = item.getDescriptionId();
            for (String blocked : blockBlacklist.split(",")) {
                if (id.equals(blocked)) return false;
            }
        }
        if (!blockWhitelist.isEmpty()) {
            String id = item.getDescriptionId();
            for (String allowed : blockWhitelist.split(",")) {
                if (id.equals(allowed)) return true;
            }
            return false;
        }
        return true;
    }

    private boolean canPlace() {
        return placesThisSecond < maxPlacementsPerSecond;
    }

    private boolean checkRateLimit() {
        return placesThisSecond < maxPlacementsPerSecond;
    }

    private void recordPlace() {
        placesThisSecond++;
        lastPlaceTime = System.currentTimeMillis();
    }

    private void tickPlaceCounter() {
        int currentSecond = (int) (System.currentTimeMillis() / 1000);
        if (currentSecond != secondCounter) {
            placesThisSecond = 0;
            secondCounter = currentSecond;
        }
    }

    private void tickCooldowns() {
        if (voidCooldown > 0) voidCooldown--;
        if (scaffoldCooldown > 0) scaffoldCooldown--;
    }

    @Override
    public void onDisable() {
        isTowerMode = false;
        towerHoldTicks = 0;
        voidCooldown = 0;
        scaffoldCooldown = 0;
        platformQueue.clear();
        activeIndicatorTimer = 0;
    }

    @Override
    public void onHudRender(GuiGraphics graphics) {
        if (!hudEnabled || !isEnabled()) return;

        String voidStatus = voidClutchEnabled ? "ON" : "OFF";
        String scaffoldStatus = scaffoldEnabled ? "ON" : "OFF";
        String bridgeStatus = bridgeAssistEnabled ? "ON" : "OFF";
        String modeStr = placementMode.name();

        String text = String.format("[CLUTCH] Void: %s | Scaf: %s | Brid: %s | %s",
                voidStatus, scaffoldStatus, bridgeStatus, modeStr);

        int color = hudTextColor;
        if (showActiveIndicator && activeIndicatorTimer > 0) {
            color = 0x00FF00;
        }

        int x, y;
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();

        switch (hudPosition) {
            case TOP_LEFT -> { x = 10; y = 10; }
            case TOP_RIGHT -> { x = w - 150; y = 10; }
            case BOTTOM_LEFT -> { x = 10; y = h - 20; }
            case BOTTOM_RIGHT -> { x = w - 150; y = h - 20; }
            default -> { x = w - 150; y = h - 20; }
        }

        graphics.drawString(mc.font, net.minecraft.network.chat.Component.literal(text), x, y, color);
    }

    public void applyPresetLegit() {
        preset = 0;
        voidPlaceDelay = 4;
        scaffoldPlaceDelay = 4;
        maxPlacementsPerSecond = 3.0;
        predictivePlacement = true;
        predictionSteps = 1;
        towerMode = true;
        requireSprint = true;
        cooldownAfterClutch = 20;
        saveConfig();
    }

    public void applyPresetNormal() {
        preset = 1;
        voidPlaceDelay = 2;
        scaffoldPlaceDelay = 2;
        maxPlacementsPerSecond = 6.0;
        predictivePlacement = true;
        predictionSteps = 1;
        towerMode = true;
        requireSprint = false;
        cooldownAfterClutch = 10;
        saveConfig();
    }

    public void applyPresetObvious() {
        preset = 2;
        voidPlaceDelay = 1;
        scaffoldPlaceDelay = 1;
        maxPlacementsPerSecond = 10.0;
        predictivePlacement = true;
        predictionSteps = 2;
        towerMode = true;
        requireSprint = false;
        cooldownAfterClutch = 5;
        saveConfig();
    }

    public void applyPresetBlatant() {
        preset = 3;
        voidPlaceDelay = 0;
        scaffoldPlaceDelay = 0;
        maxPlacementsPerSecond = 20.0;
        predictivePlacement = true;
        predictionSteps = 3;
        towerMode = true;
        requireSprint = false;
        cooldownAfterClutch = 0;
        saveConfig();
    }

    @Override
    public boolean hasConfigurableSettings() {
        return true;
    }

    @Override
    public Screen createSettingsScreen(Screen parent) {
        return new ModuleSettingsScreen(parent, this);
    }

    public boolean isVoidClutchEnabled() { return voidClutchEnabled; }
    public void setVoidClutchEnabled(boolean e) { this.voidClutchEnabled = e; saveConfig(); }

    public boolean isScaffoldEnabled() { return scaffoldEnabled; }
    public void setScaffoldEnabled(boolean e) { this.scaffoldEnabled = e; saveConfig(); }

    public boolean isBridgeAssistEnabled() { return bridgeAssistEnabled; }
    public void setBridgeAssistEnabled(boolean e) { this.bridgeAssistEnabled = e; saveConfig(); }

    public double getVoidYThreshold() { return voidYThreshold; }
    public void setVoidYThreshold(double v) { this.voidYThreshold = Math.max(1.0, Math.min(256.0, v)); saveConfig(); }

    public double getVoidVelocityThreshold() { return voidVelocityThreshold; }
    public void setVoidVelocityThreshold(double v) { this.voidVelocityThreshold = Math.max(-5.0, Math.min(-0.1, v)); saveConfig(); }

    public int getVoidRaycastDistance() { return voidRaycastDistance; }
    public void setVoidRaycastDistance(int v) { this.voidRaycastDistance = Math.max(1, Math.min(30, v)); saveConfig(); }

    public int getVoidPlaceDelay() { return voidPlaceDelay; }
    public void setVoidPlaceDelay(int v) { this.voidPlaceDelay = Math.max(0, Math.min(10, v)); saveConfig(); }

    public int getScaffoldPlaceDelay() { return scaffoldPlaceDelay; }
    public void setScaffoldPlaceDelay(int v) { this.scaffoldPlaceDelay = Math.max(0, Math.min(10, v)); saveConfig(); }

    public boolean isUseOffhand() { return useOffhand; }
    public void setUseOffhand(boolean v) { this.useOffhand = v; saveConfig(); }

    public boolean isSwingArm() { return swingArm; }
    public void setSwingArm(boolean v) { this.swingArm = v; saveConfig(); }

    public boolean isPredictivePlacement() { return predictivePlacement; }
    public void setPredictivePlacement(boolean v) { this.predictivePlacement = v; saveConfig(); }

    public int getPredictionSteps() { return predictionSteps; }
    public void setPredictionSteps(int v) { this.predictionSteps = Math.max(1, Math.min(3, v)); saveConfig(); }

    public boolean isTowerModeEnabled() { return towerMode; }
    public void setTowerModeEnabled(boolean v) { this.towerMode = v; saveConfig(); }

    public boolean isRequireSprint() { return requireSprint; }
    public void setRequireSprint(boolean v) { this.requireSprint = v; saveConfig(); }

    public PlacementMode getPlacementMode() { return placementMode; }
    public void setPlacementMode(PlacementMode m) { this.placementMode = m; saveConfig(); }

    public boolean isRotateToPlace() { return rotateToPlace; }
    public void setRotateToPlace(boolean v) { this.rotateToPlace = v; saveConfig(); }

    public int[] getSlotPriority() { return slotPriority; }
    public void setSlotPriority(int[] v) { this.slotPriority = v; saveConfig(); }

    public boolean isPreferCurrentSlot() { return preferCurrentSlot; }
    public void setPreferCurrentSlot(boolean v) { this.preferCurrentSlot = v; saveConfig(); }

    public boolean isHudEnabled() { return hudEnabled; }
    public void setHudEnabled(boolean v) { this.hudEnabled = v; saveConfig(); }

    public HPosition getHudPosition() { return hudPosition; }
    public void setHudPosition(HPosition v) { this.hudPosition = v; saveConfig(); }

    public float getHudScale() { return hudScale; }
    public void setHudScale(float v) { this.hudScale = Math.max(0.5f, Math.min(2.0f, v)); saveConfig(); }

    public int getHudTextColor() { return hudTextColor; }
    public void setHudTextColor(int v) { this.hudTextColor = v; saveConfig(); }

    public boolean isShowActiveIndicator() { return showActiveIndicator; }
    public void setShowActiveIndicator(boolean v) { this.showActiveIndicator = v; saveConfig(); }

    public int getIndicatorDurationTicks() { return indicatorDurationTicks; }
    public void setIndicatorDurationTicks(int v) { this.indicatorDurationTicks = Math.max(1, Math.min(100, v)); saveConfig(); }

    public boolean isOnlyWhenHoldingBlock() { return onlyWhenHoldingBlock; }
    public void setOnlyWhenHoldingBlock(boolean v) { this.onlyWhenHoldingBlock = v; saveConfig(); }

    public boolean isDiagonalClutch() { return diagonalClutch; }
    public void setDiagonalClutch(boolean v) { this.diagonalClutch = v; saveConfig(); }

    public int getDiagonalLookAhead() { return diagonalLookAhead; }
    public void setDiagonalLookAhead(int v) { this.diagonalLookAhead = Math.max(1, Math.min(4, v)); saveConfig(); }

    public SafetyProfile getActiveProfile() { return activeProfile; }

    public void setActiveProfile(SafetyProfile profile) {
        this.activeProfile = profile;
        this.maxPlacementsPerSecond = profile.maxPPS;
        this.scaffoldPlaceDelay = profile.delay;
        this.rotateToPlace = profile.rotate;
        saveConfig();
    }

    public boolean isAutoThrottle() { return autoThrottle; }
    public void setAutoThrottle(boolean v) { this.autoThrottle = v; saveConfig(); }

    public int getPreset() { return preset; }
    public void setPreset(int v) {
        this.preset = Math.max(0, Math.min(3, v));
        switch (this.preset) {
            case 0 -> applyPresetLegit();
            case 1 -> applyPresetNormal();
            case 2 -> applyPresetObvious();
            case 3 -> applyPresetBlatant();
        }
    }

    public double getMaxPlacementsPerSecond() { return maxPlacementsPerSecond; }
    public void setMaxPlacementsPerSecond(double v) { this.maxPlacementsPerSecond = Math.max(1.0, Math.min(20.0, v)); saveConfig(); }

    public int getCooldownAfterClutch() { return cooldownAfterClutch; }
    public void setCooldownAfterClutch(int v) { this.cooldownAfterClutch = Math.max(0, Math.min(20, v)); saveConfig(); }

    public boolean isDisableInLiquid() { return disableInLiquid; }
    public void setDisableInLiquid(boolean v) { this.disableInLiquid = v; saveConfig(); }

    public boolean isDisableWhileFlying() { return disableWhileFlying; }
    public void setDisableWhileFlying(boolean v) { this.disableWhileFlying = v; saveConfig(); }

    public String getBlockBlacklist() { return blockBlacklist; }
    public void setBlockBlacklist(String v) { this.blockBlacklist = v; saveConfig(); }

    public String getBlockWhitelist() { return blockWhitelist; }
    public void setBlockWhitelist(String v) { this.blockWhitelist = v; saveConfig(); }

    @Override
    public void loadConfig(Properties props) {
        super.loadConfig(props);
        this.voidClutchEnabled = Boolean.parseBoolean(props.getProperty("clutch.void_enabled", "true"));
        this.scaffoldEnabled = Boolean.parseBoolean(props.getProperty("clutch.scaffold_enabled", "true"));
        this.bridgeAssistEnabled = Boolean.parseBoolean(props.getProperty("clutch.bridge_assist_enabled", "true"));
        this.voidYThreshold = Double.parseDouble(props.getProperty("clutch.void_y_threshold", "5.0"));
        this.voidVelocityThreshold = Double.parseDouble(props.getProperty("clutch.void_velocity_threshold", "-0.5"));
        this.voidRaycastDistance = Integer.parseInt(props.getProperty("clutch.void_raycast_distance", "10"));
        this.voidPlaceDelay = Integer.parseInt(props.getProperty("clutch.void_place_delay", "1"));
        this.scaffoldPlaceDelay = Integer.parseInt(props.getProperty("clutch.scaffold_place_delay", "1"));
        this.useOffhand = Boolean.parseBoolean(props.getProperty("clutch.use_offhand", "true"));
        this.swingArm = Boolean.parseBoolean(props.getProperty("clutch.swing_arm", "true"));
        this.predictivePlacement = Boolean.parseBoolean(props.getProperty("clutch.predictive_placement", "true"));
        this.predictionSteps = Integer.parseInt(props.getProperty("clutch.prediction_steps", "1"));
        this.towerMode = Boolean.parseBoolean(props.getProperty("clutch.tower_mode", "true"));
        this.requireSprint = Boolean.parseBoolean(props.getProperty("clutch.require_sprint", "false"));
        this.maxPlacementsPerSecond = Double.parseDouble(props.getProperty("clutch.max_placements_per_second", "8.0"));
        this.cooldownAfterClutch = Integer.parseInt(props.getProperty("clutch.cooldown_after_clutch", "10"));
        this.disableInLiquid = Boolean.parseBoolean(props.getProperty("clutch.disable_in_liquid", "true"));
        this.disableWhileFlying = Boolean.parseBoolean(props.getProperty("clutch.disable_while_flying", "true"));
        this.onlyWhenHoldingBlock = Boolean.parseBoolean(props.getProperty("clutch.only_when_holding_block", "false"));
        this.blockBlacklist = props.getProperty("clutch.block_blacklist", "minecraft:tnt,minecraft:gravel,minecraft:sand");
        this.blockWhitelist = props.getProperty("clutch.block_whitelist", "");
        this.preset = Integer.parseInt(props.getProperty("clutch.preset", "1"));

        String modeStr = props.getProperty("clutch.placement_mode", "SINGLE");
        try { this.placementMode = PlacementMode.valueOf(modeStr); } catch (Exception ignored) {}

        this.rotateToPlace = Boolean.parseBoolean(props.getProperty("clutch.rotate_to_place", "false"));
        this.preferCurrentSlot = Boolean.parseBoolean(props.getProperty("clutch.prefer_current_slot", "true"));

        String priorityStr = props.getProperty("clutch.slot_priority", "0,1,2,3,4,5,6,7,8");
        try {
            String[] parts = priorityStr.split(",");
            this.slotPriority = new int[parts.length];
            for (int i = 0; i < parts.length; i++) {
                this.slotPriority[i] = Integer.parseInt(parts[i].trim());
            }
        } catch (Exception ignored) {}

        this.hudEnabled = Boolean.parseBoolean(props.getProperty("clutch.hud_enabled", "true"));
        String hudPosStr = props.getProperty("clutch.hud_position", "BOTTOM_RIGHT");
        try { this.hudPosition = HPosition.valueOf(hudPosStr); } catch (Exception ignored) {}
        this.hudScale = Float.parseFloat(props.getProperty("clutch.hud_scale", "1.0"));
        this.hudTextColor = Integer.parseInt(props.getProperty("clutch.hud_text_color", "FFFFFF"), 16);
        this.showActiveIndicator = Boolean.parseBoolean(props.getProperty("clutch.show_active_indicator", "true"));
        this.indicatorDurationTicks = Integer.parseInt(props.getProperty("clutch.indicator_duration_ticks", "20"));

        this.diagonalClutch = Boolean.parseBoolean(props.getProperty("clutch.diagonal_clutch", "true"));
        this.diagonalLookAhead = Integer.parseInt(props.getProperty("clutch.diagonal_look_ahead", "2"));

        String profileStr = props.getProperty("clutch.active_profile", "BALANCED");
        try { this.activeProfile = SafetyProfile.valueOf(profileStr); } catch (Exception ignored) {}

        this.autoThrottle = Boolean.parseBoolean(props.getProperty("clutch.auto_throttle", "true"));
    }

    @Override
    public void saveConfig(Properties props) {
        super.saveConfig(props);
        props.setProperty("clutch.void_enabled", String.valueOf(voidClutchEnabled));
        props.setProperty("clutch.scaffold_enabled", String.valueOf(scaffoldEnabled));
        props.setProperty("clutch.bridge_assist_enabled", String.valueOf(bridgeAssistEnabled));
        props.setProperty("clutch.void_y_threshold", String.valueOf(voidYThreshold));
        props.setProperty("clutch.void_velocity_threshold", String.valueOf(voidVelocityThreshold));
        props.setProperty("clutch.void_raycast_distance", String.valueOf(voidRaycastDistance));
        props.setProperty("clutch.void_place_delay", String.valueOf(voidPlaceDelay));
        props.setProperty("clutch.scaffold_place_delay", String.valueOf(scaffoldPlaceDelay));
        props.setProperty("clutch.use_offhand", String.valueOf(useOffhand));
        props.setProperty("clutch.swing_arm", String.valueOf(swingArm));
        props.setProperty("clutch.predictive_placement", String.valueOf(predictivePlacement));
        props.setProperty("clutch.prediction_steps", String.valueOf(predictionSteps));
        props.setProperty("clutch.tower_mode", String.valueOf(towerMode));
        props.setProperty("clutch.require_sprint", String.valueOf(requireSprint));
        props.setProperty("clutch.max_placements_per_second", String.valueOf(maxPlacementsPerSecond));
        props.setProperty("clutch.cooldown_after_clutch", String.valueOf(cooldownAfterClutch));
        props.setProperty("clutch.disable_in_liquid", String.valueOf(disableInLiquid));
        props.setProperty("clutch.disable_while_flying", String.valueOf(disableWhileFlying));
        props.setProperty("clutch.only_when_holding_block", String.valueOf(onlyWhenHoldingBlock));
        props.setProperty("clutch.block_blacklist", blockBlacklist);
        props.setProperty("clutch.block_whitelist", blockWhitelist);
        props.setProperty("clutch.preset", String.valueOf(preset));
        props.setProperty("clutch.placement_mode", placementMode.name());
        props.setProperty("clutch.rotate_to_place", String.valueOf(rotateToPlace));
        props.setProperty("clutch.prefer_current_slot", String.valueOf(preferCurrentSlot));

        StringBuilder priorityBuilder = new StringBuilder();
        for (int i = 0; i < slotPriority.length; i++) {
            if (i > 0) priorityBuilder.append(",");
            priorityBuilder.append(slotPriority[i]);
        }
        props.setProperty("clutch.slot_priority", priorityBuilder.toString());

        props.setProperty("clutch.hud_enabled", String.valueOf(hudEnabled));
        props.setProperty("clutch.hud_position", hudPosition.name());
        props.setProperty("clutch.hud_scale", String.valueOf(hudScale));
        props.setProperty("clutch.hud_text_color", Integer.toHexString(hudTextColor));
        props.setProperty("clutch.show_active_indicator", String.valueOf(showActiveIndicator));
        props.setProperty("clutch.indicator_duration_ticks", String.valueOf(indicatorDurationTicks));

        props.setProperty("clutch.diagonal_clutch", String.valueOf(diagonalClutch));
        props.setProperty("clutch.diagonal_look_ahead", String.valueOf(diagonalLookAhead));
        props.setProperty("clutch.active_profile", activeProfile.name());
        props.setProperty("clutch.auto_throttle", String.valueOf(autoThrottle));
    }
}