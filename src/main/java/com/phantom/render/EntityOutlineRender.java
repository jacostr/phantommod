/*
 * EntityOutlineRender.java — 3D wireframe box drawing utility for ESP.
 *
 * Draws AABB wireframes in world-space using the LINES render type, which ignores
 * depth testing so boxes appear through walls. Used as a fallback rendering path
 * alongside the glowing-tag approach in ESP.java. Camera-relative translation is
 * handled via PoseStack so positions stay correct at any view angle.
 */
package com.phantom.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side AABB wireframes (works in multiplayer; does not rely on glowing
 * tag).
 */
public final class EntityOutlineRender {
    private EntityOutlineRender() {
    }

    public static void drawEntityBox(WorldRenderContext context, Entity entity, float r, float g, float b, float a) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 cam = camera.position();
        drawBox(context, entity.getBoundingBox(), cam, r, g, b, a);
    }

    public static void drawBox(WorldRenderContext context, AABB box, Vec3 cameraPos, float r, float g, float b,
            float a) {
        MultiBufferSource consumers = context.consumers();
        if (consumers == null) {
            return;
        }
        VertexConsumer buffer = consumers.getBuffer(RenderTypes.LINES);
        PoseStack pose = context.matrices();
        pose.pushPose();
        pose.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        line(buffer, pose, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, r, g, b, a);
        line(buffer, pose, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, r, g, b, a);
        line(buffer, pose, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, r, g, b, a);
        line(buffer, pose, box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ, r, g, b, a);
        line(buffer, pose, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, r, g, b, a);
        line(buffer, pose, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, r, g, b, a);
        line(buffer, pose, box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ, r, g, b, a);
        line(buffer, pose, box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ, r, g, b, a);
        line(buffer, pose, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, r, g, b, a);
        line(buffer, pose, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, r, g, b, a);
        line(buffer, pose, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, r, g, b, a);
        line(buffer, pose, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, r, g, b, a);
        pose.popPose();
    }

    private static void line(VertexConsumer c, PoseStack pose, double x1, double y1, double z1,
            double x2, double y2, double z2, float r, float g, float b, float a) {
        var mat = pose.last().pose();
        var n = pose.last();
        c.addVertex(mat, (float) x1, (float) y1, (float) z1).setColor(r, g, b, a).setNormal(n, 0f, 1f, 0f);
        c.addVertex(mat, (float) x2, (float) y2, (float) z2).setColor(r, g, b, a).setNormal(n, 0f, 1f, 0f);
    }
}
