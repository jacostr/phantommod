/*
 * Places a block under you when that space is air. Presets: Standard, Legit (sneak + pacing), God bridge (fast).
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

    public enum Preset {
        STANDARD("Standard"),
        LEGIT("Legit"),
        GOD_BRIDGE("God bridge");

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
                case LEGIT -> GOD_BRIDGE;
                case GOD_BRIDGE -> STANDARD;
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
    private int placeCooldown;
    private int towerJumpCooldown;
    private boolean sneakingFromModule;

    public Scaffold() {
        super("Scaffold",
                "Places blocks under you. Standard uses the tower toggle. Legit sneaks and slows pacing. God bridge is fast for bridging up.",
                ModuleCategory.BLATANT,
                -1);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null || mc.options == null) {
            return;
        }

        if (placeCooldown > 0) {
            placeCooldown--;
        }
        if (towerJumpCooldown > 0) {
            towerJumpCooldown--;
        }

        if (preset == Preset.LEGIT) {
            sneakingFromModule = true;
            mc.options.keyShift.setDown(true);
        } else {
            releaseSneak();
        }

        BlockPos pos = mc.player.blockPosition().below();
        if (mc.level.getBlockState(pos).isAir()) {
            boolean canPlace = preset != Preset.LEGIT || placeCooldown <= 0;
            if (canPlace) {
                placeBlock(pos);
                if (preset == Preset.LEGIT) {
                    placeCooldown = 2 + mc.player.getRandom().nextInt(3);
                }
            }
        }

        boolean towerActive = switch (preset) {
            case STANDARD -> tower;
            case LEGIT, GOD_BRIDGE -> true;
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
    }

    private void releaseSneak() {
        if (sneakingFromModule && mc.options != null) {
            mc.options.keyShift.setDown(false);
        }
        sneakingFromModule = false;
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
        if (this.preset != Preset.LEGIT) {
            releaseSneak();
        }
        saveConfig();
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

    @Override
    public void loadConfig(Properties properties) {
        super.loadConfig(properties);
        tower = Boolean.parseBoolean(properties.getProperty("scaffold.tower", "true"));
        preset = Preset.fromString(properties.getProperty("scaffold.preset", "STANDARD"));
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("scaffold.tower", Boolean.toString(tower));
        properties.setProperty("scaffold.preset", preset.name());
    }
}
