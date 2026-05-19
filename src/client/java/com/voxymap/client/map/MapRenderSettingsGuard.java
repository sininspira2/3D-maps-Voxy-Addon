package com.voxymap.client.map;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;

public final class MapRenderSettingsGuard {

    private static CloudStatus previousCloudStatus;

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
    }

    public static void restoreAfterMap(Minecraft minecraft) {
        if (minecraft != null && minecraft.options != null && previousCloudStatus != null) {
            if (minecraft.options.cloudStatus().get() != previousCloudStatus) {
                minecraft.options.cloudStatus().set(previousCloudStatus);
            }
        }

        previousCloudStatus = null;
    }
}
