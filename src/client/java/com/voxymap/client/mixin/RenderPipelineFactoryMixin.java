package com.voxymap.client.mixin;

import com.voxymap.client.map.VoxyMapGuiRenderer;
import me.cortex.voxy.client.core.AbstractRenderPipeline;
import me.cortex.voxy.client.core.RenderPipelineFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RenderPipelineFactory.class, remap = false)
public abstract class RenderPipelineFactoryMixin {
    @Inject(method = "createIrisPipeline", at = @At("HEAD"), cancellable = true, remap = false)
    private static void voxymap$useGuiPipeline(CallbackInfoReturnable<AbstractRenderPipeline> cir) {
        if (VoxyMapGuiRenderer.isActive()) {
            cir.setReturnValue(null);
        }
    }
}
