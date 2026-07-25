package com.seraphina.ninecun_lingtai.client.screen;

import net.minecraft.client.Minecraft;

public final class ClientAptitudeScreenManager {
    private static boolean openRequested;

    private ClientAptitudeScreenManager() {
    }

    public static void requestOpen() {
        openRequested = true;
    }

    public static void tick() {
        if (!openRequested) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }
        openRequested = false;
        minecraft.setScreen(new AptitudeSelectionScreen());
    }

    public static void reset() {
        openRequested = false;
    }
}
