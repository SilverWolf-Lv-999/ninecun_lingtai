package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.api.technique.TechniqueSpell;

import java.util.Objects;

public final class SpellCostUtility {

    /**
     * 灵力消耗与本次实际威力使用同一倍率。
     * 基础消耗表示练气一层、圆满熟练度、九寸灵根时的一品消耗，品级再单独放大消耗。
     */
    public static double getSpiritualEnergyCost(
            TechniqueSpell spellData,
            int cultivationStrength,
            double powerEfficiency
    ) {
        Objects.requireNonNull(spellData, "spellData");
        return spellData.baseCost()
                * spellData.grade().getCostMultiplier()
                * SpellPowerUtility.getPowerMultiplier(cultivationStrength, powerEfficiency);
    }
}
