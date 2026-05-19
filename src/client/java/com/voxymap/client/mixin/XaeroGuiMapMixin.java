package com.voxymap.client.mixin;

import com.voxymap.client.integration.VoxyMapReturnOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "xaero.map.gui.GuiMap", remap = false)
public abstract class XaeroGuiMapMixin extends Screen {

    private XaeroGuiMapMixin() {
        super(null);
    }

    @Inject(method = "method_25394", at = @At("RETURN"), remap = false)
    private void voxymap$renderReturnButton(GuiGraphics g, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        VoxyMapReturnOverlay.render(g, this.width, mouseX, mouseY);
    }

    @Inject(method = "method_25402", at = @At("HEAD"), cancellable = true, remap = false)
    private void voxymap$clickReturnButton(MouseButtonEvent event, boolean isInside, CallbackInfoReturnable<Boolean> cir) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_1 && VoxyMapReturnOverlay.click(this.width, event.x(), event.y())) {
            cir.setReturnValue(true);
        }
    }
}
