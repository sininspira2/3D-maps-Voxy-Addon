package com.voxymap.client.map;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.voxymap.client.VoxyMapClient;
import me.cortex.voxy.client.core.IGetVoxyRenderSystem;
import me.cortex.voxy.client.core.VoxyRenderSystem;
import me.cortex.voxy.client.core.rendering.Viewport;
import net.fabricmc.loader.api.FabricLoader;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL30C;

public final class VoxyMapGuiRenderer {
    private static boolean active;
    private static boolean rendering;
    private static int framebuffer;
    private static int colorTexture;
    private static int depthTexture;
    private static int framebufferWidth;
    private static int framebufferHeight;

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
        releaseFramebuffer();
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

        RenderTarget renderTarget = minecraft.getMainRenderTarget();
        if (!(renderTarget.getColorTexture() instanceof GlTexture color) || !(renderTarget.getDepthTexture() instanceof GlTexture depth)) {
            return;
        }

        int width = renderTarget.width;
        int height = renderTarget.height;
        int colorId = color.glId();
        int depthId = depth.glId();
        if (width <= 0 || height <= 0 || colorId == 0 || depthId == 0 || !ensureFramebuffer(width, height, colorId, depthId)) {
            return;
        }

        int previousDrawFramebuffer = GL11C.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING);
        int previousReadFramebuffer = GL11C.glGetInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING);
        int[] previousViewport = new int[4];
        GL11C.glGetIntegerv(GL11C.GL_VIEWPORT, previousViewport);

        try {
            GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, framebuffer);
            GL11C.glViewport(0, 0, width, height);
            GL11C.glClearColor(0.03f, 0.03f, 0.06f, 1.0f);
            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT | GL11C.GL_DEPTH_BUFFER_BIT);

            rendering = true;
            renderer.renderOpaque(viewport);
            GL11C.glClear(GL11C.GL_DEPTH_BUFFER_BIT);
        } finally {
            rendering = false;
            GL30C.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, previousReadFramebuffer);
            GL30C.glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, previousDrawFramebuffer);
            GL11C.glViewport(previousViewport[0], previousViewport[1], previousViewport[2], previousViewport[3]);
        }
    }

    private static boolean ensureFramebuffer(int width, int height, int colorId, int depthId) {
        if (framebuffer != 0 && framebufferWidth == width && framebufferHeight == height && colorTexture == colorId && depthTexture == depthId) {
            return true;
        }

        releaseFramebuffer();

        framebufferWidth = width;
        framebufferHeight = height;
        colorTexture = colorId;
        depthTexture = depthId;
        framebuffer = GL30C.glGenFramebuffers();

        int previousDrawFramebuffer = GL11C.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING);
        int previousReadFramebuffer = GL11C.glGetInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING);

        GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, framebuffer);
        GL30C.glFramebufferTexture2D(GL30C.GL_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0, GL11C.GL_TEXTURE_2D, colorTexture, 0);
        GL30C.glFramebufferTexture2D(GL30C.GL_FRAMEBUFFER, GL30C.GL_DEPTH_ATTACHMENT, GL11C.GL_TEXTURE_2D, depthTexture, 0);

        boolean complete = GL30C.glCheckFramebufferStatus(GL30C.GL_FRAMEBUFFER) == GL30C.GL_FRAMEBUFFER_COMPLETE;

        GL30C.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, previousReadFramebuffer);
        GL30C.glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, previousDrawFramebuffer);

        if (!complete) {
            releaseFramebuffer();
        }

        return complete;
    }

    private static void releaseFramebuffer() {
        if (framebuffer != 0) {
            GL30C.glDeleteFramebuffers(framebuffer);
            framebuffer = 0;
        }
        colorTexture = 0;
        depthTexture = 0;
        framebufferWidth = 0;
        framebufferHeight = 0;
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
