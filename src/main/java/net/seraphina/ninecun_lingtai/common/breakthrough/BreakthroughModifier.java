package net.seraphina.ninecun_lingtai.common.breakthrough;

/**
 * 成功率公式中的单个修正项。
 *
 * @param id             修正项标识（"quality" / "dao_heart" / "technique" / "pill" / "pity"）
 * @param value          乘区项为系数（1.0 = 无影响），加算项为加数（0.0 = 无影响）
 * @param multiplicative true = 乘区（质量/道心/功法），false = 加算（丹药/瓶颈补偿）
 * @param dragging       是否"拖后腿"（低于中性值），供魂强 ≥ 5 星的玩家在 HUD 上看到提示
 */
public record BreakthroughModifier(
        String id,
        double value,
        boolean multiplicative,
        boolean dragging
) {
    public static final String ID_QUALITY = "quality";
    public static final String ID_DAO_HEART = "dao_heart";
    public static final String ID_TECHNIQUE = "technique";
    public static final String ID_PILL = "pill";
    public static final String ID_PITY = "pity";
    public static final String ID_INSIGHT = "insight";

    public static BreakthroughModifier factor(String id, double factor) {
        return new BreakthroughModifier(id, factor, true, factor < 1.0D);
    }

    public static BreakthroughModifier addend(String id, double addend, boolean draggingWhenZero) {
        return new BreakthroughModifier(id, addend, false, draggingWhenZero && addend <= 0.0D);
    }
}
