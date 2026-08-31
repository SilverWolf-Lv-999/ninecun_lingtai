package net.seraphina.ninecun_lingtai.common.insight;

import net.seraphina.ninecun_lingtai.ModMain;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 玩家已持有的悟道印记 + 待选候选（三选一）NBT 存取。
 * NBT key {@code ninecun_lingtai:insights}；{@code copy()} 需接入克隆链
 * （PlayerAptitudeEventHandler.onPlayerClone，见"需集中接线"）。
 */
public final class PlayerInsightData {
    private static final String ROOT_KEY = ModMain.MOD_ID + ":insights";
    private static final String OWNED_KEY = "Owned";
    private static final String ID_KEY = "Id";
    private static final String TIER_KEY = "Tier";
    private static final String ACQUIRED_GAME_TIME_KEY = "AcquiredGameTime";
    private static final String PENDING_KEY = "Pending";
    private static final String CANDIDATES_KEY = "Candidates";
    private static final String EXPIRE_GAME_TIME_KEY = "ExpireGameTime";

    private PlayerInsightData() {
    }

    public static List<ResourceLocation> getInsightIds(Player player) {
        CompoundTag rootTag = getRootTag(player);
        if (rootTag == null) {
            return List.of();
        }
        ListTag ownedTags = rootTag.getList(OWNED_KEY, Tag.TAG_COMPOUND);
        List<ResourceLocation> insightIds = new ArrayList<>(ownedTags.size());
        for (Tag ownedTag : ownedTags) {
            if (ownedTag instanceof CompoundTag insightTag) {
                ResourceLocation insightId = ResourceLocation.tryParse(insightTag.getString(ID_KEY));
                if (insightId != null) {
                    insightIds.add(insightId);
                }
            }
        }
        return insightIds;
    }

    public static boolean hasInsight(Player player, ResourceLocation insightId) {
        return getInsightIds(player).contains(insightId);
    }

    public static void addInsight(
            Player player,
            ResourceLocation insightId,
            InsightDefinition.Tier tier,
            long gameTime
    ) {
        if (hasInsight(player, insightId)) {
            return;
        }
        CompoundTag insightTag = new CompoundTag();
        insightTag.putString(ID_KEY, insightId.toString());
        insightTag.putString(TIER_KEY, tier.name());
        insightTag.putLong(ACQUIRED_GAME_TIME_KEY, gameTime);
        CompoundTag rootTag = getOrCreateRootTag(player);
        if (!rootTag.contains(OWNED_KEY, Tag.TAG_LIST)) {
            rootTag.put(OWNED_KEY, new ListTag());
        }
        rootTag.getList(OWNED_KEY, Tag.TAG_COMPOUND).add(insightTag);
    }

    /** 写入三选一候选（覆盖旧候选——落子无悔的温和版：旧的未选即作废）。 */
    public static void setPendingCandidates(
            Player player,
            List<ResourceLocation> candidateIds,
            long expireGameTime
    ) {
        CompoundTag pendingTag = new CompoundTag();
        ListTag candidateTags = new ListTag();
        for (ResourceLocation candidateId : candidateIds) {
            candidateTags.add(StringTag.valueOf(candidateId.toString()));
        }
        pendingTag.put(CANDIDATES_KEY, candidateTags);
        pendingTag.putLong(EXPIRE_GAME_TIME_KEY, expireGameTime);
        getOrCreateRootTag(player).put(PENDING_KEY, pendingTag);
    }

    /** 未过期的待选候选；已过期时顺手清除并返回空表。 */
    public static List<ResourceLocation> getPendingCandidates(Player player, long gameTime) {
        CompoundTag rootTag = getRootTag(player);
        if (rootTag == null || !rootTag.contains(PENDING_KEY, Tag.TAG_COMPOUND)) {
            return List.of();
        }
        CompoundTag pendingTag = rootTag.getCompound(PENDING_KEY);
        if (pendingTag.getLong(EXPIRE_GAME_TIME_KEY) < gameTime) {
            rootTag.remove(PENDING_KEY);
            notifyExpired(player);
            return List.of();
        }
        ListTag candidateTags = pendingTag.getList(CANDIDATES_KEY, Tag.TAG_STRING);
        List<ResourceLocation> candidateIds = new ArrayList<>(candidateTags.size());
        for (int index = 0; index < candidateTags.size(); index++) {
            ResourceLocation candidateId = ResourceLocation.tryParse(candidateTags.getString(index));
            if (candidateId != null) {
                candidateIds.add(candidateId);
            }
        }
        return candidateIds;
    }

    public static long getPendingExpireGameTime(Player player) {
        CompoundTag rootTag = getRootTag(player);
        return rootTag == null || !rootTag.contains(PENDING_KEY, Tag.TAG_COMPOUND)
                ? 0L
                : rootTag.getCompound(PENDING_KEY).getLong(EXPIRE_GAME_TIME_KEY);
    }

    public static void clearPendingCandidates(Player player) {
        CompoundTag rootTag = getRootTag(player);
        if (rootTag != null) {
            rootTag.remove(PENDING_KEY);
        }
    }

    /**
     * 周期驱动的过期检查（BreakthroughRitualEventHandler 低频调用）：
     * 候选过期即清除并向玩家发一条 insight_expired 提示。
     */
    public static void tickExpiry(ServerPlayer player, long gameTime) {
        CompoundTag rootTag = getRootTag(player);
        if (rootTag == null || !rootTag.contains(PENDING_KEY, Tag.TAG_COMPOUND)) {
            return;
        }
        if (rootTag.getCompound(PENDING_KEY).getLong(EXPIRE_GAME_TIME_KEY) < gameTime) {
            rootTag.remove(PENDING_KEY);
            notifyExpired(player);
        }
    }

    /** 候选过期提醒：只对在线服务端玩家发一次（客户端读取路径静默）。 */
    private static void notifyExpired(Player player) {
        if (player instanceof ServerPlayer serverPlayer && !serverPlayer.hasDisconnected()) {
            serverPlayer.displayClientMessage(
                    Component.translatableWithFallback(
                            "message.ninecun_lingtai.insight_expired",
                            "一点明悟未曾把握，已然消散"),
                    false
            );
        }
    }

    /** 克隆链复制（需集中接线到 PlayerAptitudeEventHandler.onPlayerClone）。 */
    public static void copy(Player sourcePlayer, Player targetPlayer) {
        CompoundTag sourceData = sourcePlayer.getPersistentData();
        if (sourceData.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            targetPlayer.getPersistentData().put(ROOT_KEY, sourceData.getCompound(ROOT_KEY).copy());
        }
    }

    private static CompoundTag getRootTag(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        return persistentData.contains(ROOT_KEY, Tag.TAG_COMPOUND)
                ? persistentData.getCompound(ROOT_KEY)
                : null;
    }

    private static CompoundTag getOrCreateRootTag(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            persistentData.put(ROOT_KEY, new CompoundTag());
        }
        return persistentData.getCompound(ROOT_KEY);
    }
}
