package com.voxymap.client.map;

import me.cortex.voxy.client.config.VoxyConfig;

public class VoxyBridge {

    private static Boolean previousEnvironmentalFog = null;

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
