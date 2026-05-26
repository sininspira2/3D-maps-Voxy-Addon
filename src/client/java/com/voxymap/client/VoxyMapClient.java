package com.voxymap.client;

import com.voxymap.client.gui.MapScreen;
import com.voxymap.client.map.VoxyMapSettings;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VoxyMapClient implements ClientModInitializer {

    public static final String MOD_ID = "voxymap";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static KeyMapping openMapKey;

    @Override
    public void onInitializeClient() {
        VoxyMapSettings.load();

        KeyMapping.Category voxyMapCategory = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main")
        );

        openMapKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.voxymap.open_map",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                voxyMapCategory
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMapKey.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new MapScreen());
                }
            }
        });

        LOGGER.info("[VoxyMap] Loaded. Press M to open the map.");
    }
}
