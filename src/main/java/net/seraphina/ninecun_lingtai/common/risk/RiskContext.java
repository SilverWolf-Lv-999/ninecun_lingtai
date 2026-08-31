package net.seraphina.ninecun_lingtai.common.risk;

import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;

import java.util.Objects;

/**
 * 一次失败事件的结算上下文（输入）。
 *
 * @param failureType     失败类型
 * @param realmTier       结算参照的境界档；凡人/无修为时可为 null（按最低档结算）。
 *                        破境失败建议传"目标境界"，其余场景传玩家当前境界
 * @param intensity       失败烈度 [0, 1]，用于缩放风险预算：1 = 完全失败（默认），
 *                        可用差一点成功的程度、劫雷剩余波次占比等映射；非法值收拢为 1
 * @param detail          自由文本，仅用于日志定位（例如 "target=golden_core_1"），null 归一为空串
 * @param coreLossHandled 核心损失是否已由调用方自行结算（默认 false）。破境失败的
 *                        散逸三档、天劫失败的折寿都属于"外部核心损失"——置 true 后
 *                        结算服务只抽长期/短期伤痕，不再叠加自带的灵力扣除（防双重扣减）
 */
public record RiskContext(
        RiskFailureType failureType,
        CultivationRealm realmTier,
        double intensity,
        String detail,
        boolean coreLossHandled
) {
    public RiskContext {
        Objects.requireNonNull(failureType, "failureType");
        intensity = Double.isFinite(intensity)
                ? Math.max(0.0D, Math.min(1.0D, intensity))
                : 1.0D;
        detail = detail == null ? "" : detail;
    }

    /** 完全失败（烈度 1）的便捷构造，核心损失默认由结算服务施加。 */
    public static RiskContext of(RiskFailureType failureType, CultivationRealm realmTier) {
        return new RiskContext(failureType, realmTier, 1.0D, "", false);
    }

    public RiskContext withIntensity(double newIntensity) {
        return new RiskContext(
                this.failureType, this.realmTier, newIntensity, this.detail, this.coreLossHandled);
    }

    public RiskContext withDetail(String newDetail) {
        return new RiskContext(
                this.failureType, this.realmTier, this.intensity, newDetail, this.coreLossHandled);
    }

    /** 标记核心损失已由调用方结算（散逸/折寿等），结算服务将跳过自带的灵力扣除。 */
    public RiskContext withCoreLossHandled() {
        return new RiskContext(
                this.failureType, this.realmTier, this.intensity, this.detail, true);
    }

    /** 境界档序数：凡人/未知与练气同为 0，仙为 8。用于预算缩放。 */
    public int realmIndex() {
        return this.realmTier == null ? 0 : this.realmTier.ordinal();
    }
}
