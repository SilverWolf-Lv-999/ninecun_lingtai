package net.seraphina.ninecun_lingtai.common.karma;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.behavior.PlayerBehaviorLedgerData;
import net.seraphina.ninecun_lingtai.common.behavior.PlayerBehaviorLedgerData.KillCategory;
import net.seraphina.ninecun_lingtai.common.config.CommonConfig;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 杀孽唯一口径出口（道心杀孽日志 §3/§5.1/§6.1）。
 *
 * <p>杀孽是行为账本（{@link PlayerBehaviorLedgerData}）的<b>派生只读值</b>，不落独立 NBT：</p>
 * <pre>
 * 原始杀孽 rawKarma = min(cap, 村民×3 + 修士×5 + 玩家×5)   // BEAST（妖兽）不计
 * 有效杀孽 karma    = max(0, rawKarma − redeemed)          // redeemed 存账本 KarmaRedeemed 键
 * </pre>
 *
 * <p><b>勿与 {@code MindDemonSnapshot.karmaScale} 合并口径</b>：心魔面板读的是
 * <b>锁定快照</b>的 {@code totalKillsFrom}（含 BEAST），映照的是"杀戮习性"；
 * 本类读<b>实时账本</b>的三类加权（不含 BEAST），是天道计较的"孽"。两处语义
 * 不同是设计意图（道心杀孽日志风险 1），后来者切勿"顺手统一"改坏心魔平衡。</p>
 *
 <p>全部数值走 CommonConfig 的 karma 组（villagerKillKarma / cultivatorKillKarma /
 * allyKillKarma / karmaCap / tierNeutralThreshold / tierStainedThreshold /
 * tierDrenchedThreshold / daoHeartPure / daoHeartNeutral / daoHeartStained /
 * daoHeartDrenched），运行期惰性读取。</p>
 */
public final class KarmaUtility {

    private KarmaUtility() {
    }

    /**
     * 原始杀孽：三类加权求和后 clamp 上限。读<b>实时账本</b>而非锁定快照——
     * 破境掷骰前的道心看当下；心魔开战看锁定值（两处语义不同是有意为之）。
     */
    public static double getRawKarma(Player player) {
        double raw = PlayerBehaviorLedgerData.getKills(player, KillCategory.VILLAGER)
                * CommonConfig.KARMA_VILLAGER_KILL_KARMA.get()
                + PlayerBehaviorLedgerData.getKills(player, KillCategory.CULTIVATOR)
                * CommonConfig.KARMA_CULTIVATOR_KILL_KARMA.get()
                + PlayerBehaviorLedgerData.getKills(player, KillCategory.ALLY)
                * CommonConfig.KARMA_ALLY_KILL_KARMA.get();
        return Math.min(CommonConfig.KARMA_CAP.get(), raw);
    }

    /** 有效杀孽 = max(0, 原始杀孽 − 已消解量)，取值范围 [0, 200]。 */
    public static double getKarma(Player player) {
        return Math.max(0.0D, getRawKarma(player) - PlayerBehaviorLedgerData.getKarmaRedeemed(player));
    }

    /**
     * 消解杀孽（忏心丹/闭关/除魔的统一入口，只走服务端）：
     * redeemed 单调递增并 clamp ≤ 原始杀孽（原始账目永不篡改，消解是另一笔账）。
     */
    public static void redeem(ServerPlayer player, double amount) {
        if (!(amount > 0.0D) || !Double.isFinite(amount)) {
            return;
        }
        double redeemed = Math.min(
                getRawKarma(player),
                PlayerBehaviorLedgerData.getKarmaRedeemed(player) + amount
        );
        PlayerBehaviorLedgerData.setKarmaRedeemed(player, redeemed);
        ModMain.LOGGER.info(
                "[karma] {} redeemed {} karma (effective karma now {})",
                player.getName().getString(),
                String.format("%.2f", amount),
                String.format("%.2f", getKarma(player))
        );
    }

    /**
     * 道心系数（四档曲线，签名匹配 {@code DaoHeartProvider} 函数式接口）：
     * 系数进破境公式的 ID_DAO_HEART 乘区，染血及以上自动被
     * worstDraggingModifierId 捕获，触发"杀孽缠身，道心不稳"拖后腿提示。
     */
    public static double getDaoHeartCoefficient(ServerPlayer player) {
        double karma = getKarma(player);
        if (karma < CommonConfig.KARMA_TIER_NEUTRAL_THRESHOLD.get()) {
            return CommonConfig.KARMA_DAO_HEART_PURE.get();
        }
        if (karma < CommonConfig.KARMA_TIER_STAINED_THRESHOLD.get()) {
            return CommonConfig.KARMA_DAO_HEART_NEUTRAL.get();
        }
        if (karma < CommonConfig.KARMA_TIER_DRENCHED_THRESHOLD.get()) {
            return CommonConfig.KARMA_DAO_HEART_STAINED.get();
        }
        return CommonConfig.KARMA_DAO_HEART_DRENCHED.get();
    }

    /** 是否呈血色标记（有效杀孽 ≥ 染血档阈值），神识扫描分类处读取。 */
    public static boolean isBloodMarked(Player player) {
        return getKarma(player) >= CommonConfig.KARMA_TIER_STAINED_THRESHOLD.get();
    }
}
