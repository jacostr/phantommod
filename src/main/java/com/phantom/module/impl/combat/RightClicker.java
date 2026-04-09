/*
 * RightClicker.java — Automates right-clicking for blocks and usable items.
 *
 * Detectability: Moderate to Blatant — higher CPS and poor randomization are obvious.
 */
package com.phantom.module.impl.combat;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

public class RightClicker extends Module {
    public enum Randomization {
        NORMAL("Normal", 0.08D),
        EXTRA("Extra", 0.18D),
        EXTRA_PLUS("Extra+", 0.30D);

        private final String label;
        private final double variance;

        Randomization(String label, double variance) {
            this.label = label;
            this.variance = variance;
        }

        public String getLabel() {
            return label;
        }

        public double variance() {
            return variance;
        }

        public Randomization next() {
            return switch (this) {
                case NORMAL -> EXTRA;
                case EXTRA -> EXTRA_PLUS;
                case EXTRA_PLUS -> NORMAL;
            };
        }

        public static Randomization fromString(String value) {
            if (value == null) {
                return NORMAL;
            }
            String normalized = value.trim().toUpperCase(Locale.ROOT).replace("+", "_PLUS");
            try {
                return Randomization.valueOf(normalized);
            } catch (IllegalArgumentException ignored) {
                return NORMAL;
            }
        }
    }

    private double minCps = 10.0D;
    private double maxCps = 14.0D;
    private int startDelayMs;
    private int blockPlaceDelayMs = 75;
    private Randomization randomization = Randomization.NORMAL;
    private double jitter = 0.0D;
    private boolean useItemWhitelist = true;

    private long rightClickHeldSince = -1L;
    private long lastClickAt;
    private long nextDelayMs = 100L;

    public RightClicker() {
        super("RightClicker",
                "Automates right-clicking for scaffolding, bridging, and throwable spam.\nDetectability: Moderate to Blatant",
                ModuleCategory.COMBAT,
                -1);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.gameMode == null || mc.options == null || mc.screen != null) {
            resetTiming();
            return;
        }

        if (!mc.options.keyUse.isDown()) {
            resetTiming();
            return;
        }

        if (useItemWhitelist && !isWhitelisted(mc.player.getMainHandItem()) && !isWhitelisted(mc.player.getOffhandItem())) {
            resetTiming();
            return;
        }

        long now = System.currentTimeMillis();
        if (rightClickHeldSince < 0L) {
            rightClickHeldSince = now;
            scheduleNextDelay();
            return;
        }

        if (now - rightClickHeldSince < startDelayMs || now - lastClickAt < nextDelayMs) {
            return;
        }

        if (!clickOnce()) {
            return;
        }

