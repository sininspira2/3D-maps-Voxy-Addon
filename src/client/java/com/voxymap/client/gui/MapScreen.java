package com.voxymap.client.gui;

import com.voxymap.client.VoxyMapClient;
import com.voxymap.client.integration.XaeroWorldMapBridge;
import com.voxymap.client.map.MapRenderSettingsGuard;
import com.voxymap.client.map.VoxyBridge;
import com.voxymap.client.map.VoxyMapCameraController;
import com.voxymap.client.map.VoxyMapGuiRenderer;
import com.voxymap.client.map.VoxyMapSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public class MapScreen extends Screen {

    private static final Component TITLE = Component.translatable("screen.voxymap.title");

    private static final long OPEN_ANIMATION_NANOS = 850_000_000L;
    private static final long XAERO_TRANSITION_NANOS = 380_000_000L;
    private static final double MAP_VIEW_LEVEL = 320.0;
    private static final double MAX_KEYBOARD_CAMERA_SPEED = 640.0;
    private static final double MAX_DRAG_STEP_BLOCKS = 1536.0;
    private static boolean warnedUntestedVoxyVersion = false;

    private double viewCenterX;
    private double viewCenterY;
    private double viewCenterZ;
    private double blocksPerPixel = 4.0;
    private double viewYaw = Math.toRadians(45.0);
    private double viewPitch = 0.68;

    private boolean dragging = false;
    private boolean middleDragging = false;
    private double dragStartMouseX;
    private double dragStartYaw;
    private double dragLastMouseX;
    private double dragLastMouseY;
    private double lastMouseX = Double.NaN;
    private double lastMouseY = Double.NaN;
    private double moveVelocityX = 0.0;
    private double moveVelocityZ = 0.0;
    private long lastFrameNanos = 0L;
    private long openedAtNanos = 0L;
    private boolean xaeroTransitionActive = false;
    private long xaeroTransitionStartedAt = 0L;
    private int xaeroButtonX = -1;
    private int xaeroButtonY = -1;
    private int xaeroButtonW = 0;
    private int xaeroButtonH = 0;
    private boolean settingsOpen = false;
    private int settingsButtonX = -1;
    private int settingsButtonY = -1;
    private int settingsButtonW = 0;
    private int settingsButtonH = 0;
    private int settingsPanelX = -1;
    private int settingsPanelY = -1;
    private int settingsPanelW = 0;
    private int settingsPanelH = 0;
    private int speedMinusX = -1;
    private int speedMinusY = -1;
    private int speedPlusX = -1;
    private int speedPlusY = -1;
    private int pauseRowY = -1;
    private int speedRowY = -1;
    private int timeRowY = -1;
    private static final int SETTINGS_ROW_HEIGHT = 24;
    private static final int SPEED_BUTTON_SIZE = 18;

    private static final int BG = 0xFF080810;

    public MapScreen() {
        super(TITLE);
    }

    @Override
    protected void init() {
        super.init();
        openedAtNanos = System.nanoTime();
        lastFrameNanos = 0L;
        viewCenterY = MAP_VIEW_LEVEL;
        if (minecraft.player != null) {
            viewCenterX = minecraft.player.getX();
            viewCenterZ = minecraft.player.getZ();
        }
        if (!isUnsupportedDimension()) {
            VoxyBridge.suppressEnvironmentalFogForMap();
            MapRenderSettingsGuard.applyForMap(minecraft);
            syncWorldCamera();
            VoxyMapGuiRenderer.open(minecraft);
        } else {
            VoxyMapCameraController.deactivate();
        }
        if (!VoxyBridge.isTestedVoxyVersion() && !warnedUntestedVoxyVersion) {
            VoxyMapClient.LOGGER.warn("[VoxyMap] You are using Voxy {}, which has not been tested with VoxyMap. Visual glitches or crashes may occur. Tested Voxy versions: {}.",
                    VoxyBridge.getVoxyVersion(), VoxyBridge.testedVoxyVersionsText());
            warnedUntestedVoxyVersion = true;
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        if (isUnsupportedDimension()) {
            VoxyMapGuiRenderer.close(minecraft);
            VoxyMapCameraController.deactivate();
            drawUnsupportedDimension(g);
            drawOpeningAnimation(g);
            super.render(g, mouseX, mouseY, delta);
            return;
        }

        updateSmoothControls();
        VoxyBridge.suppressEnvironmentalFogForMap();
        MapRenderSettingsGuard.applyForMap(minecraft);
        syncWorldCamera();
        VoxyMapGuiRenderer.render(minecraft);
        drawWorldViewerOverlay(g, mouseX, mouseY);
        drawOpeningAnimation(g);
        if (xaeroTransitionActive) {
            drawXaeroTransition(g);
            if (System.nanoTime() - xaeroTransitionStartedAt >= XAERO_TRANSITION_NANOS) {
                openXaeroWorldMap();
                return;
            }
        }
        super.render(g, mouseX, mouseY, delta);
    }

    private double openingProgress() {
        if (openedAtNanos == 0L) return 1.0;
        double raw = (System.nanoTime() - openedAtNanos) / (double) OPEN_ANIMATION_NANOS;
        raw = Mth.clamp(raw, 0.0, 1.0);
        return 1.0 - Math.pow(1.0 - raw, 3.0);
    }

    private void updateSmoothControls() {
        if (minecraft == null || minecraft.getWindow() == null) return;

        long now = System.nanoTime();
        double dt = lastFrameNanos == 0L ? 1.0 / 60.0 : Math.min(0.08, (now - lastFrameNanos) / 1_000_000_000.0);
        lastFrameNanos = now;

        long window = minecraft.getWindow().handle();
        double forwardInput = keyDown(window, GLFW.GLFW_KEY_W) || keyDown(window, GLFW.GLFW_KEY_UP) ? 1.0 : 0.0;
        forwardInput -= keyDown(window, GLFW.GLFW_KEY_S) || keyDown(window, GLFW.GLFW_KEY_DOWN) ? 1.0 : 0.0;
        double strafeInput = keyDown(window, GLFW.GLFW_KEY_A) || keyDown(window, GLFW.GLFW_KEY_LEFT) ? 1.0 : 0.0;
        strafeInput -= keyDown(window, GLFW.GLFW_KEY_D) || keyDown(window, GLFW.GLFW_KEY_RIGHT) ? 1.0 : 0.0;

        double horizontalInput = Math.hypot(forwardInput, strafeInput);
        if (horizontalInput > 1.0) {
            forwardInput /= horizontalInput;
            strafeInput /= horizontalInput;
        }

        double speedBoost = keyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT) || keyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT) ? 2.5 : 1.0;
        double moveSpeed = Math.max(12.0, blocksPerPixel * 76.0) * speedBoost * VoxyMapSettings.cameraSpeedMultiplier();
        moveSpeed = Math.min(moveSpeed, MAX_KEYBOARD_CAMERA_SPEED * speedBoost);
        double forwardX = -Math.sin(viewYaw);
        double forwardZ = Math.cos(viewYaw);
        double rightX = Math.cos(viewYaw);
        double rightZ = Math.sin(viewYaw);
        double targetVX = dragging ? 0.0 : (forwardX * forwardInput + rightX * strafeInput) * moveSpeed;
        double targetVZ = dragging ? 0.0 : (forwardZ * forwardInput + rightZ * strafeInput) * moveSpeed;

        double response = 1.0 - Math.exp(-dt * 10.0);
        moveVelocityX = Mth.lerp(response, moveVelocityX, targetVX);
        moveVelocityZ = Mth.lerp(response, moveVelocityZ, targetVZ);

        if (!dragging) {
            viewCenterX += moveVelocityX * dt;
            viewCenterZ += moveVelocityZ * dt;
        }
    }

    private static boolean keyDown(long window, int key) {
        return GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
    }

    private GroundPoint mapPointAt(double mouseX, double mouseY) {
        if (width <= 0 || height <= 0 || !VoxyMapCameraController.isActive()) {
            return null;
        }

        double cameraX = VoxyMapCameraController.cameraX();
        double cameraY = VoxyMapCameraController.cameraY();
        double cameraZ = VoxyMapCameraController.cameraZ();
        double pitch = Math.toRadians(VoxyMapCameraController.cameraPitch());
        double planeY = viewCenterY;

        double forwardX = -Math.sin(viewYaw) * Math.cos(pitch);
        double forwardY = -Math.sin(pitch);
        double forwardZ = Math.cos(viewYaw) * Math.cos(pitch);
        double rightX = Math.cos(viewYaw);
        double rightZ = Math.sin(viewYaw);
        double upX = Math.sin(viewYaw) * Math.sin(pitch);
        double upY = Math.cos(pitch);
        double upZ = -Math.cos(viewYaw) * Math.sin(pitch);

        double tanY = Math.tan(Math.toRadians(VoxyMapCameraController.fov()) * 0.5);
        double tanX = tanY * width / (double) height;
        double ndcX = 1.0 - mouseX / width * 2.0;
        double ndcY = 1.0 - mouseY / height * 2.0;
        double rayX = forwardX + rightX * ndcX * tanX + upX * ndcY * tanY;
        double rayY = forwardY + upY * ndcY * tanY;
        double rayZ = forwardZ + rightZ * ndcX * tanX + upZ * ndcY * tanY;

        if (Math.abs(rayY) < 1.0E-6) {
            return null;
        }

        double t = (planeY - cameraY) / rayY;
        if (t <= 0.0 || !Double.isFinite(t)) {
            return null;
        }
        return new GroundPoint(cameraX + rayX * t, cameraZ + rayZ * t);
    }

    private void keepMapPointUnderMouse(GroundPoint anchor, double mouseX, double mouseY) {
        GroundPoint current = mapPointAt(mouseX, mouseY);
        if (anchor == null || current == null) {
            return;
        }
        viewCenterX += anchor.x - current.x;
        viewCenterZ += anchor.z - current.z;
    }

    private void panByPixels(double deltaX, double deltaY) {
        double rightX = Math.cos(viewYaw);
        double rightZ = Math.sin(viewYaw);
        double forwardX = -Math.sin(viewYaw);
        double forwardZ = Math.cos(viewYaw);
        double pitch = Math.toRadians(VoxyMapCameraController.cameraPitch());
        double forwardScale = 1.0 / Math.max(0.35, Math.sin(pitch));
        double sideDistance = deltaX * blocksPerPixel;
        double forwardDistance = deltaY * blocksPerPixel * forwardScale;
        double distance = Math.hypot(sideDistance, forwardDistance);
        if (distance > MAX_DRAG_STEP_BLOCKS) {
            double scale = MAX_DRAG_STEP_BLOCKS / distance;
            sideDistance *= scale;
            forwardDistance *= scale;
        }

        viewCenterX += rightX * sideDistance + forwardX * forwardDistance;
        viewCenterZ += rightZ * sideDistance + forwardZ * forwardDistance;
    }

    @Override
    public void tick() {
        if (!isUnsupportedDimension()) {
            syncWorldCamera();
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
    }

    private void syncWorldCamera() {
        if (!isUnsupportedDimension()) {
            VoxyMapCameraController.update(minecraft, viewCenterX, viewCenterY, viewCenterZ, viewYaw, viewPitch, blocksPerPixel);
        } else {
            VoxyMapCameraController.deactivate();
        }
    }

    private boolean isUnsupportedDimension() {
        if (minecraft == null || minecraft.level == null) return false;
        String dimension = minecraft.level.dimension().identifier().toString();
        return "minecraft:the_nether".equals(dimension);
    }

    private void drawUnsupportedDimension(GuiGraphics g) {
        g.fill(0, 0, width, height, BG);
        int boxW = Math.min(width - 40, 420);
        int boxH = 82;
        int x = (width - boxW) / 2;
        int y = (height - boxH) / 2;
        g.fill(x - 2, y - 2, x + boxW + 2, y + boxH + 2, 0x553BA4FF);
        g.fill(x, y, x + boxW, y + boxH, 0xEE030914);
        g.fill(x, y, x + 4, y + boxH, 0xFF3BA4FF);
        g.drawCenteredString(minecraft.font, Component.translatable("screen.voxymap.unsupported.title"), width / 2, y + 22, 0xFFFFFFFF);
        g.drawCenteredString(minecraft.font, Component.translatable(unsupportedDimensionMessageKey()), width / 2, y + 46, 0xFFBFD8FF);
    }

    private String unsupportedDimensionMessageKey() {
        if (minecraft != null && minecraft.level != null
                && "minecraft:the_end".equals(minecraft.level.dimension().identifier().toString())) {
            return "screen.voxymap.unsupported.end";
        }
        return "screen.voxymap.unsupported.nether";
    }

    private void drawOpeningAnimation(GuiGraphics g) {
        double progress = openingProgress();
        if (progress >= 0.995) return;

        int alpha = (int) (185 * (1.0 - progress));
        int dim = (alpha << 24) | 0x030914;
        g.fill(0, 0, width, height, dim);

        int bandHeight = (int) ((height * 0.28) * (1.0 - progress));
        if (bandHeight > 0) {
            g.fill(0, 0, width, bandHeight, 0xDD030914);
            g.fill(0, height - bandHeight, width, height, 0xDD030914);
            g.fill(0, bandHeight, width, bandHeight + 2, 0xFF3BA4FF);
            g.fill(0, height - bandHeight - 2, width, height - bandHeight, 0xFF3BA4FF);
        }

        int scanY = (int) (height * progress);
        g.fill(0, scanY - 1, width, scanY + 1, 0xAA72D7FF);
        String title = "VOXYMAP";
        int titleAlpha = (int) (255 * (1.0 - Math.abs(progress - 0.35) / 0.35));
        if (titleAlpha > 0) {
            int color = (Mth.clamp(titleAlpha, 0, 255) << 24) | 0xDFF8FF;
            g.drawCenteredString(minecraft.font, title, width / 2, height / 2 - 5, color);
        }
    }

    private void drawWorldViewerOverlay(GuiGraphics g, int mouseX, int mouseY) {
        int slide = (int) ((1.0 - openingProgress()) * 28.0);
        int topX = 12;
        int topY = 12 - slide;
        int topW = Math.min(width - 24, 328);
        drawGlassPanel(g, topX, topY, topW, 52, 0xAA030914, 0xFF3BA4FF);
        g.drawString(minecraft.font, Component.literal("VoxyMap 3D"), topX + 10, topY + 7, 0xFFFFFFFF);
        if (minecraft.player != null) {
            int px = (int) minecraft.player.getX();
            int py = (int) minecraft.player.getY();
            int pz = (int) minecraft.player.getZ();
            g.drawString(minecraft.font, Component.translatable("overlay.voxymap.player").getString() + "  " + px + " / " + py + " / " + pz, topX + 10, topY + 21, 0xFFDFF8FF);
        }
        g.drawString(minecraft.font, Component.translatable("overlay.voxymap.camera").getString() + "  " + (int) viewCenterX + " / " + (int) viewCenterY + " / " + (int) viewCenterZ
                        + "   " + Component.translatable("overlay.voxymap.zoom").getString() + " " + String.format("%.2f", blocksPerPixel),
                topX + 10, topY + 36, 0xFF9FD8FF);

        drawKeyHints(g, 12, height - 34 + slide);

        drawXaeroButton(g, mouseX, mouseY, slide);
        drawSettingsButton(g, mouseX, mouseY, slide);
        if (settingsOpen) {
            drawSettingsPanel(g, mouseX, mouseY, slide);
        }
    }

    private void drawGlassPanel(GuiGraphics g, int x, int y, int w, int h, int bg, int accent) {
        g.fill(x, y, x + w, y + h, bg);
        g.fill(x, y, x + 2, y + h, accent);
        g.fill(x, y, x + w, y + 1, 0x33FFFFFF);
        g.fill(x, y + h - 1, x + w, y + h, 0x55000000);
    }

    private void drawSettingsButton(GuiGraphics g, int mouseX, int mouseY, int slide) {
        String label = Component.translatable("overlay.voxymap.settings").getString();
        settingsButtonW = Math.min(156, Math.max(104, minecraft.font.width(label) + 28));
        settingsButtonH = 24;
        settingsButtonX = width - settingsButtonW - 12;
        settingsButtonY = height - 36 + slide;
        boolean hovered = isInsideSettingsButton(mouseX, mouseY);
        int bg = settingsOpen ? 0xD014283D : (hovered ? 0xCC112338 : 0x99030914);
        int accent = settingsOpen || hovered ? 0xFF7DEBFF : 0xFF3BA4FF;

        drawGlassPanel(g, settingsButtonX, settingsButtonY, settingsButtonW, settingsButtonH, bg, accent);
        int gearX = settingsButtonX + 13;
        int gearY = settingsButtonY + 12;
        g.fill(gearX - 4, gearY - 1, gearX + 4, gearY + 1, accent);
        g.fill(gearX - 1, gearY - 4, gearX + 1, gearY + 4, accent);
        g.drawString(minecraft.font, label, settingsButtonX + 26, settingsButtonY + 8, 0xFFE9F2FF);
    }

    private void drawSettingsPanel(GuiGraphics g, int mouseX, int mouseY, int slide) {
        settingsPanelW = Math.min(360, width - 24);
        settingsPanelH = 118;
        settingsPanelX = width - settingsPanelW - 12;
        settingsPanelY = Math.max(12, height - settingsPanelH - 68 + slide);
        drawGlassPanel(g, settingsPanelX, settingsPanelY, settingsPanelW, settingsPanelH, 0xDD030914, 0xFF3BA4FF);

        int x = settingsPanelX + 12;
        int y = settingsPanelY + 10;
        pauseRowY = y + 20;
        speedRowY = y + 44;
        timeRowY = y + 68;
        g.drawString(minecraft.font, Component.translatable("screen.voxymap.settings.title"), x, y, 0xFFFFFFFF);
        drawSettingValue(g, x, pauseRowY, "screen.voxymap.settings.pause", onOff(VoxyMapSettings.pauseSingleplayer()), 0);
        drawSettingValue(g, x, speedRowY, "screen.voxymap.settings.speed", String.format("%.2fx", VoxyMapSettings.cameraSpeedMultiplier()), 1);
        drawSettingValue(g, x, timeRowY, "screen.voxymap.settings.time", VoxyMapSettings.timePreset().label().getString(), 2);
    }

    private void drawSettingValue(GuiGraphics g, int x, int y, String labelKey, String value, int row) {
        int rowX = settingsPanelX + 8;
        int rowW = settingsPanelW - 16;
        g.fill(rowX, y, rowX + rowW, y + SETTINGS_ROW_HEIGHT - 2, 0x66102034);
        g.fill(rowX, y, rowX + 2, y + SETTINGS_ROW_HEIGHT - 2, 0xFF3BA4FF);
        g.drawString(minecraft.font, Component.translatable(labelKey), x, y + 7, 0xFFDFF8FF);
        int valueW = Math.max(62, minecraft.font.width(value) + 14);
        int valueX = settingsPanelX + settingsPanelW - valueW - 14;
        if (row == 1) {
            valueX -= (SPEED_BUTTON_SIZE + 6);
        }
        drawValueBox(g, valueX, y + 3, valueW, 16, value);
        if (row == 1) {
            speedMinusX = valueX - SPEED_BUTTON_SIZE - 6;
            speedMinusY = y + 2;
            speedPlusX = valueX + valueW + 6;
            speedPlusY = y + 2;
            drawSmallButton(g, speedMinusX, speedMinusY, "-", 0xCC102034);
            drawSmallButton(g, speedPlusX, speedPlusY, "+", 0xCC102034);
        }
    }

    private void drawValueBox(GuiGraphics g, int x, int y, int w, int h, String value) {
        g.fill(x, y, x + w, y + h, 0xAA102034);
        g.fill(x, y, x + 2, y + h, 0xFF3BA4FF);
        g.drawString(minecraft.font, value, x + 7, y + 4, 0xFFFFFFFF);
    }

    private void drawSmallButton(GuiGraphics g, int x, int y, String label, int bg) {
        g.fill(x, y, x + SPEED_BUTTON_SIZE, y + SPEED_BUTTON_SIZE, bg);
        g.fill(x, y, x + 2, y + SPEED_BUTTON_SIZE, 0xFF7DEBFF);
        g.drawCenteredString(minecraft.font, label, x + SPEED_BUTTON_SIZE / 2, y + 5, 0xFFFFFFFF);
    }

    private void drawKeyHints(GuiGraphics g, int x, int y) {
        String[][] hints = {
                {Component.translatable("overlay.voxymap.key.drag.button").getString(), Component.translatable("overlay.voxymap.key.drag").getString()},
                {Component.translatable("overlay.voxymap.key.rotate.button").getString(), Component.translatable("overlay.voxymap.key.rotate").getString()},
                {Component.translatable("overlay.voxymap.key.move.button").getString(), Component.translatable("overlay.voxymap.key.move").getString()},
                {Component.translatable("overlay.voxymap.key.fast.button").getString(), Component.translatable("overlay.voxymap.key.fast").getString()},
                {Component.translatable("overlay.voxymap.key.zoom.button").getString(), Component.translatable("overlay.voxymap.key.zoom").getString()},
                {Component.translatable("overlay.voxymap.key.center.button").getString(), Component.translatable("overlay.voxymap.key.center").getString()},
                {Component.translatable("overlay.voxymap.key.close.button").getString(), Component.translatable("overlay.voxymap.key.close").getString()}
        };
        int currentX = x;
        int maxX = width - 12;
        for (String[] hint : hints) {
            int w = minecraft.font.width(hint[0]) + minecraft.font.width(hint[1]) + 22;
            if (currentX + w > maxX) {
                break;
            }
            drawGlassPanel(g, currentX, y, w, 22, 0x99030914, 0xFF3BA4FF);
            g.drawString(minecraft.font, hint[0], currentX + 9, y + 7, 0xFFFFFFFF);
            g.drawString(minecraft.font, hint[1], currentX + 14 + minecraft.font.width(hint[0]), y + 7, 0xFFBFD8FF);
            currentX += w + 6;
        }
    }

    private String onOff(boolean enabled) {
        return Component.translatable(enabled ? "screen.voxymap.settings.on" : "screen.voxymap.settings.off").getString();
    }

    private void drawXaeroButton(GuiGraphics g, int mouseX, int mouseY, int slide) {
        if (!XaeroWorldMapBridge.isAvailable()) {
            xaeroButtonX = -1;
            xaeroButtonY = -1;
            xaeroButtonW = 0;
            xaeroButtonH = 0;
            return;
        }

        String label = Component.translatable("overlay.voxymap.xaero").getString();
        xaeroButtonW = Math.min(180, Math.max(118, minecraft.font.width(label) + 36));
        xaeroButtonH = 24;
        xaeroButtonX = width - xaeroButtonW - 12;
        xaeroButtonY = 12 - slide;
        boolean hovered = isInsideXaeroButton(mouseX, mouseY);
        int bg = hovered ? 0xCC112338 : 0x99030914;
        int accent = hovered ? 0xFF7DEBFF : 0xFF3BA4FF;

        g.fill(xaeroButtonX, xaeroButtonY, xaeroButtonX + xaeroButtonW, xaeroButtonY + xaeroButtonH, bg);
        g.fill(xaeroButtonX, xaeroButtonY, xaeroButtonX + 2, xaeroButtonY + xaeroButtonH, accent);
        int iconX = xaeroButtonX + 12;
        int iconY = xaeroButtonY + 12;
        g.fill(iconX - 5, iconY - 5, iconX + 5, iconY + 5, 0xFF102A38);
        g.fill(iconX - 4, iconY - 1, iconX + 4, iconY + 1, accent);
        g.fill(iconX - 1, iconY - 4, iconX + 1, iconY + 4, accent);
        g.drawString(minecraft.font, label, xaeroButtonX + 28, xaeroButtonY + 8, 0xFFE9F2FF);
    }

    private boolean isInsideXaeroButton(double mouseX, double mouseY) {
        return xaeroButtonW > 0
                && mouseX >= xaeroButtonX && mouseX <= xaeroButtonX + xaeroButtonW
                && mouseY >= xaeroButtonY && mouseY <= xaeroButtonY + xaeroButtonH;
    }

    private boolean isInsideSettingsButton(double mouseX, double mouseY) {
        return settingsButtonW > 0
                && mouseX >= settingsButtonX && mouseX <= settingsButtonX + settingsButtonW
                && mouseY >= settingsButtonY && mouseY <= settingsButtonY + settingsButtonH;
    }

    private boolean isInsideSettingsPanel(double mouseX, double mouseY) {
        return settingsOpen && settingsPanelW > 0
                && mouseX >= settingsPanelX && mouseX <= settingsPanelX + settingsPanelW
                && mouseY >= settingsPanelY && mouseY <= settingsPanelY + settingsPanelH;
    }

    private boolean handleSettingsClick(double mouseX, double mouseY) {
        if (isInsideSettingsButton(mouseX, mouseY)) {
            settingsOpen = !settingsOpen;
            return true;
        }
        if (!isInsideSettingsPanel(mouseX, mouseY)) {
            if (settingsOpen) {
                settingsOpen = false;
                return true;
            }
            return false;
        }

        if (mouseX >= speedMinusX && mouseX <= speedMinusX + SPEED_BUTTON_SIZE
                && mouseY >= speedMinusY && mouseY <= speedMinusY + SPEED_BUTTON_SIZE) {
            VoxyMapSettings.changeCameraSpeed(-0.25);
            return true;
        }
        if (mouseX >= speedPlusX && mouseX <= speedPlusX + SPEED_BUTTON_SIZE
                && mouseY >= speedPlusY && mouseY <= speedPlusY + SPEED_BUTTON_SIZE) {
            VoxyMapSettings.changeCameraSpeed(0.25);
            return true;
        }

        if (isInsideSettingsRow(mouseX, mouseY, pauseRowY)) {
            VoxyMapSettings.togglePauseSingleplayer();
        } else if (isInsideSettingsRow(mouseX, mouseY, speedRowY)) {
            double rowMid = settingsPanelX + settingsPanelW / 2.0;
            VoxyMapSettings.changeCameraSpeed(mouseX < rowMid ? -0.25 : 0.25);
        } else if (isInsideSettingsRow(mouseX, mouseY, timeRowY)) {
            VoxyMapSettings.cycleTimePreset();
        }
        return true;
    }

    private boolean isInsideSettingsRow(double mouseX, double mouseY, int rowY) {
        return rowY >= 0
                && mouseX >= settingsPanelX + 8 && mouseX <= settingsPanelX + settingsPanelW - 8
                && mouseY >= rowY && mouseY <= rowY + SETTINGS_ROW_HEIGHT - 2;
    }

    private void startXaeroTransition() {
        if (xaeroTransitionActive) return;
        dragging = false;
        middleDragging = false;
        xaeroTransitionActive = true;
        xaeroTransitionStartedAt = System.nanoTime();
    }

    private void drawXaeroTransition(GuiGraphics g) {
        double raw = (System.nanoTime() - xaeroTransitionStartedAt) / (double) XAERO_TRANSITION_NANOS;
        raw = Mth.clamp(raw, 0.0, 1.0);
        double eased = 1.0 - Math.pow(1.0 - raw, 3.0);
        int maxRadius = Math.max(width, height) + 120;
        int radius = (int) (maxRadius * eased);
        int originX = xaeroButtonX > 0 ? xaeroButtonX + xaeroButtonW / 2 : width - 80;
        int originY = xaeroButtonY > 0 ? xaeroButtonY + xaeroButtonH / 2 : 24;

        g.fill(0, 0, width, height, (int) (0x55000000 | ((long) (80 * eased) << 24)));
        g.fill(originX - radius, originY - radius / 3, originX + radius, originY + radius / 3, 0xDD07111F);
        g.fill(originX - radius / 3, originY - radius, originX + radius / 3, originY + radius, 0xAA0B3148);
        g.drawCenteredString(minecraft.font, Component.translatable("overlay.voxymap.xaero.transition"),
                width / 2, height / 2 + 22, 0xFFE9F2FF);
    }

    private void openXaeroWorldMap() {
        VoxyMapGuiRenderer.close(minecraft);
        VoxyMapCameraController.deactivate();
        VoxyBridge.restoreEnvironmentalFogAfterMap();
        MapRenderSettingsGuard.restoreAfterMap(minecraft);

        if (XaeroWorldMapBridge.openWorldMap(minecraft)) {
            return;
        }

        VoxyBridge.suppressEnvironmentalFogForMap();
        MapRenderSettingsGuard.applyForMap(minecraft);
        syncWorldCamera();
        VoxyMapGuiRenderer.open(minecraft);
        xaeroTransitionActive = false;
        xaeroTransitionStartedAt = 0L;
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.translatable("message.voxymap.xaero_open_failed"), false);
        }
    }


    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0.0) {
            return true;
        }
        double factor = Math.pow(0.82, scrollY);
        zoomAt(mouseX, mouseY, factor);
        return true;
    }

    private void zoomAt(double mouseX, double mouseY, double factor) {
        double oldBlocksPerPixel = blocksPerPixel;
        double newBlocksPerPixel = Mth.clamp(oldBlocksPerPixel * factor, 0.125, 256.0);
        if (newBlocksPerPixel == oldBlocksPerPixel) {
            return;
        }

        syncWorldCamera();
        GroundPoint anchor = mapPointAt(mouseX, mouseY);
        blocksPerPixel = newBlocksPerPixel;
        syncWorldCamera();
        keepMapPointUnderMouse(anchor, mouseX, mouseY);
        syncWorldCamera();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isInside) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_1 && handleSettingsClick(event.x(), event.y())) {
            return true;
        }
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_1 && isInsideXaeroButton(event.x(), event.y())) {
            startXaeroTransition();
            return true;
        }
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_1) {
            dragging = true;
            middleDragging = false;
            dragLastMouseX = event.x();
            dragLastMouseY = event.y();
            moveVelocityX = 0.0;
            moveVelocityZ = 0.0;
            return true;
        }
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            middleDragging = true;
            dragging = false;
            dragStartMouseX = event.x();
            dragStartYaw = viewYaw;
            return true;
        }
        return super.mouseClicked(event, isInside);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_1) {
            dragging = false;
            return true;
        }
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            middleDragging = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging) {
            panByPixels(event.x() - dragLastMouseX, event.y() - dragLastMouseY);
            dragLastMouseX = event.x();
            dragLastMouseY = event.y();
            syncWorldCamera();
            return true;
        }
        if (middleDragging) {
            viewYaw = dragStartYaw + (event.x() - dragStartMouseX) * 0.01;
            syncWorldCamera();
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || VoxyMapClient.openMapKey.matches(event)) {
            onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_W || keyCode == GLFW.GLFW_KEY_S ||
                keyCode == GLFW.GLFW_KEY_A || keyCode == GLFW.GLFW_KEY_D ||
                keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN ||
                keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_C && minecraft.player != null) {
            viewCenterX = minecraft.player.getX();
            viewCenterY = MAP_VIEW_LEVEL;
            viewCenterZ = minecraft.player.getZ();
            syncWorldCamera();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_EQUAL || keyCode == GLFW.GLFW_KEY_KP_ADD) {
            zoomAt(lastZoomMouseX(), lastZoomMouseY(), 0.8);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_MINUS || keyCode == GLFW.GLFW_KEY_KP_SUBTRACT) {
            zoomAt(lastZoomMouseX(), lastZoomMouseY(), 1.25);
            return true;
        }
        return super.keyPressed(event);
    }

    private double lastZoomMouseX() {
        return Double.isNaN(lastMouseX) ? width / 2.0 : lastMouseX;
    }

    private double lastZoomMouseY() {
        return Double.isNaN(lastMouseY) ? height / 2.0 : lastMouseY;
    }

    @Override
    public void onClose() {
        closeMapView();
        super.onClose();
    }

    @Override
    public void removed() {
        closeMapView();
        super.removed();
    }

    private void closeMapView() {
        VoxyMapGuiRenderer.close(minecraft);
        VoxyMapCameraController.deactivate();
        VoxyBridge.restoreEnvironmentalFogAfterMap();
        MapRenderSettingsGuard.restoreAfterMap(minecraft);
    }

    @Override
    public boolean isPauseScreen() {
        return VoxyMapSettings.pauseSingleplayer()
                && minecraft != null
                && minecraft.hasSingleplayerServer()
                && minecraft.isLocalServer()
                && minecraft.getSingleplayerServer() != null
                && !minecraft.getSingleplayerServer().isPublished();
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    private record GroundPoint(double x, double z) {
    }
}
