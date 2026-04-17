package com.phantom.mixin;

import com.phantom.PhantomMod;
import com.phantom.module.impl.render.TimeChanger;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public class LevelMixin {

    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
    private void onGetRainLevel(float delta, CallbackInfoReturnable<Float> cir) {
        if (PhantomMod.getModuleManager() != null) {
            TimeChanger tc = PhantomMod.getModuleManager().getModuleByClass(TimeChanger.class);
            if (tc != null && tc.isEnabled() && tc.getWeatherMode() != TimeChanger.WeatherMode.DEFAULT) {
                if (tc.getWeatherMode() == TimeChanger.WeatherMode.CLEAR) {
                    cir.setReturnValue(0.0f);
                } else if (tc.getWeatherMode() == TimeChanger.WeatherMode.RAIN || tc.getWeatherMode() == TimeChanger.WeatherMode.THUNDER) {
                    cir.setReturnValue(1.0f);
                }
            }
        }
    }

    @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
    private void onGetThunderLevel(float delta, CallbackInfoReturnable<Float> cir) {
        if (PhantomMod.getModuleManager() != null) {
            TimeChanger tc = PhantomMod.getModuleManager().getModuleByClass(TimeChanger.class);
            if (tc != null && tc.isEnabled() && tc.getWeatherMode() != TimeChanger.WeatherMode.DEFAULT) {
                if (tc.getWeatherMode() == TimeChanger.WeatherMode.THUNDER) {
                    cir.setReturnValue(1.0f);
                } else {
                    cir.setReturnValue(0.0f);
                }
            }
        }
    }
}
