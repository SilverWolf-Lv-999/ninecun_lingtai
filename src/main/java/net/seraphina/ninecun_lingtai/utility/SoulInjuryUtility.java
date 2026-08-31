package net.seraphina.ninecun_lingtai.utility;

import net.seraphina.ninecun_lingtai.api.technique.TechniqueAssessment;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueComprehension;
import net.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.network.packet.SyncSoulInjuryPacket;
import net.seraphina.ninecun_lingtai.common.player.PlayerSoulInjuryData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 魂伤体系唯一业务入口（方案 v2 第 10.4 节）。
 * 铁律 R1：所有加伤/疗伤必须走 {@link #addInjury} / {@link #heal}，
 * 禁止任何系统直接调用 {@code PlayerSoulInjuryData.setInjury}。
 */
public final class SoulInjuryUtility {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    // 分档阈值/来源伤量/恢复与档位效果数值全部读 CommonConfig.soulInjury 组（运行时 get()）。
    /** 同步包触发阈值：非变档情况下累计变动 ≥ 该值才补发星图。 */
    private static final double SYNC_DELTA_THRESHOLD = 5.0D;

    /** 上次同步给客户端的魂伤值（会话级，用于 ±5 变动触发判定）。 */
    private static final Map<UUID, Double> LAST_SYNCED_INJURY = new HashMap<>();

    public enum InjuryTier {
        NONE(null),
        DAZED("message.ninecun_lingtai.soul_injury_tier_dazed"),
        SHAKEN("message.ninecun_lingtai.soul_injury_tier_shaken"),
        PERIL("message.ninecun_lingtai.soul_injury_tier_peril");

        private final String messageKey;

        InjuryTier(String messageKey) {
            this.messageKey = messageKey;
        }

        /** 分档下限（读配置；枚举构造期不能读 ForgeConfigSpec，故惰性求值）。 */
        public double getMinimumInjury() {
            return switch (this) {
                case NONE -> 0.0D;
                case DAZED -> CommonConfig.SOUL_INJURY_THRESHOLD_DAZED.get();
                case SHAKEN -> CommonConfig.SOUL_INJURY_THRESHOLD_SHAKEN.get();
                case PERIL -> CommonConfig.SOUL_INJURY_THRESHOLD_PERIL.get();
            };
        }

        public static InjuryTier fromInjury(double injury) {
            if (injury >= CommonConfig.SOUL_INJURY_THRESHOLD_PERIL.get()) {
                return PERIL;
            }
            if (injury >= CommonConfig.SOUL_INJURY_THRESHOLD_SHAKEN.get()) {
                return SHAKEN;
            }
            if (injury >= CommonConfig.SOUL_INJURY_THRESHOLD_DAZED.get()) {
                return DAZED;
            }
            return NONE;
        }
    }

    public enum InjurySource {
        SOUL_PIERCE_HIT,
        SOUL_SHOCK_HIT,
        SPELL_BACKLASH,
        SENSE_RECOIL,
        BREAKTHROUGH_FAIL,
        MIND_DEMON_HIT,
        TOMB_WARNING,
        ANCHOR_OVERREACH,
        SEAL_BACKLASH,
        DEATH
    }

    public enum HealSource {
        TEMPERING,
        PILL,
        NATURAL
    }

    /**
     * 所有加伤走这里：clamp、进档广播、同步包、遥测。
     *
     * @return 结算后的魂伤值
     */
    public static double addInjury(ServerPlayer player, double amount, InjurySource source) {
        if (!Double.isFinite(amount) || amount <= 0.0D) {
            return PlayerSoulInjuryData.getInjury(player);
        }
        double before = PlayerSoulInjuryData.getInjury(player);
        double after = Math.min(PlayerSoulInjuryData.MAXIMUM_INJURY, before + amount);
        if (after == before) {
            return before;
        }
        PlayerSoulInjuryData.setInjury(player, after);
        LOGGER.debug(
                "Soul injury +{} ({}) for {}: {} -> {}",
                amount, source, player.getScoreboardName(), before, after
        );
        InjuryTier beforeTier = InjuryTier.fromInjury(before);
        InjuryTier afterTier = InjuryTier.fromInjury(after);
        if (afterTier.ordinal() > beforeTier.ordinal() && afterTier.messageKey != null) {
            player.displayClientMessage(Component.translatable(afterTier.messageKey), false);
        }
        maybeSync(player, after, afterTier != beforeTier);
        return after;
    }

    /**
     * 所有疗伤走这里：温养抵扣/丹药/自然恢复。
     *
     * @return 实际疗愈的伤量（不超过当前魂伤，供温养 1:1 抵扣计算余量）
     */
    public static double heal(ServerPlayer player, double amount, HealSource source) {
        if (!Double.isFinite(amount) || amount <= 0.0D) {
            return 0.0D;
        }
        double before = PlayerSoulInjuryData.getInjury(player);
        double healed = Math.min(before, amount);
        if (healed <= 0.0D) {
            return 0.0D;
        }
        double after = before - healed;
        PlayerSoulInjuryData.setInjury(player, after);
        LOGGER.debug(
                "Soul injury -{} ({}) for {}: {} -> {}",
                healed, source, player.getScoreboardName(), before, after
        );
        maybeSync(player, after, InjuryTier.fromInjury(after) != InjuryTier.fromInjury(before));
        return healed;
    }

    public static InjuryTier getTier(Player player) {
        return InjuryTier.fromInjury(PlayerSoulInjuryData.getInjury(player));
    }

    /** 神识半径乘区（读配置）：NONE=1.0，DAZED/SHAKEN/PERIL 按档取值。 */
    public static double getSenseRangeMultiplier(Player player) {
        return switch (getTier(player)) {
            case NONE -> 1.0D;
            case DAZED -> CommonConfig.SOUL_INJURY_SENSE_RANGE_MULT_DAZED.get();
            case SHAKEN, PERIL -> CommonConfig.SOUL_INJURY_SENSE_RANGE_MULT_SHAKEN.get();
        };
    }

    /** 参悟评分扣分（读配置）：DAZED 档起生效。 */
    public static double getComprehensionPenalty(Player player) {
        return getTier(player).ordinal() >= InjuryTier.DAZED.ordinal()
                ? CommonConfig.SOUL_INJURY_COMPREHENSION_PENALTY.get()
                : 0.0D;
    }

    /** SHAKEN 档起温养停滞：进度全额转疗伤，不再涨星。 */
    public static boolean isTemperingStalled(Player player) {
        return getTier(player).ordinal() >= InjuryTier.SHAKEN.ordinal();
    }

    /** PERIL 档魂法全禁。 */
    public static boolean isSoulSpellForbidden(Player player) {
        return getTier(player) == InjuryTier.PERIL;
    }

    /** 锚位可用率（给阵禁）：魂伤达 SHAKEN 阈值时 1 - injury/200，否则 1.0。 */
    public static double getAnchorAvailabilityRatio(Player player) {
        double injury = PlayerSoulInjuryData.getInjury(player);
        if (injury < CommonConfig.SOUL_INJURY_THRESHOLD_SHAKEN.get()) {
            return 1.0D;
        }
        return 1.0D - injury / 200.0D;
    }

    /**
     * 参悟评分接入点（设计文档 R6）：在服务端拿到 assessment 后按当前魂伤扣分，
     * 不改 prompt，扣分后仍按原 minimum_learning_score 判定。
     * penalty 需在服务端主线程于发起评估时取样（{@link #getComprehensionPenalty}），
     * 避免 HTTP 回调线程读取玩家 NBT。
     */
    public static TechniqueAssessment applyComprehensionPenalty(
            TechniqueAssessment assessment,
            double penalty
    ) {
        if (penalty <= 0.0D) {
            return assessment;
        }
        int adjustedScore = Math.max(
                TechniqueComprehension.MINIMUM_SCORE,
                assessment.score() - (int) Math.round(penalty)
        );
        if (adjustedScore == assessment.score()) {
            return assessment;
        }
        return new TechniqueAssessment(adjustedScore, assessment.feedback());
    }

    /** 玩家登出/服务器停止时清理会话级同步缓存。 */
    public static void removeSyncState(UUID playerId) {
        LAST_SYNCED_INJURY.remove(playerId);
    }

    public static void clearSyncState() {
        LAST_SYNCED_INJURY.clear();
    }

    /** 同步触发：变档、±5 以上累计变动。登录/克隆重生的补发由接线方直接调 requestSync。 */
    private static void maybeSync(ServerPlayer player, double injury, boolean tierChanged) {
        Double lastSynced = LAST_SYNCED_INJURY.get(player.getUUID());
        if (!tierChanged
                && lastSynced != null
                && Math.abs(injury - lastSynced) < SYNC_DELTA_THRESHOLD) {
            return;
        }
        LAST_SYNCED_INJURY.put(player.getUUID(), injury);
        requestSync(player);
    }

    private static void requestSync(ServerPlayer player) {
        if (!player.hasDisconnected()) {
            ModNetwork.sendSoulInjury(player, SyncSoulInjuryPacket.capture(player));
        }
    }

    /** 登录/重生补发：外部事件处理器调用。 */
    public static void syncToClient(ServerPlayer player) {
        requestSync(player);
    }
}
