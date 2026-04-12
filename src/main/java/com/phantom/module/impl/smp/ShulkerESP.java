/*
 * ShulkerESP.java — Highlights shulker boxes through walls.
 *
 * Scans nearby block entities for shulker boxes and renders colored wireframes.
 * Detectability: Safe — purely client-side rendering.
 */
package com.phantom.module.impl.smp;

import com.phantom.gui.ModuleSettingsScreen;
import com.phantom.module.Module;
import com.phantom.module.ModuleCategory;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.lwjgl.opengl.GL11;
import com.phantom.util.ESPColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ShulkerESP extends Module {
    private double range = 64.0;
    private ESPColor colorMode = ESPColor.PURPLE;

    private List<BlockPos> cachedShulkers = new ArrayList<>();
    private long lastScanTick = 0;

    public ShulkerESP() {
        super("ShulkerESP", "Highlights shulker boxes through walls.\nDetectability: Safe",
                ModuleCategory.SMP, -1);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) return;
        long currentTick = mc.level.getGameTime();
        if (currentTick - lastScanTick >= 20) {
            lastScanTick = currentTick;
            rescan();
        }
    }

    private void rescan() {
        cachedShulkers.clear();
        BlockPos playerPos = mc.player.blockPosition();
        int r = (int) range;
        for (BlockPos pos : BlockPos.betweenClosed(playerPos.offset(-r, -r, -r), playerPos.offset(r, r, r))) {
            if (pos.distSqr(playerPos) > range * range) continue;
            BlockEntity be = mc.level.getBlockEntity(pos);
            if (be instanceof ShulkerBoxBlockEntity) {
                cachedShulkers.add(pos.immutable());
            }
        }
    }

    @Override
    public void onRender(WorldRenderContext context) {
        if (mc.level == null || mc.player == null || cachedShulkers.isEmpty()) return;
        MultiBufferSource consumers = context.consumers();
        if (consumers == null) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.position();

        GL11.glDepthFunc(GL11.GL_ALWAYS);
        try {
            for (BlockPos pos : cachedShulkers) {
                AABB box = new AABB(pos);
                var buffer = consumers.getBuffer(RenderTypes.lines());
                ShapeRenderer.renderShape(
                        context.matrices(), buffer,
                        Shapes.create(box.inflate(0.02D)),
                        -cameraPos.x, -cameraPos.y, -cameraPos.z,
                        colorMode.getColor(), 1.0F);
            }
            if (consumers instanceof MultiBufferSource.BufferSource bs) {
                bs.endBatch(RenderTypes.lines());
            }
        } finally {
            GL11.glDepthFunc(GL11.GL_LEQUAL);
        }
    }

    @Override
    public void onDisable() { cachedShulkers.clear(); lastScanTick = 0; }

    public double getRange() { return range; }
    public void setRange(double v) { range = Mth.clamp(v, 8.0, 128.0); saveConfig(); }
    public ESPColor getColorMode() { return colorMode; }
    public void cycleColorMode() { colorMode = colorMode.next(); saveConfig(); }

    @Override public boolean hasConfigurableSettings() { return true; }
    @Override public Screen createSettingsScreen(Screen parent) { return new ModuleSettingsScreen(parent, this); }

    @Override
    public void loadConfig(Properties p) {
        super.loadConfig(p);
        try { range = Mth.clamp(Double.parseDouble(p.getProperty("shulkeresp.range", "64.0")), 8.0, 128.0); } catch (Exception ignored) {}
        colorMode = ESPColor.fromString(p.getProperty("shulkeresp.color"), ESPColor.PURPLE);
    }

    @Override
    public void saveConfig(Properties p) {
        super.saveConfig(p);
        p.setProperty("shulkeresp.range", Double.toString(range));
        p.setProperty("shulkeresp.color", colorMode.name());
    }
}
