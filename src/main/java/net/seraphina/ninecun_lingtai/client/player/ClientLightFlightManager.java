package net.seraphina.ninecun_lingtai.client.player;

import net.seraphina.ninecun_lingtai.client.animation.LightFlightAnimationManager;
import net.seraphina.ninecun_lingtai.common.flight.LightFlightStyle;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.network.packet.BoostLightFlightPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.SetLightFlightPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.Input;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
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
    private static final float STOP_IMPACT_STEP = 0.075F;

    private static final Map<UUID, LightFlightVisualState> VISUAL_STATES = new HashMap<>();

    private static long lastJumpPressTime;
    private static float previousStopImpactProgress;
    private static float stopImpactProgress = 1.0F;
    private static float previousStopImpactTime;
    private static float stopImpactTime = 1.0F;
    private static Vec3 previousStopImpactPosition = Vec3.ZERO;
    private static Vec3 stopImpactPosition = Vec3.ZERO;
    private static int stopImpactColor = DEFAULT_LIGHT_COLOR;

    private ClientLightFlightManager() {
    }

    public static void tick() {
        VISUAL_STATES.values().forEach(LightFlightVisualState::tick);
        VISUAL_STATES.entrySet().removeIf(entry -> entry.getValue().hasFinishedExiting());
        previousStopImpactProgress = stopImpactProgress;
        previousStopImpactTime = stopImpactTime;
        if (stopImpactProgress < 1.0F) {
            stopImpactProgress = Math.min(1.0F, stopImpactProgress + STOP_IMPACT_STEP);
            stopImpactTime += 1.0F;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && !VISUAL_STATES.isEmpty()) {
            for (Player player : minecraft.level.players()) {
                LightFlightVisualState visualState = VISUAL_STATES.get(player.getUUID());
                if (visualState != null && visualState.isFlying()) {
                    enforceFlightPose(player);
                    if (visualState.style() == LightFlightStyle.LIGHT) {
                        visualState.recordTrailPosition(
                                player.position().add(0.0D, player.getBbHeight() * 0.52D, 0.0D)
                        );
                    }
                }
            }
        }

        LocalPlayer player = minecraft.player;
        if (player != null
                && isFlying(player.getUUID())
                && player.getAbilities().mayfly
                && !player.getAbilities().flying
                && !ClientSoulRoamingManager.isLocalPlayerSoulRoaming()) {
            player.getAbilities().flying = true;
        }
    }

    public static boolean handleMovementInput(Input input) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null
                || minecraft.screen != null
                || !isFlying(player.getUUID())
                || getStyle(player.getUUID()) != LightFlightStyle.LIGHT) {
            return false;
        }
        float forwardImpulse = input.forwardImpulse;
        boolean moving = Math.abs(forwardImpulse) > 1.0E-4F;
        LightFlightVisualState visualState = VISUAL_STATES.get(player.getUUID());
        if (visualState != null && visualState.isMoving() != moving) {
            visualState.setMoving(moving);
            if (!moving) {
                triggerStopImpact(player, visualState);
            }
        }
        input.leftImpulse = 0.0F;
        input.forwardImpulse = forwardImpulse;
        if (input.shiftKeyDown) {
            input.forwardImpulse *= 0.35F;
            input.shiftKeyDown = false;
        }
        if (!moving) {
            player.setDeltaMovement(Vec3.ZERO);
        }
        return true;
    }

    public static boolean applyFlightMovement(LocalPlayer player) {
        if (!isFlying(player.getUUID())
                || getStyle(player.getUUID()) != LightFlightStyle.LIGHT
                || Minecraft.getInstance().screen != null) {
            return false;
        }
        Input input = player.input;
        if (Math.abs(input.forwardImpulse) <= 1.0E-4F) {
            LightFlightVisualState visualState = VISUAL_STATES.get(player.getUUID());
            if (visualState != null && visualState.isMoving()) {
                visualState.setMoving(false);
                triggerStopImpact(player, visualState);
            }
            player.setDeltaMovement(Vec3.ZERO);
            return true;
        }
        LightFlightVisualState visualState = VISUAL_STATES.get(player.getUUID());
        if (visualState != null) {
            visualState.setMoving(true);
        }
        Vec3 direction = player.getLookAngle().normalize();
        double speed = player.getAbilities().getFlyingSpeed() * 3.0D;
        if (input.shiftKeyDown) {
            speed *= 0.35D;
        }
        player.setDeltaMovement(direction.scale(speed * input.forwardImpulse));
        player.move(MoverType.SELF, player.getDeltaMovement());
        player.setDeltaMovement(Vec3.ZERO);
        return true;
    }

    public static void onJumpPressed() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null
                || minecraft.screen != null
                || ClientMeditationManager.isLocalPlayerMeditating()
                || ClientSoulRoamingManager.isLocalPlayerSoulRoaming()
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
                || ClientSoulRoamingManager.isLocalPlayerSoulRoaming()
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
                if (visualState.isFlying() && sanitizedStyle == LightFlightStyle.LIGHT) {
                    Minecraft minecraft = Minecraft.getInstance();
                    Player player = minecraft.level == null ? null : minecraft.level.getPlayerByUUID(playerId);
                    if (player != null) {
                        triggerStopImpact(player, visualState);
                    }
                }
                visualState.setMoving(false);
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
        if (ClientSoulRoamingManager.isLocalPlayerSoulRoaming()) {
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

    public static float getStopImpactProgress(float partialTick) {
        return Mth.lerp(partialTick, previousStopImpactProgress, stopImpactProgress);
    }

    public static float getStopImpactTime(float partialTick) {
        return Mth.lerp(partialTick, previousStopImpactTime, stopImpactTime);
    }

    public static Vec3 getStopImpactPosition(float partialTick) {
        return previousStopImpactPosition.lerp(stopImpactPosition, partialTick);
    }

    public static int getStopImpactColor() {
        return stopImpactColor;
    }

    public static boolean isStopImpactActive() {
        return stopImpactProgress < 1.0F;
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
        previousStopImpactProgress = 1.0F;
        stopImpactProgress = 1.0F;
        previousStopImpactTime = 1.0F;
        stopImpactTime = 1.0F;
        previousStopImpactPosition = Vec3.ZERO;
        stopImpactPosition = Vec3.ZERO;
        stopImpactColor = DEFAULT_LIGHT_COLOR;
    }

    private static void triggerStopImpact(Player player, LightFlightVisualState visualState) {
        previousStopImpactProgress = 0.0F;
        stopImpactProgress = 0.0F;
        previousStopImpactTime = stopImpactTime;
        stopImpactTime += 1.0F;
        Vec3 impactPosition = player.position().add(0.0D, player.getBbHeight() * 0.52D, 0.0D);
        previousStopImpactPosition = impactPosition;
        stopImpactPosition = impactPosition;
        stopImpactColor = visualState.color();
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
        return ClientCultivationInfoManager.getCultivationStage()
                .flatMap(stage -> LightFlightStyle.forRealm(stage.realm()))
                .orElse(LightFlightStyle.AIR_WALKING);
    }

    private static void enforceFlightPose(Player player) {
        player.setPose(Pose.STANDING);
        if (getStyle(player.getUUID()) == LightFlightStyle.LIGHT) {
            player.yBodyRot = player.getYRot();
            player.yBodyRotO = player.yRotO;
            player.yHeadRot = player.getYRot();
            player.yHeadRotO = player.yRotO;
        }
        player.walkAnimation.update(0.0F, 1.0F);
        player.walkAnimation.update(0.0F, 1.0F);
    }

    private static boolean isShiftDown(Minecraft minecraft) {
        long window = minecraft.getWindow().getWindow();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }
}
