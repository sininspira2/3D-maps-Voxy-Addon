package com.voxymap.client.map;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.voxymap.client.VoxyMapClient;
import me.cortex.voxy.client.core.IGetVoxyRenderSystem;
import me.cortex.voxy.client.core.VoxyRenderSystem;
import me.cortex.voxy.client.core.rendering.Viewport;
import net.fabricmc.loader.api.FabricLoader;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL30C;

public final class VoxyMapGuiRenderer {
    private static final int BACKGROUND_COLOR = 0xFF080810;

    private static boolean active;
    private static boolean rendering;

    private VoxyMapGuiRenderer() {
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean shouldCancelWorldRender() {
        return active && !rendering;
    }

    public static void open(Minecraft minecraft) {
        if (active || minecraft == null || minecraft.level == null) {
            return;
        }
        if (!shouldRenderInGui()) {
            return;
        }

        active = true;
        try {
            rebuildRenderer(minecraft);
        } catch (RuntimeException | LinkageError exception) {
            active = false;
            VoxyMapClient.LOGGER.error("[VoxyMap] Could not prepare Voxy GUI rendering.", exception);
            restoreRenderer(minecraft);
        }
    }

    public static void close(Minecraft minecraft) {
        if (!active) {
            return;
        }

        rendering = false;
        active = false;
        restoreRenderer(minecraft);
    }

    public static void render(Minecraft minecraft) {
        if (!active || minecraft == null || minecraft.level == null) {
            return;
        }

        VoxyRenderSystem renderer = IGetVoxyRenderSystem.getNullable();
        if (renderer == null) {
            return;
        }

        Viewport<?> viewport = renderer.getViewport();
        if (viewport == null) {
            return;
        }

        var renderTarget = minecraft.getMainRenderTarget();
        if (!(RenderSystem.getDevice() instanceof GlDevice device)
                || !(renderTarget.getColorTexture() instanceof GlTexture colorTexture)
                || !(renderTarget.getDepthTexture() instanceof GlTexture depthTexture)) {
            return;
        }

        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
                colorTexture,
                BACKGROUND_COLOR,
                depthTexture,
                1.0
        );

        int framebuffer = colorTexture.getFbo(device.directStateAccess(), depthTexture);
        GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, framebuffer);
        rendering = true;
        try {
            renderer.renderOpaque(viewport);
        } finally {
            rendering = false;
            GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, 0);
            RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(depthTexture, 1.0);
        }
    }

    private static void rebuildRenderer(Minecraft minecraft) {
        IGetVoxyRenderSystem levelRenderer = (IGetVoxyRenderSystem) minecraft.levelRenderer;
        levelRenderer.voxy$shutdownRenderer();
        levelRenderer.voxy$createRenderer();
    }

    private static void restoreRenderer(Minecraft minecraft) {
        if (minecraft == null || minecraft.level == null) {
            return;
        }

        try {
            rebuildRenderer(minecraft);
        } catch (RuntimeException | LinkageError exception) {
            VoxyMapClient.LOGGER.error("[VoxyMap] Could not restore Voxy world rendering.", exception);
        }
    }

    private static boolean shouldRenderInGui() {
        if (!FabricLoader.getInstance().isModLoaded("iris")) {
            return false;
        }

        try {
            return IrisAccess.hasActiveShaderPack();
        } catch (LinkageError exception) {
            return false;
        }
    }

    private static final class IrisAccess {
        private static boolean hasActiveShaderPack() {
            return IrisApi.getInstance().isShaderPackInUse();
        }
    }
}
