package com.seraphina.ninecun_lingtai.client.player;

import com.seraphina.ninecun_lingtai.api.element.FiveElementValues;
import com.seraphina.ninecun_lingtai.client.animation.MeditationAnimationManager;
import com.seraphina.ninecun_lingtai.client.input.ModKeyMappings;
import com.seraphina.ninecun_lingtai.client.screen.CultivationScreen;
import com.seraphina.ninecun_lingtai.client.tribulation.ClientTribulationManager;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.network.packet.SetMeditationStatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ClientMeditationManager {
    private static final Map<UUID, MeditationVisualState> MEDITATION_VISUAL_STATES = new HashMap<>();

    private ClientMeditationManager() {
    }

    public static void tick() {
        MEDITATION_VISUAL_STATES.values().forEach(MeditationVisualState::tick);
        MEDITATION_VISUAL_STATES.entrySet().removeIf(entry -> entry.getValue().hasFinishedExiting());

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        while (ModKeyMappings.MEDITATION.consumeClick()) {
            if (player != null
                    && minecraft.screen == null
                    && !ClientTribulationManager.isLocalPlayerInTribulation()
                    && !ClientSoulRoamingManager.isLocalPlayerSoulRoaming()) {
                minecraft.setScreen(new CultivationScreen());
            }
        }
        if (player == null || !isMeditating(player.getUUID())) {
            return;
        }
        player.stopUsingItem();
        player.setDeltaMovement(Vec3.ZERO);
        ClientMeditationCameraManager.lockPlayerRotation(player);
    }

    public static void startTechniqueCultivation(ResourceLocation techniqueId) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && !isMeditating(player.getUUID())) {
            requestState(player, true, techniqueId, false);
        }
    }

    public static void startRealmCultivation(ResourceLocation techniqueId) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && !isMeditating(player.getUUID())) {
            requestState(player, true, techniqueId, true);
        }
    }

    public static void requestStop() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && isMeditating(player.getUUID())) {
            requestState(player, false, null, false);
        }
    }

    public static void setMeditating(
            UUID playerId,
            boolean meditating,
            FiveElementValues environmentValues,
            FiveElementValues movementAffinities,
            int realmLevel
    ) {
        MeditationVisualState visualState = MEDITATION_VISUAL_STATES.get(playerId);
        boolean wasMeditating = visualState != null && visualState.isMeditating();
        if (meditating) {
            if (visualState == null) {
                MEDITATION_VISUAL_STATES.put(
                        playerId,
                        new MeditationVisualState(environmentValues, movementAffinities, realmLevel)
                );
            } else {
                visualState.update(environmentValues, movementAffinities, realmLevel);
            }
        } else if (visualState != null) {
            visualState.stopMeditating();
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.getUUID().equals(playerId)) {
            if (meditating && !wasMeditating) {
                ClientMeditationCameraManager.start(player);
            } else if (!meditating && wasMeditating) {
                ClientMeditationCameraManager.stop(player);
            }
        }
        MeditationAnimationManager.setMeditating(playerId, meditating);
    }

    public static boolean isMeditating(UUID playerId) {
        MeditationVisualState visualState = MEDITATION_VISUAL_STATES.get(playerId);
        return visualState != null && visualState.isMeditating();
    }

    public static Map<UUID, MeditationVisualState> getVisualStates() {
        return Collections.unmodifiableMap(MEDITATION_VISUAL_STATES);
    }

    public static boolean isLocalPlayerMeditating() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && isMeditating(player.getUUID());
    }

    public static void removePlayer(UUID playerId) {
        setMeditating(playerId, false, FiveElementValues.EMPTY, FiveElementValues.EMPTY, 0);
    }

    public static void reset() {
        ClientMeditationCameraManager.reset();
        MEDITATION_VISUAL_STATES.keySet().forEach(
                playerId -> MeditationAnimationManager.setMeditating(playerId, false)
        );
        MEDITATION_VISUAL_STATES.clear();
    }

    private static void requestState(
            LocalPlayer player,
            boolean meditating,
            @Nullable ResourceLocation techniqueId,
            boolean cultivatingRealm
    ) {
        // Local prediction only; the realm level is corrected by the server sync that follows.
        setMeditating(
                player.getUUID(),
                meditating,
                meditating ? FiveElementValues.NEUTRAL : FiveElementValues.EMPTY,
                FiveElementValues.EMPTY,
                0
        );
        ModNetwork.sendToServer(new SetMeditationStatePacket(
                meditating,
                Optional.ofNullable(techniqueId),
                cultivatingRealm
        ));
    }
}
