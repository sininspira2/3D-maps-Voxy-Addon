package com.voxymap.client.mixin;

import com.voxymap.client.map.VoxyMapCameraController;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Gui.class, remap = false)
public abstract class GuiMixin {
    @Inject(method = {"renderCrosshair", "extractCrosshair"}, at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void voxymap$hideCrosshair(CallbackInfo ci) {
        if (VoxyMapCameraController.isActive()) {
            ci.cancel();
        }
    }

    @Inject(method = {"renderItemHotbar", "extractItemHotbar"}, at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void voxymap$hideHotbar(CallbackInfo ci) {
        if (VoxyMapCameraController.isActive()) {
            ci.cancel();
        }
    }
}
