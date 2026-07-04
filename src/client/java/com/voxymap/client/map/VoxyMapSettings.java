package com.voxymap.client.map;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

public final class VoxyMapSettings {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("voxymap.properties");

    private static boolean loaded = false;
    private static boolean pauseSingleplayer = true;
    private static double cameraSpeedMultiplier = 1.0;

    private VoxyMapSettings() {
    }

    public static void load() {
        if (loaded) return;
        loaded = true;

        Properties properties = new Properties();
        if (Files.exists(CONFIG_PATH)) {
            try (InputStream input = Files.newInputStream(CONFIG_PATH)) {
                properties.load(input);
            } catch (IOException ignored) {
            }
        }

        pauseSingleplayer = Boolean.parseBoolean(properties.getProperty("pauseSingleplayer", Boolean.toString(pauseSingleplayer)));
        cameraSpeedMultiplier = clamp(readDouble(properties.getProperty("cameraSpeedMultiplier"), cameraSpeedMultiplier), 0.25, 4.0);
    }

    public static void save() {
        Properties properties = new Properties();
        properties.setProperty("pauseSingleplayer", Boolean.toString(pauseSingleplayer));
        properties.setProperty("cameraSpeedMultiplier", String.format(Locale.ROOT, "%.2f", cameraSpeedMultiplier));

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (OutputStream output = Files.newOutputStream(CONFIG_PATH)) {
                properties.store(output, "VoxyMap client settings");
            }
        } catch (IOException ignored) {
        }
    }

    public static boolean pauseSingleplayer() {
        load();
        return pauseSingleplayer;
    }

    public static void togglePauseSingleplayer() {
        load();
        pauseSingleplayer = !pauseSingleplayer;
        save();
    }

    public static double cameraSpeedMultiplier() {
        load();
        return cameraSpeedMultiplier;
    }

    public static void changeCameraSpeed(double delta) {
        load();
        cameraSpeedMultiplier = clamp(cameraSpeedMultiplier + delta, 0.25, 4.0);
        save();
    }

    private static double readDouble(String value, double fallback) {
        if (value == null) return fallback;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
