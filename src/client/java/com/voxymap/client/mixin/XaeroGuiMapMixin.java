package com.voxymap.client.mixin;

import com.voxymap.client.integration.VoxyMapReturnOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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
public abstract class XaeroGuiMapMixin {
    @Inject(method = "method_25394", at = @At("RETURN"), remap = false, require = 0)
    private void voxymap$renderReturnButton(GuiGraphics g, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        VoxyMapReturnOverlay.render(g, Minecraft.getInstance().getWindow().getGuiScaledWidth(), mouseX, mouseY);
    }

    @Inject(method = {"method_25402", "mouseClicked"}, at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void voxymap$clickReturnButton(MouseButtonEvent event, boolean isInside, CallbackInfoReturnable<Boolean> cir) {
        int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_1 && VoxyMapReturnOverlay.click(width, event.x(), event.y())) {
            cir.setReturnValue(true);
        }
    }
}
