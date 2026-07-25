package com.seraphina.ninecun_lingtai.common.player;

import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class PlayerMeditationManager {
    private static final Set<UUID> MEDITATING_PLAYERS = new HashSet<>();

    private PlayerMeditationManager() {
    }

    public static boolean startMeditating(ServerPlayer player) {
        if (isMeditating(player)) {
            ModNetwork.sendMeditationStateToTrackingAndSelf(player, true);
            return true;
        }
        if (!canStartMeditating(player)) {
            ModNetwork.sendMeditationState(player, player.getUUID(), false);
            return false;
        }
        MEDITATING_PLAYERS.add(player.getUUID());
        preparePlayer(player);
        ModNetwork.sendMeditationStateToTrackingAndSelf(player, true);
        return true;
    }

    public static void stopMeditating(ServerPlayer player) {
        MEDITATING_PLAYERS.remove(player.getUUID());
        ModNetwork.sendMeditationStateToTrackingAndSelf(player, false);
    }

    public static boolean isMeditating(ServerPlayer player) {
        return MEDITATING_PLAYERS.contains(player.getUUID());
    }

    public static void restrictMovement(ServerPlayer player) {
        if (!isMeditating(player)) {
            return;
        }
        if (!canContinueMeditating(player)) {
            stopMeditating(player);
            return;
        }
        preparePlayer(player);
    }

    public static void syncState(ServerPlayer recipient, ServerPlayer trackedPlayer) {
        ModNetwork.sendMeditationState(recipient, trackedPlayer.getUUID(), isMeditating(trackedPlayer));
    }

    public static void clear() {
        MEDITATING_PLAYERS.clear();
    }

    private static boolean canStartMeditating(ServerPlayer player) {
        return player.isAlive()
                && !player.isSpectator()
                && !player.isPassenger()
                && !player.isSleeping()
                && player.onGround();
    }

    private static boolean canContinueMeditating(ServerPlayer player) {
        return player.isAlive()
                && !player.isSpectator()
                && !player.isPassenger()
                && !player.isSleeping();
    }

    private static void preparePlayer(ServerPlayer player) {
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.setSprinting(false);
        player.setShiftKeyDown(false);
        player.stopUsingItem();
    }
}
