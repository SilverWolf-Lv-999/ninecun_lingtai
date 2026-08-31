package net.seraphina.ninecun_lingtai.common.tomb;

import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;

/**
 * 墓穴分级：墓主境界与埋葬深度正相关（越深者道行越高），出现权重递减。
 *
 * <p>深度取值满足施工图"Y&lt;-10 生成"约束：最浅一档主室顶盖也在 Y=-12 以下。
 * 权重合计 1.0，按 {@link #roll(double)} 的单位随机数区间选档。</p>
 */
public enum TombTier {
    /** 筑基墓：浅层（-18），最常见。 */
    FOUNDATION(CultivationRealm.FOUNDATION_ESTABLISHMENT, -18, 5, 3, 0.45D, 0.35F),
    /** 金丹墓：中层（-30）。 */
    GOLDEN_CORE(CultivationRealm.GOLDEN_CORE, -30, 6, 4, 0.30D, 0.55F),
    /** 元婴墓：深层（-42），双宝箱。 */
    NASCENT_SOUL(CultivationRealm.NASCENT_SOUL, -42, 6, 4, 0.18D, 0.8F),
    /** 化神墓：深板岩底层（-54），最稀有，机缘之物概率最高。 */
    SPIRIT_TRANSFORMATION(CultivationRealm.SPIRIT_TRANSFORMATION, -54, 7, 5, 0.07D, 1.0F);

    /** 主室内部净高（不含地板与顶盖）。 */
    public static final int CHAMBER_INNER_HEIGHT = 5;

    private final CultivationRealm realm;
    private final int floorY;
    private final int halfLong;
    private final int halfShort;
    private final double weight;
    private final float pressureSeverity;

    TombTier(
            CultivationRealm realm,
            int floorY,
            int halfLong,
            int halfShort,
            double weight,
            float pressureSeverity
    ) {
        this.realm = realm;
        this.floorY = floorY;
        this.halfLong = halfLong;
        this.halfShort = halfShort;
        this.weight = weight;
        this.pressureSeverity = pressureSeverity;
    }

    public CultivationRealm getRealm() {
        return this.realm;
    }

    /** 主室地板所在 Y（室内可行走面为 floorY+1）。 */
    public int getFloorY() {
        return this.floorY;
    }

    /** 长轴半径（室内），墙体在 halfLong+1 处。 */
    public int getHalfLong() {
        return this.halfLong;
    }

    /** 短轴半径（室内），墙体在 halfShort+1 处。 */
    public int getHalfShort() {
        return this.halfShort;
    }

    /** 警告态威压体感强度（复用威压后处理通道的 severity）。 */
    public float getPressureSeverity() {
        return this.pressureSeverity;
    }

    /** 元婴及以上墓穴带第二只宝箱与更高机缘概率。 */
    public boolean isHighTier() {
        return this.ordinal() >= NASCENT_SOUL.ordinal();
    }

    /** 按权重把 [0,1) 单位随机数映射到档位。 */
    public static TombTier roll(double unit) {
        double cumulative = 0.0D;
        for (TombTier tier : values()) {
            cumulative += tier.weight;
            if (unit < cumulative) {
                return tier;
            }
        }
        return SPIRIT_TRANSFORMATION;
    }
}
