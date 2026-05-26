package com.voxymap.client.map;

import me.cortex.voxy.client.config.VoxyConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Set;

public class VoxyBridge {

    private static final Set<String> TESTED_VOXY_VERSIONS = Set.of("0.2.15-beta");

    private static final boolean VOXY_PRESENT;
    private static final String VOXY_VERSION;
    private static Boolean previousEnvironmentalFog = null;

    static {
        VOXY_PRESENT = FabricLoader.getInstance().isModLoaded("voxy");
        VOXY_VERSION = FabricLoader.getInstance().getModContainer("voxy")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("not installed");
    }

    public static String getVoxyVersion() {
        return VOXY_VERSION;
    }

    public static String testedVoxyVersionsText() {
        return String.join(", ", TESTED_VOXY_VERSIONS);
    }

    public static boolean isTestedVoxyVersion() {
        return VOXY_PRESENT && TESTED_VOXY_VERSIONS.contains(VOXY_VERSION);
    }

    public static void suppressEnvironmentalFogForMap() {
        setEnvironmentalFog(false, true);
    }

    public static void restoreEnvironmentalFogAfterMap() {
        if (previousEnvironmentalFog == null) return;
        setEnvironmentalFog(previousEnvironmentalFog, false);
        previousEnvironmentalFog = null;
    }

    private static void setEnvironmentalFog(boolean value, boolean rememberPrevious) {
        try {
            var config = VoxyConfig.CONFIG;
            boolean current = config.useEnvironmentalFog;
            if (rememberPrevious && previousEnvironmentalFog == null) {
                previousEnvironmentalFog = current;
            }
            if (current == value) {
                return;
            }
            config.useEnvironmentalFog = value;
        } catch (Throwable ignored) {
        }
    }

}
