package com.seraphina.ninecun_lingtai.client.player;

import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.network.packet.SetShrinkEarthPacket;
import com.seraphina.ninecun_lingtai.utility.ShrinkEarthUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.lwjgl.glfw.GLFW;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientShrinkEarthManager {
    private static final long DOUBLE_TAP_WINDOW_MILLISECONDS = 300L;

    private static final Map<UUID, Integer> ACTIVE_PLAYERS = new ConcurrentHashMap<>();

    private static long lastControlAltPressTime;
    private static boolean controlAltPressed;

    private ClientShrinkEarthManager() {
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null || !isControlAltDown(minecraft)) {
            controlAltPressed = false;
        }
    }

    public static void onKeyInput(int key, int action) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null
                || minecraft.screen != null
                || ClientMeditationManager.isLocalPlayerMeditating()
                || !isControlOrAlt(key)) {
            return;
        }
        if (action == GLFW.GLFW_RELEASE && !isControlAltDown(minecraft)) {
            controlAltPressed = false;
            return;
        }
        if (action != GLFW.GLFW_PRESS || controlAltPressed || !isControlAltDown(minecraft)) {
            return;
        }
        controlAltPressed = true;
        long now = System.currentTimeMillis();
        if (now - lastControlAltPressTime <= DOUBLE_TAP_WINDOW_MILLISECONDS) {
            lastControlAltPressTime = 0L;
            requestActive(player, !isActive(player.getUUID()));
            return;
        }
        lastControlAltPressTime = now;
    }

    public static void setActive(UUID playerId, boolean active, int compressionScale) {
        if (active) {
            ACTIVE_PLAYERS.put(playerId, Math.max(1, compressionScale));
        } else {
            ACTIVE_PLAYERS.remove(playerId);
        }
    }

    public static boolean isActive(UUID playerId) {
        return ACTIVE_PLAYERS.containsKey(playerId);
    }

    public static boolean isLocalPlayerActive() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && isActive(player.getUUID());
    }

    public static float getLocalDepthRenderScale() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return 1.0F;
        }
        return ShrinkEarthUtility.getDepthRenderScale(ACTIVE_PLAYERS.getOrDefault(player.getUUID(), 1));
    }

    public static void removePlayer(UUID playerId) {
        ACTIVE_PLAYERS.remove(playerId);
    }

    public static void reset() {
        ACTIVE_PLAYERS.clear();
        lastControlAltPressTime = 0L;
        controlAltPressed = false;
    }

    private static void requestActive(LocalPlayer player, boolean active) {
        setActive(
                player.getUUID(),
                active,
                ACTIVE_PLAYERS.getOrDefault(player.getUUID(), ShrinkEarthUtility.ENTRY_COMPRESSION_SCALE)
        );
        ModNetwork.sendToServer(new SetShrinkEarthPacket(active));
    }

    private static boolean isControlOrAlt(int key) {
        return key == GLFW.GLFW_KEY_LEFT_CONTROL
                || key == GLFW.GLFW_KEY_RIGHT_CONTROL
                || key == GLFW.GLFW_KEY_LEFT_ALT
                || key == GLFW.GLFW_KEY_RIGHT_ALT;
    }

    private static boolean isControlAltDown(Minecraft minecraft) {
        long window = minecraft.getWindow().getWindow();
        boolean controlDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
        boolean altDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
        return controlDown && altDown;
    }
}
