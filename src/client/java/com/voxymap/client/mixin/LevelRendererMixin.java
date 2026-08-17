package com.voxymap.client.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.voxymap.client.map.VoxyMapCameraController;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, remap = false)
public abstract class LevelRendererMixin {
    @Inject(method = "addCloudsPass", at = @At("HEAD"), cancellable = true, remap = false)
    private void voxymap$hideClouds(FrameGraphBuilder frameGraphBuilder, CloudStatus cloudStatus, Vec3 cameraPosition,
                                    long ticks, float partialTick, int color, float cloudHeight, int viewDistance,
                                    CallbackInfo ci) {
        if (VoxyMapCameraController.isActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "addWeatherPass", at = @At("HEAD"), cancellable = true, remap = false)
    private void voxymap$hideWeather(FrameGraphBuilder frameGraphBuilder, GpuBufferSlice fogBuffer, CallbackInfo ci) {
        if (VoxyMapCameraController.isActive()) {
            ci.cancel();
        }
    }
}
