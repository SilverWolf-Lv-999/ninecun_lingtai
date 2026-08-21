package net.seraphina.ninecun_lingtai.common.causality;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.utility.DimensionNameUtility;
import net.seraphina.ninecun_lingtai.utility.EntityUtility;
import net.seraphina.ninecun_lingtai.utility.GreatCausalityUtility;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class GreatCausalityQueryService {
    private static final double MAXIMUM_QUERY_DISTANCE_SQUARED = 64.0D * 64.0D;
    private static final long PLAYER_LIST_SESSION_DURATION = 20L * 15L;
    private static final Map<UUID, PlayerListSession> PLAYER_LIST_SESSIONS = new HashMap<>();

    private GreatCausalityQueryService() {
    }

    public static void query(ServerPlayer observer, UUID targetId) {
        PLAYER_LIST_SESSIONS.remove(observer.getUUID());
        if (!GreatCausalityUtility.canQuery(observer)) {
            observer.displayClientMessage(Component.literal("尚未参悟《大因果术》，无法推衍因果"), true);
            return;
        }
        if (targetId == null) {
            sendPlayerList(observer);
            return;
        }
        queryTarget(observer, targetId, true);
    }

    public static void querySelectedPlayer(ServerPlayer observer, UUID targetId) {
        if (targetId == null) {
            observer.displayClientMessage(Component.literal("未锁定可推衍的因果对象"), true);
            return;
        }
        PlayerListSession session = PLAYER_LIST_SESSIONS.remove(observer.getUUID());
        if (session == null
                || observer.level().getGameTime() > session.expiresAt()
                || !observer.level().dimension().location().equals(session.dimensionId())
                || !session.targetIds().contains(targetId)) {
            observer.displayClientMessage(Component.literal("因果观测已失效，请重新施展《大因果术》"), true);
            return;
        }
        queryTarget(observer, targetId, false);
    }

    private static void queryTarget(
            ServerPlayer observer,
            UUID targetId,
            boolean requireDirectSight
    ) {
        if (!GreatCausalityUtility.canQuery(observer)) {
            observer.displayClientMessage(Component.literal("尚未参悟《大因果术》，无法推衍因果"), true);
            return;
        }
        ServerPlayer target = observer.server.getPlayerList().getPlayer(targetId);
        if (!isValidTarget(observer, target, requireDirectSight)) {
            observer.displayClientMessage(Component.literal("因果线超出当前感知范围"), true);
            return;
        }
        if (!GreatCausalityUtility.isImmortal(observer)
                && GreatCausalityUtility.isImmortal(target)) {
            EntityUtility.killedEntity(observer, target);
            observer.displayClientMessage(Component.literal("窥探仙人因果，已承受因果反噬"), true);
            return;
        }

        CausalityPlayerSnapshot targetSnapshot = createPlayerSnapshot(target);
        net.seraphina.ninecun_lingtai.common.player.GreatCausalitySettings settings =
                net.seraphina.ninecun_lingtai.common.player.PlayerGreatCausalityData.getSettings(observer);
        List<CausalityRecord> records = CausalityHistoryManager.getRecords(
                observer.server,
                target.getUUID(),
                GreatCausalityUtility.getMaximumVisibleRecords(observer)
        );
        boolean fullHistory = GreatCausalityUtility.isPerfection(observer);
        ModNetwork.sendGreatCausalitySnapshot(observer, new CausalitySnapshot(
                false,
                List.of(),
                createSettingsTargets(observer),
                targetSnapshot,
                records,
                fullHistory,
                "",
                settings,
                GreatCausalityUtility.isPerfection(observer)
        ));
    }

    public static void sendPlayerList(ServerPlayer observer) {
        removeExpiredPlayerListSessions(observer);
        List<CausalityPlayerSnapshot> players = observer.server.getPlayerList().getPlayers().stream()
                .filter(player -> player != observer && player.level() == observer.level())
                .map(GreatCausalityQueryService::createPlayerSnapshot)
                .sorted(Comparator.comparing(CausalityPlayerSnapshot::playerName))
                .toList();
        List<CausalityPlayerSnapshot> settingsTargets = createSettingsTargets(observer);
        PLAYER_LIST_SESSIONS.put(observer.getUUID(), new PlayerListSession(
                observer.level().dimension().location(),
                observer.level().getGameTime() + PLAYER_LIST_SESSION_DURATION,
                players.stream()
                        .map(CausalityPlayerSnapshot::playerId)
                        .collect(java.util.stream.Collectors.toUnmodifiableSet())
        ));
        ModNetwork.sendGreatCausalitySnapshot(observer, new CausalitySnapshot(
                true,
                players,
                settingsTargets,
                null,
                List.of(),
                false,
                "",
                net.seraphina.ninecun_lingtai.common.player.PlayerGreatCausalityData.getSettings(observer),
                GreatCausalityUtility.isPerfection(observer)
        ));
    }

    private static void removeExpiredPlayerListSessions(ServerPlayer observer) {
        long gameTime = observer.level().getGameTime();
        PLAYER_LIST_SESSIONS.entrySet().removeIf(entry -> entry.getValue().expiresAt() < gameTime);
    }

    private static boolean isValidTarget(
            ServerPlayer observer,
            ServerPlayer target,
            boolean requireDirectSight
    ) {
        return target != null
                && target.level() == observer.level()
                && target != observer
                && (!requireDirectSight
                || observer.distanceToSqr(target) <= MAXIMUM_QUERY_DISTANCE_SQUARED
                && observer.hasLineOfSight(target));
    }

    private static CausalityPlayerSnapshot createPlayerSnapshot(ServerPlayer player) {
        String cultivation = player.getName().getString();
        cultivation = net.seraphina.ninecun_lingtai.api.cultivation.CultivationApi.getStage(player)
                .map(stage -> stage.realm().getChineseName() + " " + stage.layer() + "层")
                .orElse("凡人");
        return new CausalityPlayerSnapshot(
                player.getUUID(),
                player.getGameProfile().getName(),
                DimensionNameUtility.getDisplayName(
                        player.server,
                        player.level().dimension().location()
                ),
                cultivation
        );
    }

    private static List<CausalityPlayerSnapshot> createSettingsTargets(ServerPlayer observer) {
        return GreatCausalityUtility.getEligibleTargets(observer).stream()
                .map(GreatCausalityQueryService::createPlayerSnapshot)
                .sorted(Comparator.comparing(CausalityPlayerSnapshot::playerName))
                .toList();
    }

    private record PlayerListSession(
            ResourceLocation dimensionId,
            long expiresAt,
            Set<UUID> targetIds
    ) {
    }

}
