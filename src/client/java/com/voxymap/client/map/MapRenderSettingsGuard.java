package com.voxymap.client.map;

import net.fabricmc.loader.api.FabricLoader;
import net.irisshaders.iris.api.v0.IrisApiConfig;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;

public final class MapRenderSettingsGuard {

    private static final String IRIS_MOD_ID = "iris";
    private static CloudStatus previousCloudStatus;
    private static Boolean previousShadersEnabled;

    private MapRenderSettingsGuard() {
    }

    public static void applyForMap(Minecraft minecraft) {
        if (minecraft == null || minecraft.options == null) {
            return;
        }

        CloudStatus current = minecraft.options.cloudStatus().get();
        if (previousCloudStatus == null) {
            previousCloudStatus = current;
        }
        if (current != CloudStatus.OFF) {
            minecraft.options.cloudStatus().set(CloudStatus.OFF);
        }

        applyShaderSetting();
    }

    public static void restoreAfterMap(Minecraft minecraft) {
        if (minecraft != null && minecraft.options != null && previousCloudStatus != null) {
            if (minecraft.options.cloudStatus().get() != previousCloudStatus) {
                minecraft.options.cloudStatus().set(previousCloudStatus);
            }
        }

        previousCloudStatus = null;
        restoreShaders();
    }

    private static void applyShaderSetting() {
        if (!VoxyMapSettings.disableShadersDuringMap()) {
            restoreShaders();
            return;
        }

        IrisApiConfig config = getIrisConfig();
        if (config == null) return;

        try {
            boolean current = areIrisShadersEnabled(config);
            if (previousShadersEnabled == null) {
                previousShadersEnabled = current;
            }
            if (current) {
                setIrisShadersEnabled(config, false);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void restoreShaders() {
        if (previousShadersEnabled == null) return;

        IrisApiConfig config = getIrisConfig();
        if (config != null) {
            try {
                boolean current = areIrisShadersEnabled(config);
                if (current != previousShadersEnabled) {
                    setIrisShadersEnabled(config, previousShadersEnabled);
                }
            } catch (Throwable ignored) {
            }
        }

        previousShadersEnabled = null;
    }

    private static IrisApiConfig getIrisConfig() {
        if (!FabricLoader.getInstance().isModLoaded(IRIS_MOD_ID)) return null;
        try {
            return IrisAccess.getConfig();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean areIrisShadersEnabled(IrisApiConfig config) {
        return IrisAccess.areShadersEnabled(config);
    }

    private static void setIrisShadersEnabled(IrisApiConfig config, boolean value) {
        IrisAccess.setShadersEnabled(config, value);
    }

    private static final class IrisAccess {
        private static IrisApiConfig getConfig() {
            var api = net.irisshaders.iris.api.v0.IrisApi.getInstance();
            return api == null ? null : api.getConfig();
        }

        private static boolean areShadersEnabled(IrisApiConfig config) {
            return config.areShadersEnabled();
        }

        private static void setShadersEnabled(IrisApiConfig config, boolean value) {
            config.setShadersEnabledAndApply(value);
        }
    }
}
