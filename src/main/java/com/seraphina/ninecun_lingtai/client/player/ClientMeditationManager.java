package com.seraphina.ninecun_lingtai.client.player;

import com.seraphina.ninecun_lingtai.client.animation.MeditationAnimationManager;
import com.seraphina.ninecun_lingtai.client.input.ModKeyMappings;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.network.packet.SetMeditationStatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ClientMeditationManager {
    private static final Set<UUID> MEDITATING_PLAYERS = new HashSet<>();

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
        player.setDeltaMovement(Vec3.ZERO);
        player.setSprinting(false);
        player.stopUsingItem();
        minecraft.options.keyAttack.setDown(false);
        minecraft.options.keyUse.setDown(false);
        minecraft.options.keyPickItem.setDown(false);
        if (minecraft.gameMode != null) {
            minecraft.gameMode.stopDestroyBlock();
        }
    }

    public static void requestStop() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && isMeditating(player.getUUID())) {
            requestState(player, false);
        }
    }

    public static void setMeditating(UUID playerId, boolean meditating) {
        if (meditating) {
            MEDITATING_PLAYERS.add(playerId);
        } else {
            MEDITATING_PLAYERS.remove(playerId);
        }
        MeditationAnimationManager.setMeditating(playerId, meditating);
    }

    public static boolean isMeditating(UUID playerId) {
        return MEDITATING_PLAYERS.contains(playerId);
    }

    public static boolean isLocalPlayerMeditating() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && isMeditating(player.getUUID());
    }

    public static void removePlayer(UUID playerId) {
        setMeditating(playerId, false);
    }

    public static void reset() {
        Set.copyOf(MEDITATING_PLAYERS).forEach(playerId -> setMeditating(playerId, false));
        MEDITATING_PLAYERS.clear();
    }

    private static void requestState(LocalPlayer player, boolean meditating) {
        setMeditating(player.getUUID(), meditating);
        ModNetwork.sendToServer(new SetMeditationStatePacket(meditating));
    }
}
