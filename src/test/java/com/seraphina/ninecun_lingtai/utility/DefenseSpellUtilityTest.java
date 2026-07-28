package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.common.spell.defense.DefenseSpellKind;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefenseSpellUtilityTest {
    @Test
    void elementalSpellsFollowContainedRootElements() {
        SpiritualRoot metalRoot = root(ElementType.METAL);
        SpiritualRoot metalWoodRoot = root(ElementType.METAL, ElementType.WOOD);

        assertTrue(DefenseSpellUtility.canUse(metalRoot, DefenseSpellKind.METAL_BODY_SHIELD));
        assertFalse(DefenseSpellUtility.canUse(metalRoot, DefenseSpellKind.WOOD_GUARDIAN));
        assertTrue(DefenseSpellUtility.canUse(metalWoodRoot, DefenseSpellKind.METAL_BODY_SHIELD));
        assertTrue(DefenseSpellUtility.canUse(metalWoodRoot, DefenseSpellKind.WOOD_GUARDIAN));
    }

    @Test
    void compositeSpellsRequireMultipleRootElements() {
        assertFalse(DefenseSpellUtility.canUse(
                root(ElementType.FIRE),
                DefenseSpellKind.COMPOSITE_BODY_SHIELD
        ));
        assertTrue(DefenseSpellUtility.canUse(
                root(ElementType.FIRE, ElementType.EARTH),
                DefenseSpellKind.COMPOSITE_BODY_SHIELD
        ));
        assertFalse(DefenseSpellUtility.canUse(
                root(ElementType.METAL, ElementType.WOOD, ElementType.WATER),
                DefenseSpellKind.FOUR_PHASE_GUARDIAN
        ));
        assertTrue(DefenseSpellUtility.canUse(
                root(ElementType.METAL, ElementType.WOOD, ElementType.WATER, ElementType.FIRE),
                DefenseSpellKind.FOUR_PHASE_GUARDIAN
        ));
    }

    @Test
    void fiveElementAndChaosSpellsRemainDistinct() {
        SpiritualRoot fiveElementRoot = root(
                ElementType.METAL,
                ElementType.WOOD,
                ElementType.WATER,
                ElementType.FIRE,
                ElementType.EARTH
        );
        SpiritualRoot chaosRoot = root(SpiritualRoot.SELECTABLE_ELEMENTS.toArray(ElementType[]::new));

        assertTrue(DefenseSpellUtility.canUse(fiveElementRoot, DefenseSpellKind.FIVE_ELEMENT_CYCLE));
        assertFalse(DefenseSpellUtility.canUse(fiveElementRoot, DefenseSpellKind.CHAOS_VOID));
        assertFalse(DefenseSpellUtility.canUse(chaosRoot, DefenseSpellKind.FIVE_ELEMENT_CYCLE));
        assertTrue(DefenseSpellUtility.canUse(chaosRoot, DefenseSpellKind.CHAOS_VOID));
    }

    private static SpiritualRoot root(ElementType... elements) {
        return new SpiritualRoot(Set.of(elements), SpiritualRoot.MAXIMUM_LENGTH_INCHES);
    }
}
