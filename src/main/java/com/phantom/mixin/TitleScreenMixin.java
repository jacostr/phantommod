package com.phantom.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void phantom$renderBranding(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        String text = "Phantom Mod";
        int x = 8;
        int y = 8;

        FontDescription cleanFont = new FontDescription.Resource(Identifier.fromNamespaceAndPath("minecraft", "uniform"));
        net.minecraft.network.chat.Component styledText = Component.literal(text).withStyle(s -> s.withFont(cleanFont));
        graphics.drawString(minecraft.font, styledText, x, y, 0xFFA8E6A3, true);
    }
}