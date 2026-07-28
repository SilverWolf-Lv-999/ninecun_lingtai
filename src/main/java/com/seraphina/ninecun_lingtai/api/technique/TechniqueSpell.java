package com.seraphina.ninecun_lingtai.api.technique;

import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.api.spell.SpellCategory;
import com.seraphina.ninecun_lingtai.api.spell.SpellGrade;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Set;

/**
 * Marks a technique as a castable spell (功能类法术). Like breathing techniques, spells are ordinary
 * data-driven techniques: they are imprinted, comprehended (参悟) and trained through use. The
 * {@code spellId} references the functional implementation registered in the spell registry
 * (ModSpells, via DeferredRegister), which supplies the actual in-world behaviour.
 *
 * <p>{@code baseCost} 表示练气一层、圆满熟练度、九寸灵根时的一品基础消耗；实际施法时还会
 * 乘以法术品级与本次威力倍率。{@code costElements} 为空表示此法术不挑灵气属性，施展时从
 * 修士现有的任意灵力中扣取。
 */
public record TechniqueSpell(
        ResourceLocation spellId,
        SpellCategory category,
        SpellGrade grade,
        double baseCost,
        Set<ElementType> costElements
) {
    public TechniqueSpell {
        Objects.requireNonNull(spellId, "spellId");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(grade, "grade");
        if (!Double.isFinite(baseCost) || baseCost <= 0.0D) {
            throw new IllegalArgumentException("baseCost must be a positive finite number");
        }
        costElements = Set.copyOf(costElements);
    }

    /** 是否不挑灵气属性（消耗任意灵力）。 */
    public boolean consumesAnyElement() {
        return this.costElements.isEmpty();
    }
}
