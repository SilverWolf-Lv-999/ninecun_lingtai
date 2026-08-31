package net.seraphina.ninecun_lingtai.api.alchemy;

/**
 * 火源四等（炼丹 M4-b）：开炉时判定一次，随炼制会话存 BlockEntity NBT。
 *
 * <p>判定优先级从高到低：三昧真火（炉主境界 ≥ 化神）＞ 灵火（炉下灵魂火系方块）
 * ＞ 地火（炉下邻接岩浆）＞ 凡火。升温上限加成与稳定度乘区走 CommonConfig 的
 * alchemyM4 组（枚举构造期不能读 Spec，调用方惰性取值）。
 */
public enum FireSource {
    /** 凡火：打火石点燃的寻常炉火（现状基线，无修正）。 */
    MUNDANE("mundane", "凡火"),
    /** 灵火：炉腹下灵魂火系方块温养，火性纯净，稳定度增。 */
    SPIRITUAL("spiritual", "灵火"),
    /** 地火：引地脉熔岩为薪，火力雄浑（上限增）但躁动难驯（稳定度略降）。 */
    EARTH("earth", "地火"),
    /** 三昧真火：化神修士以神念驭火，上限与稳定度俱增。 */
    SAMADHI("samadhi", "三昧真火");

    /** 语言键前缀，完整键 = 前缀 + serializedName。 */
    public static final String LANGUAGE_KEY_PREFIX = "fire_source.ninecun_lingtai.";

    private final String serializedName;
    private final String chineseName;

    FireSource(String serializedName, String chineseName) {
        this.serializedName = serializedName;
        this.chineseName = chineseName;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public String getChineseName() {
        return this.chineseName;
    }

    public String getLanguageKey() {
        return LANGUAGE_KEY_PREFIX + this.serializedName;
    }

    /** 按序号取档，越界回落凡火（NBT 解码用）。 */
    public static FireSource fromOrdinal(int ordinal) {
        FireSource[] sources = values();
        return ordinal >= 0 && ordinal < sources.length ? sources[ordinal] : MUNDANE;
    }
}
