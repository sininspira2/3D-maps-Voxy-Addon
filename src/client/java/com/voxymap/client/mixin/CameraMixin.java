package com.voxymap.client.mixin;

import com.voxymap.client.map.VoxyMapCameraController;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Camera.class, remap = false)
public abstract class CameraMixin {
    @Shadow(remap = false)
    private boolean detached;

    @Shadow(remap = false)
    private float fov;

    @Shadow(remap = false)
    private float hudFov;

    @Shadow(remap = false)
    protected abstract void setPosition(double x, double y, double z);

    @Shadow(remap = false)
    protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "setup", at = @At("RETURN"), remap = false, require = 0)
    private void voxymap$useLegacyMapCamera(Level level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        voxymap$applyMapCamera();
    }

    @Inject(method = "update", at = @At("RETURN"), remap = false, require = 0)
    private void voxymap$useMapCamera(DeltaTracker deltaTracker, CallbackInfo ci) {
        voxymap$applyMapCamera();
    }

    @ModifyArg(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setupPerspective(FFFFF)V"), index = 2, remap = false, require = 0)
    private float voxymap$useMapProjectionFov(float original) {
        return VoxyMapCameraController.isActive() ? VoxyMapCameraController.fov() : original;
    }

    private void voxymap$applyMapCamera() {
        if (!VoxyMapCameraController.isActive()) {
            return;
        }

        this.detached = true;
        this.fov = VoxyMapCameraController.fov();
        this.hudFov = VoxyMapCameraController.fov();
        setPosition(VoxyMapCameraController.cameraX(), VoxyMapCameraController.cameraY(), VoxyMapCameraController.cameraZ());
        setRotation(VoxyMapCameraController.cameraYaw(), VoxyMapCameraController.cameraPitch());
    }
}
