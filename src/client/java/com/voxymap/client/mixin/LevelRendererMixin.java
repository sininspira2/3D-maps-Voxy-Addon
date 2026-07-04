package com.voxymap.client.mixin;

import com.voxymap.client.map.VoxyMapCameraController;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, remap = false)
public abstract class LevelRendererMixin {
    @Inject(method = "addCloudsPass", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void voxymap$hideClouds(CallbackInfo ci) {
        if (VoxyMapCameraController.isActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "addWeatherPass", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void voxymap$hideWeather(CallbackInfo ci) {
        if (VoxyMapCameraController.isActive()) {
            ci.cancel();
        }
    }
}
