package net.seraphina.ninecun_lingtai.common.causality;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.utility.EntityUtility;
import net.seraphina.ninecun_lingtai.utility.GreatCausalityUtility;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class GreatCausalityQueryService {
    private static final double MAXIMUM_QUERY_DISTANCE_SQUARED = 64.0D * 64.0D;
    private static final Map<UUID, PendingQuery> PENDING_QUERIES = new HashMap<>();

    private GreatCausalityQueryService() {
    }

    public static void query(ServerPlayer observer, UUID targetId) {
        if (!GreatCausalityUtility.canQuery(observer)) {
            observer.displayClientMessage(Component.literal("尚未参悟《大因果术》，无法推衍因果"), true);
            return;
        }
        if (targetId == null) {
            sendPlayerList(observer);
            return;
        }

        ServerPlayer target = observer.server.getPlayerList().getPlayer(targetId);
        if (!isValidTarget(observer, target)) {
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
        List<CausalityRecord> records = CausalityHistoryManager.getRecords(
                observer.server,
                target.getUUID(),
                GreatCausalityUtility.getMaximumVisibleRecords(observer)
        );
        boolean fullHistory = GreatCausalityUtility.isPerfection(observer);
        ModNetwork.sendGreatCausalitySnapshot(observer, new CausalitySnapshot(
                false,
                List.of(),
                targetSnapshot,
                records,
                fullHistory,
                ""
        ));
    }

    public static void setPendingTarget(ServerPlayer player, UUID targetId) {
        PENDING_QUERIES.put(player.getUUID(), new PendingQuery(targetId));
    }

    public static boolean hasPendingQuery(ServerPlayer player) {
        return PENDING_QUERIES.containsKey(player.getUUID());
    }

    public static UUID consumePendingTarget(ServerPlayer player) {
        PendingQuery pendingQuery = PENDING_QUERIES.remove(player.getUUID());
        return pendingQuery == null ? null : pendingQuery.targetId();
    }

    public static void clearPendingTarget(ServerPlayer player) {
        PENDING_QUERIES.remove(player.getUUID());
    }

    public static void sendPlayerList(ServerPlayer observer) {
        List<CausalityPlayerSnapshot> players = observer.server.getPlayerList().getPlayers().stream()
                .filter(player -> player.level() == observer.level())
                .map(GreatCausalityQueryService::createPlayerSnapshot)
                .sorted(Comparator.comparing(CausalityPlayerSnapshot::playerName))
                .toList();
        ModNetwork.sendGreatCausalitySnapshot(observer, new CausalitySnapshot(
                true,
                players,
                null,
                List.of(),
                false,
                ""
        ));
    }

    private static boolean isValidTarget(ServerPlayer observer, ServerPlayer target) {
        return target != null
                && target.level() == observer.level()
                && target != observer
                && observer.distanceToSqr(target) <= MAXIMUM_QUERY_DISTANCE_SQUARED
                && observer.hasLineOfSight(target);
    }

    private static CausalityPlayerSnapshot createPlayerSnapshot(ServerPlayer player) {
        String cultivation = player.getName().getString();
        cultivation = net.seraphina.ninecun_lingtai.api.cultivation.CultivationApi.getStage(player)
                .map(stage -> stage.realm().getChineseName() + " " + stage.layer() + "层")
                .orElse("凡人");
        return new CausalityPlayerSnapshot(
                player.getUUID(),
                player.getGameProfile().getName(),
                player.level().dimension().location().toString(),
                cultivation
        );
    }

    private record PendingQuery(UUID targetId) {
    }
}
