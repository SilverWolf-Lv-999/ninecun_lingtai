package net.seraphina.ninecun_lingtai.utility;

import net.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueSpell;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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

    /**
     * Resolves the spiritual-energy pools that should pay for a spell.
     * Chaos spiritual roots use their chaos energy to drive spells with a
     * specific elemental cost.
     */
    public static Set<ElementType> getEffectiveCostElements(
            TechniqueSpell spellData,
            Optional<SpiritualRoot> spiritualRoot
    ) {
        Objects.requireNonNull(spellData, "spellData");
        Objects.requireNonNull(spiritualRoot, "spiritualRoot");
        if (!spellData.consumesAnyElement() && spiritualRoot.filter(SpiritualRoot::isChaos).isPresent()) {
            return Set.of(ElementType.CHAOS);
        }
        return spellData.costElements();
    }
}
