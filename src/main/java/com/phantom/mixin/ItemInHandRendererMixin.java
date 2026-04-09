package com.phantom.mixin;

import com.phantom.PhantomMod;
import com.phantom.module.impl.combat.BlockHit;
import com.phantom.module.impl.combat.AutoBlock;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {

    @Inject(
        method = "renderArmWithItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;applyItemArmTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V",
            shift = At.Shift.AFTER
        )
    )
    private void onRenderArmWithItem(
        AbstractClientPlayer player,
        float tickDelta,
        float pitch,
        InteractionHand hand,
        float swingProgress,
        ItemStack stack,
        float equipProgress,
        PoseStack matrices,
        SubmitNodeCollector vertexConsumers,
        int light,
        CallbackInfo ci
    ) {
        if (hand == InteractionHand.MAIN_HAND) {
            BlockHit blockHit = (BlockHit) PhantomMod.getModuleManager().getModuleByName("BlockHit");
            AutoBlock autoBlock = (AutoBlock) PhantomMod.getModuleManager().getModuleByName("AutoBlock");

            boolean isBlocking =
                    (blockHit != null && blockHit.isEnabled() && blockHit.isVisualAnimation() && blockHit.isHoldingUse())
                    || (autoBlock != null && autoBlock.isEnabled() && autoBlock.isHoldingUse());

            if (isBlocking && stack.getItem().getDescriptionId().toLowerCase().contains("sword")) {
                matrices.translate(0.15f, -0.1f, -0.1f);
                matrices.mulPose(Axis.YP.rotationDegrees(-60.0f));
                matrices.mulPose(Axis.XP.rotationDegrees(-15.0f));
                matrices.translate(-0.5f, 0.4f, 0.0f);
            }
        }
    }
}
