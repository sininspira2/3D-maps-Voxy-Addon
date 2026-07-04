package com.voxymap.client;

import com.voxymap.client.gui.MapScreen;
import com.voxymap.client.map.VoxyMapSettings;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VoxyMapClient implements ClientModInitializer {

    public static final String MOD_ID = "voxymap";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static boolean openMapPressed;

    @Override
    public void onInitializeClient() {
        VoxyMapSettings.load();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            long window = client.getWindow().handle();
            boolean pressed = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_M) == GLFW.GLFW_PRESS;
            if (pressed && !openMapPressed && client.screen == null) {
                client.setScreen(new MapScreen());
            }
            openMapPressed = pressed;
        });

        LOGGER.info("[VoxyMap] Loaded. Press M to open the map.");
    }
}
