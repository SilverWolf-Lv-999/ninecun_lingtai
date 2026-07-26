package com.seraphina.ninecun_lingtai.api.technique;

import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.api.spell.SpellGrade;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Set;

/**
 * Marks a technique as a castable spell (功能类法术). Like breathing techniques, spells are ordinary
 * data-driven techniques: they are imprinted, comprehended (参悟) and trained through use. The
 * {@code spellId} references the functional implementation registered in the spell registry
 * (ModSpells, via DeferredRegister), which supplies the actual in-world behaviour.
 */
public record TechniqueSpell(
        ResourceLocation spellId,
        SpellGrade grade,
        double baseCost,
        Set<ElementType> costElements
) {
    public TechniqueSpell {
        Objects.requireNonNull(spellId, "spellId");
        Objects.requireNonNull(grade, "grade");
        if (!Double.isFinite(baseCost) || baseCost <= 0.0D) {
            throw new IllegalArgumentException("baseCost must be a positive finite number");
        }
        costElements = Set.copyOf(costElements);
        if (costElements.isEmpty()) {
            throw new IllegalArgumentException("A spell must consume at least one element of spiritual energy");
        }
    }
}
