package net.seraphina.ninecun_lingtai.api.alchemy;

import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * 丹炉阶别属性（炼丹 M4-a，数据驱动 {@code alchemy_furnaces/*.json}）。
 *
 * @param blockId       对应的炉主方块注册名（按方块查表的键）
 * @param heatCap       炉温上限（凡铁 600，九龙 900；火源另有加成）
 * @param heatDecayRate 每 tick 自然降温速率（1.0 = 现状凡铁；越小保温越好）
 * @param toleranceBonus 火候容错加成（0.02 = 火候评分宽容 +2%；炉性相克丹方时清零）
 * @param element       炉性元素（null = 无性/凡铁；混沌视为与诸元素平）
 * @param maxRecipeTier 可炼最高丹阶（丹方 grade.tier 超过则拒绝开炉）
 * @param maxDurability 耐久上限（每炉次损耗走配置 alchemyM4.durabilityLossPerRefine）
 */
public record FurnaceTier(
        ResourceLocation blockId,
        int heatCap,
        double heatDecayRate,
        double toleranceBonus,
        @Nullable ElementType element,
        int maxRecipeTier,
        int maxDurability
) {
    public FurnaceTier {
        Objects.requireNonNull(blockId, "blockId");
        if (heatCap <= 0 || maxRecipeTier <= 0 || maxDurability <= 0) {
            throw new IllegalArgumentException("heatCap, maxRecipeTier and maxDurability must be positive");
        }
        if (heatDecayRate < 0.0D || toleranceBonus < 0.0D) {
            throw new IllegalArgumentException("heatDecayRate and toleranceBonus must not be negative");
        }
    }
}
