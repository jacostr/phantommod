/*
 * Scaffold.java — Automatically places blocks under the player (Movement module).
 *
 * Detects when the block directly below is air, temporarily adjusts pitch downward,
 * triggers a use-item action to place a block, then restores pitch. Supports Standard
 * (tower), Legit (sneaking), and God Bridge (fast) modes.
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

import java.util.Locale;
import java.util.Properties;

public class Scaffold extends Module {
    private static final double EDGE_CHECK_OFFSET = 0.32D;
    private static final double LEGIT_AUTO_OFF_DELAY = 2.34D;
    private static final double LEGIT_EAGLE_DELAY_MS = 34.66D;
    private static final double LEGIT_SLOWDOWN = 0.78D;
    private static final double LEGIT_MISTAKE_CHANCE = 0.08D;

    public enum Preset {
        STANDARD("Standard"),
        LEGIT("Legit"),
        FAST_LEGIT("Fast Legit");

        private final String label;

        Preset(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public Preset next() {
            return switch (this) {
                case STANDARD -> LEGIT;
                case LEGIT -> FAST_LEGIT;
                case FAST_LEGIT -> STANDARD;
            };
        }

        public static Preset fromString(String raw) {
            if (raw == null) {
                return STANDARD;
            }
            String s = raw.trim().toUpperCase(Locale.ROOT);
            try {
                return Preset.valueOf(s);
            } catch (IllegalArgumentException e) {
                return STANDARD;
            }
        }
    }

    private Preset preset = Preset.STANDARD;
    private boolean tower = true;
    private boolean safeWalk = true;
    private int placeCooldown;
    private int towerJumpCooldown;
    private boolean sneakingFromModule;
    private boolean desiredSneakState;
    private boolean pendingSneakState;
    private boolean mistakeInjectedThisEdge;
    private long sneakStateChangeAt;
    private long lastPlaceTime;
    private double autoOffDelay = 3.0;
    private double eagleDelayMs = 100.0;
    private double slowdown = 0.78;
    private double mistakeChance = 0.08;

    public Scaffold() {
        super("Scaffold",
                "Automatically places blocks under you.\nDetectability: Legit in 'Legit' mode.",
                ModuleCategory.MOVEMENT,
                -1);
    }

    @Override
    public void onEnable() {
        lastPlaceTime = System.currentTimeMillis();
        desiredSneakState = false;
        pendingSneakState = false;
        mistakeInjectedThisEdge = false;
        sneakStateChangeAt = 0L;
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

        if (placeCooldown > 0) {
            placeCooldown--;
        }
        if (towerJumpCooldown > 0) {
            towerJumpCooldown--;
        }

        if (preset == Preset.LEGIT || preset == Preset.FAST_LEGIT) {
            handleEagle();
        } else {
            releaseSneak();
        }

        if (preset == Preset.LEGIT) {
            applyLegitSlowdown();
        }

        if (safeWalk) {
            handleSafeWalk();
        }

        BlockPos pos = mc.player.blockPosition().below();
        if (mc.level.getBlockState(pos).isAir()) {
            boolean canPlace = preset != Preset.LEGIT || placeCooldown <= 0;
            if (canPlace) {
                placeBlock(pos);
                lastPlaceTime = System.currentTimeMillis();
                if (preset == Preset.LEGIT) {
                    placeCooldown = 2 + mc.player.getRandom().nextInt(3);
                }
            }
        }

        boolean towerActive = switch (preset) {
            case STANDARD -> tower;
            case LEGIT, FAST_LEGIT -> true;
        };

        if (towerActive && mc.options.keyJump.isDown() && !mc.player.isSprinting()) {
            if (preset == Preset.LEGIT) {
                if (towerJumpCooldown <= 0 && mc.player.onGround()) {
                    mc.player.jumpFromGround();
                    towerJumpCooldown = 5 + mc.player.getRandom().nextInt(4);
                }
            } else {
                if (mc.player.onGround()) {
                    mc.player.jumpFromGround();
                }
            }
        }
    }

    @Override
    public void onDisable() {
        releaseSneak();
        placeCooldown = 0;
        towerJumpCooldown = 0;
        desiredSneakState = false;
        pendingSneakState = false;
        mistakeInjectedThisEdge = false;
        sneakStateChangeAt = 0L;
    }

    private void releaseSneak() {
        if (sneakingFromModule && mc.options != null) {
            mc.options.keyShift.setDown(false);
        }
        sneakingFromModule = false;
        desiredSneakState = false;
    }

    private void handleEagle() {
        if (mc.player == null || mc.options == null) return;

        boolean isAtEdge = isAtSneakEdge();
        long now = System.currentTimeMillis();

        if (isAtEdge && mc.player.onGround()) {
            scheduleSneakState(true, now);
        } else {
            mistakeInjectedThisEdge = false;
            scheduleSneakState(false, now);
        }

        if (pendingSneakState != desiredSneakState && now >= sneakStateChangeAt) {
            if (pendingSneakState && !mistakeInjectedThisEdge && mc.player.getRandom().nextDouble() < mistakeChance) {
                mistakeInjectedThisEdge = true;
                sneakStateChangeAt = now + nextEagleDelayMs();
            } else {
                mc.options.keyShift.setDown(pendingSneakState);
                sneakingFromModule = pendingSneakState;
                desiredSneakState = pendingSneakState;
            }
        }

        if (preset == Preset.FAST_LEGIT && sneakingFromModule && mc.player.getRandom().nextInt(10) == 0) {
            releaseSneak();
        }
    }

    private boolean isAtSneakEdge() {
        if (mc.player == null || mc.level == null) {
            return false;
        }

        Vec3 movement = mc.player.getDeltaMovement();
        double moveX = movement.x;
        double moveZ = movement.z;

        // When movement is tiny, fall back to the player's facing direction so sneak
        // can still engage while beginning a bridge.
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

    private void scheduleSneakState(boolean shouldSneak, long now) {
        if (pendingSneakState == shouldSneak && desiredSneakState == shouldSneak) {
            return;
        }
        if (pendingSneakState != shouldSneak) {
            pendingSneakState = shouldSneak;
            sneakStateChangeAt = now + nextEagleDelayMs();
        }
    }

    private long nextEagleDelayMs() {
        if (mc.player == null) {
            return Math.max(0L, Math.round(eagleDelayMs));
        }
        double min = eagleDelayMs * 0.5;
        double max = eagleDelayMs * 1.5;
        return Math.max(0L, Math.round(min + mc.player.getRandom().nextDouble() * Math.max(1.0, max - min)));
    }

    private void applyLegitSlowdown() {
        if (mc.player == null || !mc.player.onGround()) {
            return;
        }
        if (!sneakingFromModule && !pendingSneakState) {
            return;
        }
        Vec3 vel = mc.player.getDeltaMovement();
        mc.player.setDeltaMovement(vel.x * slowdown, vel.y, vel.z * slowdown);
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

    public Preset getPreset() {
        return preset;
    }

    public void setPreset(Preset preset) {
        this.preset = preset;
        if (this.preset == Preset.LEGIT) {
            applyPresetLegit();
        }
        if (this.preset != Preset.LEGIT) {
            releaseSneak();
        }
        saveConfig();
    }

    public void applyPresetLegit() {
        safeWalk = true;
        tower = false;
        autoOffDelay = LEGIT_AUTO_OFF_DELAY;
        eagleDelayMs = LEGIT_EAGLE_DELAY_MS;
        slowdown = LEGIT_SLOWDOWN;
        mistakeChance = LEGIT_MISTAKE_CHANCE;
    }

    public void cyclePreset() {
        setPreset(preset.next());
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

    public double getEagleDelayMs() {
        return eagleDelayMs;
    }

    public void setEagleDelayMs(double eagleDelayMs) {
        this.eagleDelayMs = Math.max(0.0, Math.min(250.0, eagleDelayMs));
        saveConfig();
    }

    public double getSlowdown() {
        return slowdown;
    }

    public void setSlowdown(double slowdown) {
        this.slowdown = Math.max(0.2, Math.min(1.0, slowdown));
        saveConfig();
    }

    public double getMistakeChance() {
        return mistakeChance;
    }

    public void setMistakeChance(double mistakeChance) {
        this.mistakeChance = Math.max(0.0, Math.min(0.75, mistakeChance));
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
        preset = Preset.fromString(properties.getProperty("scaffold.preset", "STANDARD"));
        String delay = properties.getProperty("scaffold.auto_off_delay");
        if (delay != null) {
            try {
                autoOffDelay = Double.parseDouble(delay);
            } catch (Exception ignored) {}
        }
        String eagleDelay = properties.getProperty("scaffold.eagle_delay_ms");
        if (eagleDelay != null) {
            try {
                eagleDelayMs = Math.max(0.0, Math.min(250.0, Double.parseDouble(eagleDelay)));
            } catch (Exception ignored) {}
        }
        String slowdown = properties.getProperty("scaffold.legit_slowdown");
        if (slowdown != null) {
            try {
                this.slowdown = Math.max(0.2, Math.min(1.0, Double.parseDouble(slowdown)));
            } catch (Exception ignored) {}
        }
        String mistakeChance = properties.getProperty("scaffold.mistake_chance");
        if (mistakeChance != null) {
            try {
                this.mistakeChance = Math.max(0.0, Math.min(0.75, Double.parseDouble(mistakeChance)));
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("scaffold.tower", Boolean.toString(tower));
        properties.setProperty("scaffold.safewalk", Boolean.toString(safeWalk));
        properties.setProperty("scaffold.preset", preset.name());
        properties.setProperty("scaffold.auto_off_delay", Double.toString(autoOffDelay));
        properties.setProperty("scaffold.eagle_delay_ms", Double.toString(eagleDelayMs));
        properties.setProperty("scaffold.legit_slowdown", Double.toString(slowdown));
        properties.setProperty("scaffold.mistake_chance", Double.toString(mistakeChance));
    }
}
