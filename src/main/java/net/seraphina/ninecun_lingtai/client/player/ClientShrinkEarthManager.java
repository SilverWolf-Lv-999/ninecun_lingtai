package net.seraphina.ninecun_lingtai.client.player;

import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.network.packet.SetShrinkEarthPacket;
import net.seraphina.ninecun_lingtai.utility.ShrinkEarthUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientShrinkEarthManager {
    private static final long DOUBLE_TAP_WINDOW_MILLISECONDS = 300L;
    private static final int SMOOTH_TELEPORT_EXPECTATION_TICKS = 5;
    private static final double SMOOTH_TELEPORT_TARGET_TOLERANCE_SQR = 0.25D * 0.25D;
    private static final double SMOOTH_TELEPORT_START_TOLERANCE_SQR = 4.0D * 4.0D;
    private static final double SMOOTH_TELEPORT_MINIMUM_DISTANCE_SQR = 0.3D * 0.3D;
    private static final double SMOOTH_TELEPORT_MAXIMUM_DISTANCE_SQR = 512.0D * 512.0D;

    private static final Map<UUID, Integer> ACTIVE_PLAYERS = new ConcurrentHashMap<>();

    private static long lastControlAltPressTime;
    private static boolean controlAltPressed;
    private static Vec3 pendingSmoothTeleportFrom;
    private static Vec3 pendingSmoothTeleportTo;
    private static int pendingSmoothTeleportTicks;

    private ClientShrinkEarthManager() {
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (pendingSmoothTeleportTicks > 0 && --pendingSmoothTeleportTicks <= 0) {
            clearPendingSmoothTeleport();
        }
        if (minecraft.player == null || minecraft.screen != null || !isControlAltDown(minecraft)) {
            controlAltPressed = false;
        }
        if (ClientSoulRoamingManager.isLocalPlayerSoulRoaming()) {
            controlAltPressed = false;
        }
    }

    public static void onKeyInput(int key, int action) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null
                || minecraft.screen != null
                || ClientMeditationManager.isLocalPlayerMeditating()
                || ClientSoulRoamingManager.isLocalPlayerSoulRoaming()
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
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && player.getUUID().equals(playerId)) {
                clearPendingSmoothTeleport();
            }
        }
    }

    public static boolean isActive(UUID playerId) {
        return ACTIVE_PLAYERS.containsKey(playerId);
    }

    public static boolean isLocalPlayerActive() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && isActive(player.getUUID());
    }

    public static float getLocalWorldRenderScale() {
        return ShrinkEarthUtility.getWorldRenderScale(getLocalCompressionScale());
    }

    public static float getLocalWorldRenderScale(double horizontalDistance) {
        return ShrinkEarthUtility.getWorldRenderScale(
                getLocalCompressionScale(),
                horizontalDistance,
                getLocalFullCompressionDistance()
        );
    }

    public static int getLocalCompressionScale() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return 1;
        }
        return ACTIVE_PLAYERS.getOrDefault(player.getUUID(), 1);
    }

    public static Optional<Vec3> getLocalCompressionCenter() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !isActive(player.getUUID())) {
            return Optional.empty();
        }
        BlockPos supportingBlockPos = player.getOnPos();
        return Optional.of(Vec3.atCenterOf(supportingBlockPos));
    }

    public static void expectSmoothTeleport(Vec3 from, Vec3 to) {
        pendingSmoothTeleportFrom = from;
        pendingSmoothTeleportTo = to;
        pendingSmoothTeleportTicks = SMOOTH_TELEPORT_EXPECTATION_TICKS;
    }

    public static void smoothLocalTeleportIfExpected(Player player, Vec3 previousPosition) {
        if (pendingSmoothTeleportFrom == null
                || pendingSmoothTeleportTo == null
                || !isActive(player.getUUID())) {
            return;
        }
        Vec3 currentPosition = player.position();
        if (currentPosition.distanceToSqr(pendingSmoothTeleportTo) > SMOOTH_TELEPORT_TARGET_TOLERANCE_SQR) {
            return;
        }
        Vec3 expectedFrom = pendingSmoothTeleportFrom;
        clearPendingSmoothTeleport();
        double movementDistanceSqr = currentPosition.distanceToSqr(previousPosition);
        if (movementDistanceSqr < SMOOTH_TELEPORT_MINIMUM_DISTANCE_SQR
                || movementDistanceSqr > SMOOTH_TELEPORT_MAXIMUM_DISTANCE_SQR
                || previousPosition.distanceToSqr(expectedFrom) > SMOOTH_TELEPORT_START_TOLERANCE_SQR) {
            return;
        }
        player.xo = previousPosition.x;
        player.yo = previousPosition.y;
        player.zo = previousPosition.z;
        player.xOld = previousPosition.x;
        player.yOld = previousPosition.y;
        player.zOld = previousPosition.z;
    }

    public static void removePlayer(UUID playerId) {
        ACTIVE_PLAYERS.remove(playerId);
    }

    public static void reset() {
        ACTIVE_PLAYERS.clear();
        lastControlAltPressTime = 0L;
        controlAltPressed = false;
        clearPendingSmoothTeleport();
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

    private static double getLocalFullCompressionDistance() {
        Minecraft minecraft = Minecraft.getInstance();
        return Math.max(
                ShrinkEarthUtility.UNCHANGED_WORLD_RENDER_RADIUS + 1.0D,
                minecraft.options.getEffectiveRenderDistance() * 16.0D
        );
    }

    private static void clearPendingSmoothTeleport() {
        pendingSmoothTeleportFrom = null;
        pendingSmoothTeleportTo = null;
        pendingSmoothTeleportTicks = 0;
    }
}
