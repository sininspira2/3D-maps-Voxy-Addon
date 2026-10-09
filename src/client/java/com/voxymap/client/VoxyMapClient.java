package com.voxymap.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.voxymap.client.gui.MapScreen;
import com.voxymap.client.map.VoxyMapSettings;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
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
            boolean pressed = InputConstants.isKeyDown(InputConstants.KEY_M);
            if (pressed && !openMapPressed && client.gui.screen() == null) {
                client.gui.setScreen(new MapScreen());
            }
            openMapPressed = pressed;
        });

        LOGGER.info("[VoxyMap] Loaded. Press M to open the map.");
    }
}
