package net.seraphina.ninecun_lingtai.common.aura;

import net.seraphina.ninecun_lingtai.common.config.CommonConfig;

/**
 * 灵气质量四档（仙途总纲 7.4）：凡浊 / 平和 / 清灵 / 极清。
 *
 * <p>破境系数与 {@code QiQualityProvider} javadoc 口径一致
 * （浑浊 0.70 / 中平 1.00 / 清灵 1.15 / 至纯 1.30），实际数值走
 * CommonConfig 的 breakthrough.qualityCoef* 键——枚举构造期不能读
 * ForgeConfigSpec，故 {@link #getBreakthroughCoefficient()} 惰性求值。
 * 语言键为 translatableWithFallback 的兜底中文。
 */
public enum AuraQuality {
    /** 凡浊：人口密集或污秽之地，破境系数默认 0.70。 */
    FOUL("凡浊"),
    /** 平和：寻常荒野，破境系数默认 1.00。 */
    BALANCED("平和"),
    /** 清灵：远离人烟的清净之地，破境系数默认 1.15。 */
    CLEAR("清灵"),
    /** 极清：荒野高山绝顶，破境系数默认 1.30。 */
    PRISTINE("极清");

    /** 语言键前缀，完整键 = 前缀 + name().toLowerCase()。 */
    public static final String LANGUAGE_KEY_PREFIX = "aura.ninecun_lingtai.quality.";

    private final String chineseName;

    AuraQuality(String chineseName) {
        this.chineseName = chineseName;
    }

    /** 破境成功率乘区系数（BreakthroughChanceCalculator 用；运行期惰性读配置）。 */
    public double getBreakthroughCoefficient() {
        return switch (this) {
            case FOUL -> CommonConfig.BREAKTHROUGH_QUALITY_COEF_TURBID.get();
            case BALANCED -> CommonConfig.BREAKTHROUGH_QUALITY_COEF_NEUTRAL.get();
            case CLEAR -> CommonConfig.BREAKTHROUGH_QUALITY_COEF_CLEAR.get();
            case PRISTINE -> CommonConfig.BREAKTHROUGH_QUALITY_COEF_PRISTINE.get();
        };
    }

    /** 中文档位名（同时作为 translatableWithFallback 的兜底文案）。 */
    public String getChineseName() {
        return this.chineseName;
    }

    /** 语言键（"aura.ninecun_lingtai.quality.foul" 等，词条见接线清单）。 */
    public String getLanguageKey() {
        return LANGUAGE_KEY_PREFIX + name().toLowerCase(java.util.Locale.ROOT);
    }

    /** 按序号取档，越界收拢到边界档（用于降档运算与网络解码）。 */
    public static AuraQuality fromOrdinal(int ordinal) {
        AuraQuality[] qualities = values();
        return qualities[Math.max(0, Math.min(qualities.length - 1, ordinal))];
    }

    /** 降档 {@code tiers} 档（过度采集惩罚），下限凡浊。 */
    public AuraQuality downgradeBy(int tiers) {
        return fromOrdinal(ordinal() - Math.max(0, tiers));
    }
}
