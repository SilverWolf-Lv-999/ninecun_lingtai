package net.seraphina.ninecun_lingtai.common.risk;

import net.seraphina.ninecun_lingtai.common.status.Scar;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * 一次风险结算的产出（已施加到玩家身上的结果快照），供调用方发提示、记日志、做后续演出。
 *
 * @param failureType      本次结算的失败类型
 * @param riskBudget       实际使用的风险预算（诊断用）
 * @param coreLossRatio    已施加的核心损失比例（当前实现为灵力扣除比例），0 表示无核心损失
 * @param coreLossExternal 核心损失是否由调用方在结算服务之外自行施加（散逸/折寿等）；
 *                         为 true 时 {@code coreLossRatio} 恒为 0，{@link #describe()} 注明 external
 * @param longTermScar     已写入的长期伤痕，可为 null；请通过 {@link #longTerm()} 读取
 * @param shortTermScar    已写入的短期限制，可为 null；请通过 {@link #shortTerm()} 读取
 */
public record RiskOutcome(
        RiskFailureType failureType,
        int riskBudget,
        double coreLossRatio,
        boolean coreLossExternal,
        Scar longTermScar,
        Scar shortTermScar
) {
    public RiskOutcome {
        Objects.requireNonNull(failureType, "failureType");
    }

    /** 预算不足以产生任何后果时的空结果。 */
    public static RiskOutcome none(RiskFailureType failureType) {
        return new RiskOutcome(failureType, 0, 0.0D, false, null, null);
    }

    public Optional<Scar> longTerm() {
        return Optional.ofNullable(this.longTermScar);
    }

    public Optional<Scar> shortTerm() {
        return Optional.ofNullable(this.shortTermScar);
    }

    public boolean hasAnyConsequence() {
        return this.coreLossRatio > 0.0D || this.longTermScar != null || this.shortTermScar != null;
    }

    /** 单行诊断文本，供日志使用；核心损失外部结算时注明 external。 */
    public String describe() {
        return String.format(
                Locale.ROOT,
                "failure=%s, budget=%d, coreLoss=%s, longTerm=%s, shortTerm=%s",
                this.failureType.getSerializedName(),
                this.riskBudget,
                this.coreLossExternal
                        ? "external"
                        : String.format(Locale.ROOT, "%.2f", this.coreLossRatio),
                this.longTermScar == null
                        ? "none"
                        : this.longTermScar.type().getSerializedName() + "@" + this.longTermScar.severity(),
                this.shortTermScar == null
                        ? "none"
                        : this.shortTermScar.type().getSerializedName() + "@" + this.shortTermScar.severity()
        );
    }
}
