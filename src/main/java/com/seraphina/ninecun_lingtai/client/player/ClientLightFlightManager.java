package com.seraphina.ninecun_lingtai.client.player;

import com.seraphina.ninecun_lingtai.client.animation.LightFlightAnimationManager;
import com.seraphina.ninecun_lingtai.common.flight.LightFlightStyle;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.network.packet.BoostLightFlightPacket;
import com.seraphina.ninecun_lingtai.common.network.packet.SetLightFlightPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClientLightFlightManager {
    private static final int DEFAULT_LIGHT_COLOR = 0xF2F7FF;
    private static final long DOUBLE_TAP_WINDOW_MILLISECONDS = 300L;
    private static final double BOOST_FOV_STRETCH = 0.08D;

    private static final Map<UUID, LightFlightVisualState> VISUAL_STATES = new HashMap<>();

    private static long lastJumpPressTime;

    private ClientLightFlightManager() {
    }

    public static void tick() {
        VISUAL_STATES.values().forEach(LightFlightVisualState::tick);
        VISUAL_STATES.entrySet().removeIf(entry -> entry.getValue().hasFinishedExiting());

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && !VISUAL_STATES.isEmpty()) {
            for (Player player : minecraft.level.players()) {
                if (isFlying(player.getUUID())) {
                    enforceFlightPose(player);
                }
            }
        }

        LocalPlayer player = minecraft.player;
        if (player != null
                && isFlying(player.getUUID())
                && player.getAbilities().mayfly
                && !player.getAbilities().flying) {
            player.getAbilities().flying = true;
        }
    }

    public static void onJumpPressed() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null
                || minecraft.screen != null
                || ClientMeditationManager.isLocalPlayerMeditating()
                || !isShiftDown(minecraft)) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastJumpPressTime <= DOUBLE_TAP_WINDOW_MILLISECONDS) {
            lastJumpPressTime = 0L;
            requestFlying(player, !isFlying(player.getUUID()));
            return;
        }
        lastJumpPressTime = now;
    }

    public static void onControlPressed() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null
                || minecraft.screen != null
                || ClientMeditationManager.isLocalPlayerMeditating()
                || !isFlying(player.getUUID())) {
            return;
        }
        LightFlightVisualState visualState = VISUAL_STATES.get(player.getUUID());
        if (visualState == null || visualState.style() != LightFlightStyle.LIGHT) {
            return;
        }
        setBoosting(player.getUUID(), true);
        ModNetwork.sendToServer(BoostLightFlightPacket.INSTANCE);
    }

    public static void setFlying(
            UUID playerId,
            boolean flying,
            int color,
            boolean boosting,
            LightFlightStyle style
    ) {
        LightFlightStyle sanitizedStyle = style == null ? LightFlightStyle.LIGHT : style;
        boolean effectiveBoosting = sanitizedStyle == LightFlightStyle.LIGHT && boosting;
        if (flying) {
            VISUAL_STATES.compute(
                    playerId,
                    (id, visualState) -> {
                        if (visualState == null) {
                            return new LightFlightVisualState(sanitizedStyle, color, effectiveBoosting);
                        }
                        visualState.update(sanitizedStyle, color, effectiveBoosting);
                        return visualState;
                    }
            );
        } else {
            LightFlightVisualState visualState = VISUAL_STATES.get(playerId);
            if (visualState != null) {
                visualState.stopFlying();
            }
        }
        LightFlightAnimationManager.setFlying(playerId, flying, sanitizedStyle);
    }

    public static boolean isFlying(UUID playerId) {
        LightFlightVisualState visualState = VISUAL_STATES.get(playerId);
        return visualState != null && visualState.isFlying();
    }

    public static boolean isLocalPlayerFlying() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && isFlying(player.getUUID());
    }

    @Nullable
    public static LightFlightVisualState getVisualState(UUID playerId) {
        return VISUAL_STATES.get(playerId);
    }

    public static Map<UUID, LightFlightVisualState> getVisualStates() {
        return Collections.unmodifiableMap(VISUAL_STATES);
    }

    public static LightFlightStyle getStyle(UUID playerId) {
        LightFlightVisualState visualState = VISUAL_STATES.get(playerId);
        return visualState == null ? LightFlightStyle.LIGHT : visualState.style();
    }

    public static double getFovMultiplier(float partialTick) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return 1.0D;
        }
        LightFlightVisualState visualState = VISUAL_STATES.get(player.getUUID());
        if (visualState == null) {
            return 1.0D;
        }
        if (visualState.style() != LightFlightStyle.LIGHT) {
            return 1.0D;
        }
        float boostVisibility = visualState.boostVisibility(partialTick);
        if (boostVisibility <= 0.0F) {
            return 1.0D;
        }
        float easedBoost = boostVisibility * boostVisibility * (3.0F - 2.0F * boostVisibility);
        return 1.0D + BOOST_FOV_STRETCH * easedBoost;
    }

    public static void removePlayer(UUID playerId) {
        VISUAL_STATES.remove(playerId);
        LightFlightAnimationManager.setFlying(playerId, false, LightFlightStyle.LIGHT);
    }

    public static void reset() {
        VISUAL_STATES.keySet().forEach(
                playerId -> LightFlightAnimationManager.setFlying(playerId, false, LightFlightStyle.LIGHT)
        );
        VISUAL_STATES.clear();
        lastJumpPressTime = 0L;
    }

    private static void requestFlying(LocalPlayer player, boolean flying) {
        LightFlightStyle style = getPredictedStyle(player.getUUID());
        setFlying(
                player.getUUID(),
                flying,
                getVisualStateColor(player.getUUID()),
                false,
                style
        );
        ModNetwork.sendToServer(new SetLightFlightPacket(flying));
    }

    private static int getVisualStateColor(UUID playerId) {
        LightFlightVisualState visualState = VISUAL_STATES.get(playerId);
        return visualState == null ? DEFAULT_LIGHT_COLOR : visualState.color();
    }

    private static void setBoosting(UUID playerId, boolean boosting) {
        LightFlightVisualState visualState = VISUAL_STATES.get(playerId);
        if (visualState != null) {
            visualState.update(
                    visualState.style(),
                    visualState.color(),
                    visualState.style() == LightFlightStyle.LIGHT && boosting
            );
        }
    }

    private static LightFlightStyle getPredictedStyle(UUID playerId) {
        LightFlightVisualState visualState = VISUAL_STATES.get(playerId);
        if (visualState != null) {
            return visualState.style();
        }
        return ClientCultivationInfoManager.getCultivationInfo()
                .flatMap(cultivationInfo -> cultivationInfo.cultivationStage())
                .flatMap(stage -> LightFlightStyle.forRealm(stage.realm()))
                .orElse(LightFlightStyle.AIR_WALKING);
    }

    private static void enforceFlightPose(Player player) {
        player.setPose(Pose.STANDING);
        player.walkAnimation.update(0.0F, 1.0F);
        player.walkAnimation.update(0.0F, 1.0F);
    }

    private static boolean isShiftDown(Minecraft minecraft) {
        long window = minecraft.getWindow().getWindow();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }
}
