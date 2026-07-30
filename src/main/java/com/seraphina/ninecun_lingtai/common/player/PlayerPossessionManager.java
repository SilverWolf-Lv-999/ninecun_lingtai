package com.seraphina.ninecun_lingtai.common.player;

import com.mojang.authlib.GameProfile;
import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerPossessionManager {
    private static final String TARGET_STATE_KEY = ModMain.MOD_ID + ":possessed_target";
    private static final String POSSESSOR_UUID_KEY = "possessor";
    private static final String ORIGINAL_GAME_TYPE_KEY = "original_game_type";
    private static final Map<UUID, PossessionSession> POSSESSION_SESSIONS = new HashMap<>();
    private static final Map<UUID, UUID> POSSESSED_TARGETS = new HashMap<>();

    private PlayerPossessionManager() {
    }

    public static boolean startPossessing(ServerPlayer possessor, ServerPlayer target) {
        if (possessor == target
                || !possessor.isAlive()
                || !target.isAlive()
                || target.isSpectator()
                || isPossessing(possessor)
                || isPossessedTarget(target)) {
            return false;
        }

        GameType targetGameType = target.gameMode.getGameModeForPlayer();
        GameProfile targetProfile = copyGameProfile(target.getGameProfile());
        CompoundTag targetData = target.saveWithoutId(new CompoundTag());
        targetData.remove(Entity.UUID_TAG);
        targetData.remove(Entity.PASSENGERS_TAG);
        targetData.remove("RootVehicle");
        targetData.remove("Dimension");
        targetData.remove("playerGameType");
        targetData.remove("previousPlayerGameType");

        UUID possessorUuid = possessor.getUUID();
        UUID targetUuid = target.getUUID();
        markTargetAsPossessed(target, possessorUuid, targetGameType);

        possessor.closeContainer();
        target.closeContainer();
        possessor.stopRiding();
        possessor.ejectPassengers();
        target.stopRiding();
        target.ejectPassengers();

        possessor.load(targetData);
        possessor.setGameMode(targetGameType);
        target.setGameMode(GameType.SPECTATOR);
        target.setCamera(target);

        PossessionSession session = new PossessionSession(
                targetUuid,
                targetProfile,
                targetGameType
        );
        POSSESSION_SESSIONS.put(possessorUuid, session);
        POSSESSED_TARGETS.put(targetUuid, possessorUuid);

        refreshPlayerState(possessor);
        ModNetwork.sendCultivationInfo(possessor);
        ModNetwork.sendDefenseResource(possessor);
        ModNetwork.sendTechniqueLibrary(possessor);
        ModNetwork.sendPlayerValueControlSettings(possessor);
        broadcastState(possessor.server, createSyncData(possessor));
        return true;
    }

    public static boolean isPossessing(ServerPlayer player) {
        return isPossessing(player.getUUID());
    }

    public static boolean isPossessing(UUID playerUuid) {
        return POSSESSION_SESSIONS.containsKey(playerUuid);
    }

    public static boolean isPossessedTarget(ServerPlayer player) {
        return POSSESSED_TARGETS.containsKey(player.getUUID()) || hasPossessionMarker(player);
    }

    public static void syncState(ServerPlayer player) {
        PossessionSession session = POSSESSION_SESSIONS.get(player.getUUID());
        ModNetwork.sendPossessionState(
                player,
                session == null
                        ? PossessionSyncData.inactive(player.getUUID())
                        : createSyncData(player)
        );
    }

    public static void syncStatesTo(ServerPlayer recipient) {
        for (UUID possessorUuid : POSSESSION_SESSIONS.keySet()) {
            ServerPlayer possessor = recipient.server.getPlayerList().getPlayer(possessorUuid);
            if (possessor != null) {
                ModNetwork.sendPossessionState(recipient, createSyncData(possessor));
            }
        }
    }

    public static void onPlayerLoggedIn(ServerPlayer player) {
        PossessionSession activeSession = findSessionByTarget(player.getUUID());
        if (activeSession != null) {
            player.setGameMode(GameType.SPECTATOR);
            return;
        }
        restoreMarkedTarget(player);
    }

    public static void onPlayerClone(ServerPlayer original, ServerPlayer clone) {
        if (isPossessing(original)) {
            cleanup(original);
        }
        if (hasPossessionMarker(original)) {
            clone.getPersistentData().put(
                    TARGET_STATE_KEY,
                    original.getPersistentData().getCompound(TARGET_STATE_KEY).copy()
            );
            clone.setGameMode(GameType.SPECTATOR);
        }
    }

    public static void cleanup(ServerPlayer possessor) {
        PossessionSession session = POSSESSION_SESSIONS.remove(possessor.getUUID());
        if (session == null) {
            syncState(possessor);
            return;
        }

        POSSESSED_TARGETS.remove(session.targetUuid(), possessor.getUUID());
        restoreTarget(possessor.server, session);
        broadcastState(possessor.server, PossessionSyncData.inactive(possessor.getUUID()));
    }

    public static void clear(MinecraftServer server) {
        Map<UUID, PossessionSession> sessions = new HashMap<>(POSSESSION_SESSIONS);
        for (Map.Entry<UUID, PossessionSession> entry : sessions.entrySet()) {
            POSSESSION_SESSIONS.remove(entry.getKey());
            POSSESSED_TARGETS.remove(entry.getValue().targetUuid(), entry.getKey());
            restoreTarget(server, entry.getValue());
        }
        POSSESSION_SESSIONS.clear();
        POSSESSED_TARGETS.clear();
    }

    private static void restoreTarget(MinecraftServer server, PossessionSession session) {
        ServerPlayer target = server.getPlayerList().getPlayer(session.targetUuid());
        if (target == null) {
            return;
        }
        restoreTargetGameMode(target, session.targetGameType());
    }

    private static void restoreMarkedTarget(ServerPlayer target) {
        CompoundTag marker = getPossessionMarker(target);
        if (marker == null) {
            return;
        }
        if (marker.hasUUID(POSSESSOR_UUID_KEY)
                && POSSESSION_SESSIONS.containsKey(marker.getUUID(POSSESSOR_UUID_KEY))) {
            target.setGameMode(GameType.SPECTATOR);
            return;
        }
        restoreTargetGameMode(target, GameType.byId(marker.getInt(ORIGINAL_GAME_TYPE_KEY)));
    }

    private static void restoreTargetGameMode(ServerPlayer target, GameType gameType) {
        target.getPersistentData().remove(TARGET_STATE_KEY);
        if (target.gameMode.getGameModeForPlayer() != gameType) {
            target.setGameMode(gameType);
        }
    }

    private static void markTargetAsPossessed(
            ServerPlayer target,
            UUID possessorUuid,
            GameType originalGameType
    ) {
        CompoundTag marker = new CompoundTag();
        marker.putUUID(POSSESSOR_UUID_KEY, possessorUuid);
        marker.putInt(ORIGINAL_GAME_TYPE_KEY, originalGameType.getId());
        target.getPersistentData().put(TARGET_STATE_KEY, marker);
    }

    private static boolean hasPossessionMarker(ServerPlayer player) {
        return player.getPersistentData().contains(TARGET_STATE_KEY, CompoundTag.TAG_COMPOUND);
    }

    private static CompoundTag getPossessionMarker(ServerPlayer player) {
        return hasPossessionMarker(player)
                ? player.getPersistentData().getCompound(TARGET_STATE_KEY)
                : null;
    }

    private static PossessionSession findSessionByTarget(UUID targetUuid) {
        return POSSESSION_SESSIONS.values().stream()
                .filter(session -> session.targetUuid().equals(targetUuid))
                .findFirst()
                .orElse(null);
    }

    private static PossessionSyncData createSyncData(ServerPlayer possessor) {
        PossessionSession session = POSSESSION_SESSIONS.get(possessor.getUUID());
        return new PossessionSyncData(
                true,
                possessor.getUUID(),
                session.possessedProfile()
        );
    }

    private static void broadcastState(MinecraftServer server, PossessionSyncData syncData) {
        ModNetwork.sendPossessionStateToAll(server.getPlayerList().getPlayers(), syncData);
    }

    private static void refreshPlayerState(ServerPlayer player) {
        player.onUpdateAbilities();
        player.inventoryMenu.broadcastChanges();
        player.containerMenu.broadcastChanges();
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(
                player.getInventory().selected
        ));
    }

    private static GameProfile copyGameProfile(GameProfile sourceProfile) {
        GameProfile copiedProfile = new GameProfile(sourceProfile.getId(), sourceProfile.getName());
        copiedProfile.getProperties().putAll(sourceProfile.getProperties());
        return copiedProfile;
    }

    private record PossessionSession(
            UUID targetUuid,
            GameProfile possessedProfile,
            GameType targetGameType
    ) {
    }
}
