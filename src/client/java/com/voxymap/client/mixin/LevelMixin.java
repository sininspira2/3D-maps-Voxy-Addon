package com.voxymap.client.mixin;

import com.voxymap.client.map.VoxyMapCameraController;
import com.voxymap.client.map.VoxyMapSettings;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelMixin {
    private boolean voxymap$isClientMapTimeOverrideActive() {
        return VoxyMapCameraController.isActive()
                && VoxyMapSettings.shouldOverrideTime()
                && (Object) this instanceof ClientLevel;
    }

    private boolean voxymap$isClientMapWeatherOverrideActive() {
        return VoxyMapCameraController.isActive()
                && (Object) this instanceof ClientLevel;
    }

    @Inject(method = "getDayTime", at = @At("HEAD"), cancellable = true)
    private void voxymap$forceDayForMap(CallbackInfoReturnable<Long> cir) {
        if (voxymap$isClientMapTimeOverrideActive()) {
            cir.setReturnValue(VoxyMapSettings.mapDayTime());
        }
    }

    @Inject(method = "isDarkOutside", at = @At("HEAD"), cancellable = true)
    private void voxymap$clearDarknessForMap(CallbackInfoReturnable<Boolean> cir) {
        if (voxymap$isClientMapTimeOverrideActive()) {
            cir.setReturnValue(VoxyMapSettings.mapIsDarkOutside());
        }
    }

    @Inject(method = "getSkyDarken", at = @At("HEAD"), cancellable = true)
    private void voxymap$clearSkyDarkenForMap(CallbackInfoReturnable<Integer> cir) {
        if (voxymap$isClientMapTimeOverrideActive()) {
            cir.setReturnValue(VoxyMapSettings.mapSkyDarken());
        }
    }

    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
    private void voxymap$clearRainForMap(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (voxymap$isClientMapWeatherOverrideActive()) {
            cir.setReturnValue(0.0f);
        }
    }

    @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
    private void voxymap$clearThunderForMap(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (voxymap$isClientMapWeatherOverrideActive()) {
            cir.setReturnValue(0.0f);
        }
    }

    @Inject(method = "isRaining", at = @At("HEAD"), cancellable = true)
    private void voxymap$clearRainingStateForMap(CallbackInfoReturnable<Boolean> cir) {
        if (voxymap$isClientMapWeatherOverrideActive()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isThundering", at = @At("HEAD"), cancellable = true)
    private void voxymap$clearThunderingStateForMap(CallbackInfoReturnable<Boolean> cir) {
        if (voxymap$isClientMapWeatherOverrideActive()) {
            cir.setReturnValue(false);
        }
    }
}
