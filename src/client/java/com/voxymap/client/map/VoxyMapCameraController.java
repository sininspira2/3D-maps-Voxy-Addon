package com.voxymap.client.map;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;

public final class VoxyMapCameraController {
    private static boolean active;
    private static double cameraX;
    private static double cameraY;
    private static double cameraZ;
    private static double smoothedTargetY;
    private static long lastUpdateNanos;
    private static float cameraYaw;
    private static float cameraPitch;
    private static float fov;

    private VoxyMapCameraController() {
    }

    public static void update(Minecraft minecraft, double centerX, double centerZ, double yawRadians, double pitchControl, double blocksPerPixel) {
        if (minecraft == null || minecraft.level == null || minecraft.player == null) {
            active = false;
            return;
        }

        boolean endDimension = "minecraft:the_end".equals(minecraft.level.dimension().identifier().toString());
        double targetY = resolveStableTargetY(minecraft, centerX, centerZ, endDimension);
        if (endDimension) {
            targetY = Math.max(Math.max(targetY, minecraft.player.getY() + 12.0), 72.0);
        }
        float pitchDegrees = Mth.clamp((float) (18.0 + pitchControl * 42.0), 18.0f, 72.0f);
        float yawDegrees = (float) Math.toDegrees(yawRadians);
        double desiredRange = Mth.clamp(blocksPerPixel * 210.0, 64.0, 8192.0);
        double range;
        if (endDimension) {
            pitchDegrees = Mth.clamp((float) (28.0 + pitchControl * 34.0), 30.0f, 62.0f);
            range = Math.max(512.0, desiredRange);
            fov = Mth.clamp((float) (70.0 * desiredRange / range), 14.0f, 70.0f);
        } else {
            range = Math.max(1024.0, desiredRange);
            fov = Mth.clamp((float) (70.0 * desiredRange / range), 6.0f, 70.0f);
        }

        double pitchRadians = Math.toRadians(pitchDegrees);
        double horizontalRange = Math.cos(pitchRadians) * range;
        double verticalRange = Math.sin(pitchRadians) * range;
        double forwardX = -Math.sin(yawRadians);
        double forwardZ = Math.cos(yawRadians);

        cameraX = centerX - forwardX * horizontalRange;
        cameraY = targetY + verticalRange;
        cameraZ = centerZ - forwardZ * horizontalRange;
        cameraYaw = yawDegrees;
        cameraPitch = pitchDegrees;
        active = true;
    }

    private static double resolveStableTargetY(Minecraft minecraft, double centerX, double centerZ, boolean endDimension) {
        double playerAnchor = minecraft.player.getY() + 18.0;
        double safeMinimum = minecraft.level.getMinY() + 8.0;
        double fallbackY = Math.max(safeMinimum, playerAnchor);

        int blockX = Mth.floor(centerX);
        int blockZ = Mth.floor(centerZ);
        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;
        boolean hasVanillaChunk = minecraft.level.hasChunk(chunkX, chunkZ);

        double desiredY = fallbackY;
        if (hasVanillaChunk) {
            int surfaceY = minecraft.level.getHeight(Heightmap.Types.WORLD_SURFACE, blockX, blockZ);
            boolean suspiciousSurface = surfaceY <= minecraft.level.getMinY() + 2
                    || (!endDimension && surfaceY < minecraft.player.getY() - 96.0);
            if (!suspiciousSurface) {
                desiredY = Math.max(safeMinimum, surfaceY + 18.0);
            }
        } else if (active) {
            desiredY = smoothedTargetY;
        }

        long now = System.nanoTime();
        double dt = lastUpdateNanos == 0L ? 1.0 / 60.0 : Math.min(0.1, (now - lastUpdateNanos) / 1_000_000_000.0);
        lastUpdateNanos = now;

        if (!active || smoothedTargetY == 0.0 || Math.abs(smoothedTargetY - desiredY) > 96.0) {
            smoothedTargetY = desiredY;
        } else {
            double response = 1.0 - Math.exp(-dt * 5.0);
            smoothedTargetY = Mth.lerp(response, smoothedTargetY, desiredY);
        }
        return smoothedTargetY;
    }

    public static boolean isActive() {
        return active;
    }

    public static double cameraX() {
        return cameraX;
    }

    public static double cameraY() {
        return cameraY;
    }

    public static double cameraZ() {
        return cameraZ;
    }

    public static float cameraYaw() {
        return cameraYaw;
    }

    public static float cameraPitch() {
        return cameraPitch;
    }

    public static float fov() {
        return fov;
    }

    public static void deactivate() {
        active = false;
        lastUpdateNanos = 0L;
    }
}
