package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.api.spell.SpellCategory;
import com.seraphina.ninecun_lingtai.api.spell.SpellGrade;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueSpell;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpellCostUtilityTest {
    private static final TechniqueSpell SPELL_DATA = new TechniqueSpell(
            ResourceLocation.fromNamespaceAndPath("ninecun_lingtai", "test_spell"),
            SpellCategory.COMBAT,
            SpellGrade.SECOND,
            4.0D,
            Set.of(ElementType.FIRE)
    );

    @Test
    void costUsesTheSameCultivationProficiencyAndAptitudeMultiplierAsPower() {
        assertEquals(4.0D, cost(1, 0.5D), 1.0E-9D);
        assertEquals(8.0D, cost(1, 1.0D), 1.0E-9D);
        assertEquals(8.0D * Math.pow(1.12D, 8), cost(9, 1.0D), 1.0E-9D);
        assertEquals(24.0D, cost(10, 0.5D), 1.0E-9D);
        assertEquals(48.0D, cost(10, 1.0D), 1.0E-9D);
        assertEquals(288.0D, cost(19, 1.0D), 1.0E-9D);
    }

    private static double cost(int cultivationStrength, double powerEfficiency) {
        return SpellCostUtility.getSpiritualEnergyCost(
                SPELL_DATA,
                cultivationStrength,
                powerEfficiency
        );
    }
}
