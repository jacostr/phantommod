/*
 * MinecraftClientMixin.java — Placeholder mixin on the Minecraft client class.
 *
 * Currently empty — kept for future use. All tick-based logic is handled via
 * Fabric's ClientTickEvents in PhantomMod.java instead of injecting directly
 * into Minecraft.tick(). This mixin exists so it's already registered in
 * phantom.mixins.json and ready when we need to hook client internals.
 */
package com.phantom.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

// Kept for future use. Ticking handled via ClientTickEvents in PhantomMod.java
@Mixin(Minecraft.class)
public class MinecraftClientMixin {
}