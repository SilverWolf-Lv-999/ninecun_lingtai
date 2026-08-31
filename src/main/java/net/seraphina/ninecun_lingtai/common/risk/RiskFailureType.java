package net.seraphina.ninecun_lingtai.common.risk;

/**
 * 统一风险结算覆盖的失败类型。破境/天劫/死亡/走火入魔各系统在失败分支
 * 构造对应类型的 {@link RiskContext} 交给 {@link RiskSettlementService} 结算。
 */
public enum RiskFailureType {
    /** 破境（升大境界/小层）失败。 */
    BREAKTHROUGH_FAILURE("breakthrough_failure", "破境失败"),
    /** 渡劫失败（未身死，身死走 DEATH）。 */
    TRIBULATION_FAILURE("tribulation_failure", "渡劫失败"),
    /** 死亡。 */
    DEATH("death", "死亡"),
    /** 走火入魔。 */
    QI_DEVIATION("qi_deviation", "走火入魔");

    private final String serializedName;
    private final String chineseName;

    RiskFailureType(String serializedName, String chineseName) {
        this.serializedName = serializedName;
        this.chineseName = chineseName;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    /** 中文名，仅用于日志；玩家可见文本走语言键（接线时补）。 */
    public String getChineseName() {
        return this.chineseName;
    }
}