        applyJitter();
        lastClickAt = now;
        scheduleNextDelay();
    }

    private boolean clickOnce() {
        if (mc.hitResult instanceof BlockHitResult blockHitResult) {
            InteractionResult result = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, blockHitResult);
            if (!result.consumesAction()) {
                result = mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            }
            if (result.consumesAction()) {
                mc.player.swing(InteractionHand.MAIN_HAND);
                nextDelayMs = Math.max(nextDelayMs, blockPlaceDelayMs);
                return true;
            }
            return false;
        }

        InteractionResult result = mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        if (result.consumesAction()) {
            mc.player.swing(InteractionHand.MAIN_HAND);
            return true;
        }
        return false;
    }

    private void applyJitter() {
        if (jitter <= 0.0D) {
            return;
        }
        float yawJitter = (float) ((ThreadLocalRandom.current().nextDouble() - 0.5D) * jitter * 2.0D);
        float pitchJitter = (float) ((ThreadLocalRandom.current().nextDouble() - 0.5D) * jitter * 1.2D);
        mc.player.setYRot(mc.player.getYRot() + yawJitter);
        mc.player.setXRot(Mth.clamp(mc.player.getXRot() + pitchJitter, -90.0F, 90.0F));
    }

    private void scheduleNextDelay() {
        double cps = ThreadLocalRandom.current().nextDouble(minCps, Math.max(minCps, maxCps) + 0.0001D);
        double baseDelay = 1000.0D / Math.max(0.1D, cps);
        double factor = 1.0D + ((ThreadLocalRandom.current().nextDouble() - 0.5D) * 2.0D * randomization.variance());
        nextDelayMs = Math.max(1L, Math.round(baseDelay * factor));
    }

    private void resetTiming() {
        rightClickHeldSince = -1L;
        lastClickAt = 0L;
    }

    private boolean isWhitelisted(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof BlockItem) {
            return true;
        }
        String id = stack.getItem().getDescriptionId().toLowerCase(Locale.ROOT);
        return id.contains("ender_pearl")
                || id.contains("snowball")
                || id.contains("egg")
                || id.contains("potion")
                || id.contains("rod")
                || id.contains("bow")
                || id.contains("crossbow")
                || id.contains("fireball");
    }

    public double getMinCps() {
        return minCps;
    }

    public void setMinCps(double minCps) {
        this.minCps = Mth.clamp(minCps, 1.0D, 20.0D);
        if (this.maxCps < this.minCps) {
            this.maxCps = this.minCps;
        }
        saveConfig();
    }

    public double getMaxCps() {
        return maxCps;
    }

    public void setMaxCps(double maxCps) {
        this.maxCps = Mth.clamp(maxCps, this.minCps, 20.0D);
        saveConfig();
    }

    public int getStartDelayMs() {
        return startDelayMs;
    }

    public void setStartDelayMs(int startDelayMs) {
        this.startDelayMs = Math.max(0, Math.min(1000, startDelayMs));
        saveConfig();
    }

    public int getBlockPlaceDelayMs() {
        return blockPlaceDelayMs;
    }

    public void setBlockPlaceDelayMs(int blockPlaceDelayMs) {
        this.blockPlaceDelayMs = Math.max(0, Math.min(1000, blockPlaceDelayMs));
        saveConfig();
    }

    public Randomization getRandomization() {
        return randomization;
    }

    public void cycleRandomization() {
        randomization = randomization.next();
        saveConfig();
    }

    public double getJitter() {
        return jitter;
    }

    public void setJitter(double jitter) {
        this.jitter = Mth.clamp(jitter, 0.0D, 3.0D);
        saveConfig();
    }

    public boolean isUseItemWhitelist() {
        return useItemWhitelist;
    }

    public void setUseItemWhitelist(boolean useItemWhitelist) {
        this.useItemWhitelist = useItemWhitelist;
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

    @Override
    public void loadConfig(Properties properties) {
        super.loadConfig(properties);
        String minValue = properties.getProperty("rightclicker.min_cps");
        if (minValue != null) {
            try {
                minCps = Mth.clamp(Double.parseDouble(minValue.trim()), 1.0D, 20.0D);
            } catch (NumberFormatException ignored) {
            }
        }
        String maxValue = properties.getProperty("rightclicker.max_cps");
        if (maxValue != null) {
            try {
                maxCps = Mth.clamp(Double.parseDouble(maxValue.trim()), minCps, 20.0D);
            } catch (NumberFormatException ignored) {
            }
        }
        String startValue = properties.getProperty("rightclicker.start_delay_ms");
        if (startValue != null) {
            try {
                startDelayMs = Math.max(0, Math.min(1000, Integer.parseInt(startValue.trim())));
            } catch (NumberFormatException ignored) {
            }
        }
        String blockDelayValue = properties.getProperty("rightclicker.block_place_delay_ms");
        if (blockDelayValue != null) {
            try {
                blockPlaceDelayMs = Math.max(0, Math.min(1000, Integer.parseInt(blockDelayValue.trim())));
            } catch (NumberFormatException ignored) {
            }
        }
        randomization = Randomization.fromString(properties.getProperty("rightclicker.randomization"));
        String jitterValue = properties.getProperty("rightclicker.jitter");
        if (jitterValue != null) {
            try {
                jitter = Mth.clamp(Double.parseDouble(jitterValue.trim()), 0.0D, 3.0D);
            } catch (NumberFormatException ignored) {
            }
        }
        useItemWhitelist = Boolean.parseBoolean(properties.getProperty("rightclicker.use_item_whitelist", Boolean.toString(useItemWhitelist)));
    }

    @Override
    public void saveConfig(Properties properties) {
        super.saveConfig(properties);
        properties.setProperty("rightclicker.min_cps", Double.toString(minCps));
        properties.setProperty("rightclicker.max_cps", Double.toString(maxCps));
        properties.setProperty("rightclicker.start_delay_ms", Integer.toString(startDelayMs));
        properties.setProperty("rightclicker.block_place_delay_ms", Integer.toString(blockPlaceDelayMs));
        properties.setProperty("rightclicker.randomization", randomization.name());
        properties.setProperty("rightclicker.jitter", Double.toString(jitter));
        properties.setProperty("rightclicker.use_item_whitelist", Boolean.toString(useItemWhitelist));
    }
}
