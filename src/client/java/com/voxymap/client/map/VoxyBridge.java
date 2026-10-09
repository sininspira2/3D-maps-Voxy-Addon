package com.voxymap.client.map;

import me.cortex.voxy.client.config.VoxyConfig;
import me.cortex.voxy.client.core.NormalRenderPipeline;

public class VoxyBridge {

    private static NormalRenderPipeline.FogMode previousFogMode = null;

    public static void suppressEnvironmentalFogForMap() {
        // OFF drops both Voxy's own fog and the vanilla environmental fog over LODs.
        setFogMode(NormalRenderPipeline.FogMode.OFF, true);
    }

    public static void restoreEnvironmentalFogAfterMap() {
        if (previousFogMode == null) return;
        setFogMode(previousFogMode, false);
        previousFogMode = null;
    }

    private static void setFogMode(NormalRenderPipeline.FogMode value, boolean rememberPrevious) {
        try {
            var config = VoxyConfig.CONFIG;
            NormalRenderPipeline.FogMode current = config.getFogMode();
            if (rememberPrevious && previousFogMode == null) {
                previousFogMode = current;
            }
            if (current == value) {
                return;
            }
            config.setFogMode(value);
        } catch (Throwable ignored) {
        }
    }

}
