package com.seraphina.ninecun_lingtai.client.player;

import com.seraphina.ninecun_lingtai.api.element.FiveElementValues;
import com.seraphina.ninecun_lingtai.client.animation.MeditationAnimationManager;
import com.seraphina.ninecun_lingtai.client.input.ModKeyMappings;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.network.packet.SetMeditationStatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClientMeditationManager {
    private static final Map<UUID, MeditationVisualState> MEDITATING_PLAYERS = new HashMap<>();

    private ClientMeditationManager() {
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        while (ModKeyMappings.MEDITATION.consumeClick()) {
            if (player != null && minecraft.screen == null && !isMeditating(player.getUUID())) {
                requestState(player, true);
            }
        }
        if (player == null || !isMeditating(player.getUUID())) {
            return;
        }
        player.stopUsingItem();
        player.setDeltaMovement(Vec3.ZERO);
        ClientMeditationCameraManager.lockPlayerRotation(player);
    }

    public static void requestStop() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && isMeditating(player.getUUID())) {
            requestState(player, false);
        }
    }

    public static void setMeditating(
            UUID playerId,
            boolean meditating,
            FiveElementValues environmentValues,
            FiveElementValues absorptionEfficiencies
    ) {
        boolean wasMeditating = isMeditating(playerId);
        if (meditating) {
            MEDITATING_PLAYERS.put(
                    playerId,
                    new MeditationVisualState(environmentValues, absorptionEfficiencies)
            );
        } else {
            MEDITATING_PLAYERS.remove(playerId);
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
        return MEDITATING_PLAYERS.containsKey(playerId);
    }

    public static Map<UUID, MeditationVisualState> getVisualStates() {
        return Collections.unmodifiableMap(MEDITATING_PLAYERS);
    }

    public static boolean isLocalPlayerMeditating() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && isMeditating(player.getUUID());
    }

    public static void removePlayer(UUID playerId) {
        setMeditating(playerId, false, FiveElementValues.EMPTY, FiveElementValues.EMPTY);
    }

    public static void reset() {
        ClientMeditationCameraManager.reset();
        Map.copyOf(MEDITATING_PLAYERS).keySet().forEach(playerId -> setMeditating(
                playerId,
                false,
                FiveElementValues.EMPTY,
                FiveElementValues.EMPTY
        ));
        MEDITATING_PLAYERS.clear();
    }

    private static void requestState(LocalPlayer player, boolean meditating) {
        setMeditating(
                player.getUUID(),
                meditating,
                meditating ? FiveElementValues.NEUTRAL : FiveElementValues.EMPTY,
                FiveElementValues.EMPTY
        );
        ModNetwork.sendToServer(new SetMeditationStatePacket(meditating));
    }
}
