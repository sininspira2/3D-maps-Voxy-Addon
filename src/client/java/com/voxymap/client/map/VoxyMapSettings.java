package com.voxymap.client.map;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

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
    private static TimePreset timePreset = TimePreset.DAY;

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
        timePreset = TimePreset.fromId(properties.getProperty("timePreset"), timePreset);
    }

    public static void save() {
        Properties properties = new Properties();
        properties.setProperty("pauseSingleplayer", Boolean.toString(pauseSingleplayer));
        properties.setProperty("cameraSpeedMultiplier", String.format(Locale.ROOT, "%.2f", cameraSpeedMultiplier));
        properties.setProperty("timePreset", timePreset.id);

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

    public static TimePreset timePreset() {
        load();
        return timePreset;
    }

    public static void cycleTimePreset() {
        load();
        TimePreset[] values = TimePreset.values();
        timePreset = values[(timePreset.ordinal() + 1) % values.length];
        save();
    }

    public static boolean shouldOverrideTime() {
        load();
        return timePreset != TimePreset.REAL;
    }

    public static long mapDayTime() {
        load();
        return timePreset.timeTicks;
    }

    public static boolean mapIsDarkOutside() {
        load();
        return timePreset.darkOutside;
    }

    public static int mapSkyDarken() {
        load();
        return timePreset.skyDarken;
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

    public enum TimePreset {
        REAL("real", "setting.voxymap.time.real", 0L, false, 0),
        DAY("day", "setting.voxymap.time.day", 6000L, false, 0),
        SUNSET("sunset", "setting.voxymap.time.sunset", 12000L, false, 3),
        NIGHT("night", "setting.voxymap.time.night", 18000L, true, 11),
        DAWN("dawn", "setting.voxymap.time.dawn", 23000L, false, 2);

        private final String id;
        private final String translationKey;
        private final long timeTicks;
        private final boolean darkOutside;
        private final int skyDarken;

        TimePreset(String id, String translationKey, long timeTicks, boolean darkOutside, int skyDarken) {
            this.id = id;
            this.translationKey = translationKey;
            this.timeTicks = timeTicks;
            this.darkOutside = darkOutside;
            this.skyDarken = skyDarken;
        }

        public Component label() {
            return Component.translatable(translationKey);
        }

        private static TimePreset fromId(String id, TimePreset fallback) {
            if (id == null) return fallback;
            for (TimePreset preset : values()) {
                if (preset.id.equalsIgnoreCase(id)) {
                    return preset;
                }
            }
            return fallback;
        }
    }
}
