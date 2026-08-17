package com.voxymap.client.mixin;

import com.voxymap.client.map.VoxyMapCameraController;
import com.voxymap.client.map.VoxyMapGuiRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GameRenderer.class, remap = false)
public abstract class GameRendererMixin {
    @Shadow(remap = false)
    @Final
    private Minecraft minecraft;

    @Inject(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render()V", shift = At.Shift.BEFORE),
            remap = false
    )
    private void voxymap$renderGuiMapBeforeHud(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        VoxyMapGuiRenderer.render(minecraft);
    }

    @Inject(method = "renderItemInHand(Lnet/minecraft/client/renderer/state/level/CameraRenderState;FLorg/joml/Matrix4fc;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void voxymap$hideHandInMap(CameraRenderState cameraRenderState, float tickDelta, Matrix4fc projectionMatrix, CallbackInfo ci) {
        if (VoxyMapCameraController.isActive()) {
            ci.cancel();
        }
    }
}
