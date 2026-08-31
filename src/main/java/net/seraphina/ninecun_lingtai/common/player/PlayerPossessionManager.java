package net.seraphina.ninecun_lingtai.common.player;

import com.mojang.authlib.GameProfile;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.behavior.PlayerBehaviorLedgerData;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 夺舍会话管理。{@link PossessionSavedData} 是权威数据源（跨重启存续）；
 * 本类的静态 map 仅是在线运行期缓存，二者在会话开始/结束时同步写入。
 *
 * <p>生命周期语义：
 * <ul>
 *   <li>夺舍者主动登出 = 解除夺舍，目标归位（沿用旧语义）；</li>
 *   <li>目标主动登出 = 会话存续，目标重登仍为旁观（沿用旧语义）；</li>
 *   <li>干净关服 = 挂起，重启后夺舍者登录即恢复会话；</li>
 *   <li>重启后目标先登录而夺舍者缺席 = 安全解除，目标当场归位。</li>
 * </ul>
 */
public final class PlayerPossessionManager {
    private static final String TARGET_STATE_KEY = ModMain.MOD_ID + ":possessed_target";
    private static final String POSSESSOR_UUID_KEY = "possessor";
    private static final String ORIGINAL_GAME_TYPE_KEY = "original_game_type";
    private static final Map<UUID, PossessionSession> POSSESSION_SESSIONS = new HashMap<>();
    private static final Map<UUID, UUID> POSSESSED_TARGETS = new HashMap<>();

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
        PossessionSavedData.get(possessor.server).putRecord(new PossessionSavedData.PossessionRecord(
                possessorUuid,
                targetUuid,
                targetProfile.getId() != null ? targetProfile.getId() : targetUuid,
                targetProfile.getName() != null ? targetProfile.getName() : "",
                targetGameType
        ));
        // 行为账本采样（心魔即己 §5.2）：夺舍成功各记一笔。
        PlayerBehaviorLedgerData.recordPossess(possessor);
        PlayerBehaviorLedgerData.recordPossessedBy(target);

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
        PossessionSavedData savedData = PossessionSavedData.get(player.server);

        // 场景 1：登录者是被夺舍的目标。夺舍者在场（会话已在内存中，含重启后已恢复
        // 的挂起会话）则维持旁观；夺舍者缺席（重启后尚未归来）则安全解除、当场归位，
        // 绝不让目标无限期卡在旁观模式等一个可能永远不回来的夺舍者。
        PossessionSavedData.PossessionRecord targetRecord = savedData.findRecordByTarget(player.getUUID());
        if (targetRecord != null) {
            if (POSSESSION_SESSIONS.containsKey(targetRecord.possessorUuid())) {
                POSSESSED_TARGETS.put(player.getUUID(), targetRecord.possessorUuid());
                player.setGameMode(GameType.SPECTATOR);
            } else {
                savedData.removeRecord(targetRecord.possessorUuid());
                restoreTargetGameMode(player, targetRecord.targetGameType());
                ModMain.LOGGER.info(
                        "Dissolved suspended possession of target {} because possessor {} is absent after restart",
                        player.getGameProfile().getName(),
                        targetRecord.possessorUuid()
                );
            }
            return;
        }

        // 场景 2：登录者是夺舍者且有挂起会话，恢复进内存并广播。目标是否在线不影响
        // 恢复（与运行期"目标可离线"的语义一致）。
        PossessionSavedData.PossessionRecord possessorRecord = savedData.getRecord(player.getUUID());
        if (possessorRecord != null && !POSSESSION_SESSIONS.containsKey(player.getUUID())) {
            resumeSuspendedSession(player, possessorRecord);
            return;
        }

        // 场景 3：兜底——孤儿旁观标记（会话已不存在），恢复原游戏模式。
        restoreMarkedTarget(player);
    }

    private static void resumeSuspendedSession(
            ServerPlayer possessor,
            PossessionSavedData.PossessionRecord record
    ) {
        // name 允许为空串但不能为 null：writeGameProfile → writeUtf(name) 遇 null 会 NPE。
        GameProfile targetProfile = new GameProfile(
                record.targetProfileId(),
                record.targetProfileName()
        );
        POSSESSION_SESSIONS.put(possessor.getUUID(), new PossessionSession(
                record.targetUuid(),
                targetProfile,
                record.targetGameType()
        ));
        POSSESSED_TARGETS.put(record.targetUuid(), possessor.getUUID());
        ServerPlayer target = possessor.server.getPlayerList().getPlayer(record.targetUuid());
        if (target != null && !target.isSpectator()) {
            target.setGameMode(GameType.SPECTATOR);
        }
        broadcastState(possessor.server, createSyncData(possessor));
        ModMain.LOGGER.info(
                "Resumed suspended possession session of {} (target={})",
                possessor.getGameProfile().getName(),
                record.targetUuid()
        );
    }

    /**
     * 登出统一入口：关服断连 = 挂起（会话留在 SavedData，重启恢复）；
     * 主动登出 = 解除夺舍并让目标归位（沿用旧语义）。
     */
    public static void onPlayerLoggedOut(ServerPlayer player) {
        if (player.hasDisconnected() && !player.server.isRunning()) {
            suspend(player);
            return;
        }
        cleanup(player);
    }

    private static void suspend(ServerPlayer player) {
        PossessionSession session = POSSESSION_SESSIONS.remove(player.getUUID());
        if (session == null) {
            return;
        }
        POSSESSED_TARGETS.remove(session.targetUuid(), player.getUUID());
        PossessionSavedData.get(player.server).setDirty();
        ModMain.LOGGER.info(
                "Suspended possession session of {} for server shutdown; it will resume after restart",
                player.getGameProfile().getName()
        );
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
        PossessionSavedData.get(possessor.server).removeRecord(possessor.getUUID());
        PossessionSession session = POSSESSION_SESSIONS.remove(possessor.getUUID());
        if (session == null) {
            syncState(possessor);
            return;
        }

        POSSESSED_TARGETS.remove(session.targetUuid(), possessor.getUUID());
        restoreTarget(possessor.server, session);
        broadcastState(possessor.server, PossessionSyncData.inactive(possessor.getUUID()));
    }

    /**
     * 关服后清空运行期缓存（单人模式下静态 map 会跨"退出世界再进入"存活，必须清）。
     * 只清内存不动 {@link PossessionSavedData}：挂起的会话要留待重启恢复。
     */
    public static void clear(MinecraftServer server) {
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
