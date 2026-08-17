package com.voxymap.client.mixin;

import com.voxymap.client.map.VoxyMapCameraController;
import net.minecraft.core.Holder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.ClientClockManager", remap = false)
public abstract class ClientClockManagerMixin {
    @Inject(method = "getTotalTicks", at = @At("HEAD"), cancellable = true, remap = false)
    private void voxymap$forceDayForMap(Holder<?> clock, CallbackInfoReturnable<Long> cir) {
        if (VoxyMapCameraController.isActive()) {
            cir.setReturnValue(6000L);
        }
    }
}
