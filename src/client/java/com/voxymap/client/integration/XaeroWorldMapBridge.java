package com.voxymap.client.integration;

import com.voxymap.client.VoxyMapClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class XaeroWorldMapBridge {

    private static final String XAERO_MOD_ID = "xaeroworldmap";

    private static Boolean available;
    private static Method getCurrentSessionMethod;
    private static Method getMapProcessorMethod;
    private static Constructor<?> guiMapConstructor;

    private XaeroWorldMapBridge() {
    }

    public static boolean isAvailable() {
        if (available == null) {
            available = FabricLoader.getInstance().isModLoaded(XAERO_MOD_ID);
        }
        return available;
    }

    public static boolean openWorldMap(Minecraft client) {
        if (!isAvailable() || client == null || client.player == null) {
            return false;
        }

        try {
            ensureReflectionReady();
            Object session = getCurrentSessionMethod.invoke(null);
            if (session == null) {
                VoxyMapClient.LOGGER.warn("[VoxyMap] Xaero World Map is installed, but its current session is not ready yet.");
                return false;
            }

            Object mapProcessor = getMapProcessorMethod.invoke(session);
            if (mapProcessor == null) {
                VoxyMapClient.LOGGER.warn("[VoxyMap] Xaero World Map session has no active map processor.");
                return false;
            }

            Object xaeroScreen = guiMapConstructor.newInstance(null, null, mapProcessor, client.player);
            if (!(xaeroScreen instanceof Screen screen)) {
                VoxyMapClient.LOGGER.warn("[VoxyMap] Xaero GuiMap did not create a Minecraft Screen.");
                return false;
            }

            client.setScreen(screen);
            return true;
        } catch (Throwable t) {
            VoxyMapClient.LOGGER.warn("[VoxyMap] Could not open Xaero World Map from VoxyMap: {}", t.toString());
            return false;
        }
    }

    private static void ensureReflectionReady() throws ReflectiveOperationException {
        if (getCurrentSessionMethod != null && getMapProcessorMethod != null && guiMapConstructor != null) {
            return;
        }

        Class<?> sessionClass = Class.forName("xaero.map.WorldMapSession");
        Class<?> mapProcessorClass = Class.forName("xaero.map.MapProcessor");
        Class<?> guiMapClass = Class.forName("xaero.map.gui.GuiMap");

        getCurrentSessionMethod = sessionClass.getDeclaredMethod("getCurrentSession");
        getCurrentSessionMethod.setAccessible(true);
        getMapProcessorMethod = sessionClass.getDeclaredMethod("getMapProcessor");
        getMapProcessorMethod.setAccessible(true);
        guiMapConstructor = guiMapClass.getConstructor(
                Screen.class,
                Screen.class,
                mapProcessorClass,
                net.minecraft.world.entity.Entity.class
        );
    }
}
