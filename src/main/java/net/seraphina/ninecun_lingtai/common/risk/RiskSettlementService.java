package net.seraphina.ninecun_lingtai.common.risk;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.seraphina.ninecun_lingtai.common.player.PlayerCultivationData;
import net.seraphina.ninecun_lingtai.common.player.PlayerSpiritualEnergyData;
import net.seraphina.ninecun_lingtai.common.status.Scar;
import net.seraphina.ninecun_lingtai.common.status.ScarManager;
import net.seraphina.ninecun_lingtai.common.status.ScarType;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 统一风险结算服务：破境失败、渡劫失败、死亡、走火入魔的失败分支统一调用
 * {@link #settle(ServerPlayer, RiskContext)}，按"风险预算"施加并返回后果。
 *
 * <p>预算模型：预算 = 失败类型基础预算 × 境界档系数 × 失败烈度。预算按固定顺序消费——</p>
 * <ol>
 *   <li>核心损失：按预算比例扣除灵力（比例有上限）；上下文标记
 *       {@link RiskContext#coreLossHandled()} 时跳过（调用方已自行结算散逸/折寿）；</li>
 *   <li>至多 1 项长期伤痕：预算达到阈值才写入；同类型已存在时按"现有严重度 + 1"递增，
 *       封顶 {@link Scar#MAX_SEVERITY}，不无限叠加；</li>
 *   <li>至多 1 项短期限制：预算达到阈值才写入；同类型已存在时只顺延到期时间。</li>
 * </ol>
 *
 * <p>规则数值全部读 {@code CommonConfig} 的 {@code risk} 配置组，服主可调。</p>
 */
public final class RiskSettlementService {

    /** 短期限制严重度封顶（短期状态不需要太深的档位）。 */
    private static final int SHORT_TERM_MAX_SEVERITY = 3;

    /** 失败类型 -> (长期伤痕类型, 短期限制类型)。基础预算等数值均读 CommonConfig 的 risk 组。 */
    private static final Map<RiskFailureType, RiskRule> RULES = Map.of(
            RiskFailureType.BREAKTHROUGH_FAILURE,
            new RiskRule(ScarType.DAO_BASE_INJURY, ScarType.BOTTLENECK),
            RiskFailureType.TRIBULATION_FAILURE,
            new RiskRule(ScarType.SOUL_INJURY, ScarType.POST_TRIBULATION_WEAKNESS),
            RiskFailureType.DEATH,
            new RiskRule(ScarType.SOUL_INJURY, ScarType.BOTTLENECK),
            RiskFailureType.QI_DEVIATION,
            new RiskRule(ScarType.HEART_DEMON, ScarType.QI_DEVIATION)
    );

    private record RiskRule(ScarType longTermScarType, ScarType shortTermScarType) {
    }

    private static int baseBudget(RiskFailureType failureType) {
        return switch (failureType) {
            case BREAKTHROUGH_FAILURE -> CommonConfig.RISK_BREAKTHROUGH_FAILURE_BASE_BUDGET.get();
            case TRIBULATION_FAILURE -> CommonConfig.RISK_TRIBULATION_FAILURE_BASE_BUDGET.get();
            case DEATH -> CommonConfig.RISK_DEATH_BASE_BUDGET.get();
            case QI_DEVIATION -> CommonConfig.RISK_QI_DEVIATION_BASE_BUDGET.get();
        };
    }

    /**
     * 便捷入口：境界档取玩家当前境界（无修为按最低档），烈度按完全失败（1.0）结算。
     */
    public static RiskOutcome settle(ServerPlayer player, RiskFailureType failureType) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(failureType, "failureType");
        return settle(player, RiskContext.of(
                failureType,
                PlayerCultivationData.getStage(player).map(CultivationStage::realm).orElse(null)
        ));
    }

    /**
     * 核心入口：按上下文计算风险预算，施加"核心损失 + 至多 1 项长期伤痕 + 至多 1 项短期限制"。
     * 只应在服务端逻辑中调用。
     *
     * @return 已施加后果的快照，调用方据此发玩家提示/做演出
     */
    public static RiskOutcome settle(ServerPlayer player, RiskContext context) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(context, "context");
        RiskRule rule = RULES.get(context.failureType());
        int budget = computeBudget(rule, context);
        if (budget <= 0) {
            return RiskOutcome.none(context.failureType());
        }

        // 核心损失已由调用方结算（破境散逸/天劫折寿）时跳过自带的灵力扣除，防双重扣减。
        double coreLossRatio = context.coreLossHandled()
                ? 0.0D
                : applyCoreLoss(player, budget);
        Scar longTermScar = budget >= CommonConfig.RISK_LONG_TERM_SCAR_BUDGET_THRESHOLD.get()
                ? applyLongTermScar(player, rule.longTermScarType(), budget)
                : null;
        Scar shortTermScar = budget >= CommonConfig.RISK_SHORT_TERM_SCAR_BUDGET_THRESHOLD.get()
                ? applyShortTermScar(player, rule.shortTermScarType(), budget)
                : null;

        RiskOutcome outcome = new RiskOutcome(
                context.failureType(),
                budget,
                coreLossRatio,
                context.coreLossHandled(),
                longTermScar,
                shortTermScar
        );
        ModMain.LOGGER.info(
                "[risk] settled for {} ({}): {}",
                player.getName().getString(),
                context.detail().isEmpty() ? "no detail" : context.detail(),
                outcome.describe()
        );
        return outcome;
    }

    private static int computeBudget(RiskRule rule, RiskContext context) {
        double realmScale = 1.0D + CommonConfig.RISK_REALM_BUDGET_STEP.get() * context.realmIndex();
        return (int) Math.round(baseBudget(context.failureType()) * realmScale * context.intensity());
    }

    /**
     * 核心损失：当前实现为按比例扣除全属性灵力（走现有公开 API，不改动其类）。
     *
     * @return 实际施加的损失比例
     */
    private static double applyCoreLoss(ServerPlayer player, int budget) {
        double lossRatio = Math.min(
                CommonConfig.RISK_MAX_CORE_LOSS_RATIO.get(),
                budget * CommonConfig.RISK_CORE_LOSS_RATIO_PER_BUDGET.get()
        );
        if (lossRatio <= 0.0D) {
            return 0.0D;
        }
        PlayerSpiritualEnergyData.retainEnergyRatio(player, 1.0D - lossRatio);
        // TODO(接线): 高预算时的修为跌落（掉小层）尚待策划定档，届时经
        //  PlayerCultivationData.getStage/setStage 实现；见开发日志"待接入点"。
        // TODO(接线): 寿元扣减——仓库目前没有独立寿元数据类的公开 API
        //  （丹药加寿逻辑在 PillEffectApplier 内部），待寿元系统落地后补。
        return lossRatio;
    }

    /** 长期伤痕：默认永久。同类型已存在时严重度 +1 递增（封顶），体现屡败屡损但不叠满。 */
    private static Scar applyLongTermScar(ServerPlayer player, ScarType type, int budget) {
        int budgetSeverity = Scar.MIN_SEVERITY
                + (budget - CommonConfig.RISK_LONG_TERM_SCAR_BUDGET_THRESHOLD.get())
                / CommonConfig.RISK_SEVERITY_BUDGET_STEP.get();
        int escalatedSeverity = ScarManager.getSeverity(player, type) + 1;
        int severity = Math.min(Scar.MAX_SEVERITY, Math.max(budgetSeverity, escalatedSeverity));
        return ScarManager.applyScar(player, type, severity);
    }

    /** 短期限制：时长随预算增长；同类型已存在时由合并规则顺延到期、取较高严重度。 */
    private static Scar applyShortTermScar(ServerPlayer player, ScarType type, int budget) {
        int severity = Math.min(
                SHORT_TERM_MAX_SEVERITY,
                Scar.MIN_SEVERITY
                        + (budget - CommonConfig.RISK_SHORT_TERM_SCAR_BUDGET_THRESHOLD.get())
                        / CommonConfig.RISK_SEVERITY_BUDGET_STEP.get()
        );
        long durationTicks = budget * CommonConfig.RISK_SHORT_TERM_DURATION_TICKS_PER_BUDGET.get();
        return ScarManager.applyScar(player, type, severity, durationTicks);
    }

    /** 查询规则表中某失败类型的长期伤痕类型（供外部系统预判提示用）。 */
    public static Optional<ScarType> getLongTermScarType(RiskFailureType failureType) {
        return Optional.ofNullable(RULES.get(failureType)).map(RiskRule::longTermScarType);
    }

    /** 查询规则表中某失败类型的短期限制类型（供外部系统预判提示用）。 */
    public static Optional<ScarType> getShortTermScarType(RiskFailureType failureType) {
        return Optional.ofNullable(RULES.get(failureType)).map(RiskRule::shortTermScarType);
    }
}
