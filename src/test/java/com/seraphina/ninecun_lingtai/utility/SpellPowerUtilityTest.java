package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueMasteryStage;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpellPowerUtilityTest {
    @Test
    void proficiencyContinuouslyImprovesPowerBetweenMasteryStages() {
        assertEquals(0.5D, efficiency(0L), 1.0E-9D);
        assertEquals(0.6D, efficiency(30L), 1.0E-9D);
        assertEquals(0.7D, efficiency(60L), 1.0E-9D);
        assertEquals(0.775D, efficiency(210L), 1.0E-9D);
        assertEquals(0.85D, efficiency(360L), 1.0E-9D);
        assertEquals(0.925D, efficiency(1080L), 1.0E-9D);
        assertEquals(1.0D, efficiency(1800L), 1.0E-9D);
    }

    @Test
    void learnedMasteryRemainsTheMinimumPowerFloor() {
        assertEquals(
                0.85D,
                SpellPowerUtility.getProficiencyEfficiency(TechniqueMasteryStage.MAJOR_MASTERY, 0L),
                1.0E-9D
        );
    }

    @Test
    void spiritualRootLengthScalesAptitudeAndCombinedPower() {
        SpiritualRoot oneInchRoot = new SpiritualRoot(Set.of(ElementType.FIRE), 1.0F);
        SpiritualRoot nineInchRoot = new SpiritualRoot(Set.of(ElementType.FIRE), 9.0F);

        assertEquals(1.0D / 9.0D, SpellPowerUtility.getAptitudeEfficiency(Optional.of(oneInchRoot)), 1.0E-9D);
        assertEquals(1.0D, SpellPowerUtility.getAptitudeEfficiency(Optional.of(nineInchRoot)), 1.0E-9D);
        assertEquals(
                0.5D / 9.0D,
                SpellPowerUtility.getPowerEfficiency(
                        TechniqueMasteryStage.ENTRY,
                        0L,
                        Optional.of(oneInchRoot)
                ),
                1.0E-9D
        );
    }

    @Test
    void cultivationPowerJumpsByMajorRealmAndGrowsWithinEachRealm() {
        assertEquals(1.0D, SpellPowerUtility.getCultivationPower(1), 1.0E-9D);
        assertEquals(
                Math.pow(1.12D, 8),
                SpellPowerUtility.getCultivationPower(9),
                1.0E-9D
        );
        assertEquals(6.0D, SpellPowerUtility.getCultivationPower(10), 1.0E-9D);
        assertEquals(
                6.0D * Math.pow(1.12D, 8),
                SpellPowerUtility.getCultivationPower(18),
                1.0E-9D
        );
        assertEquals(36.0D, SpellPowerUtility.getCultivationPower(19), 1.0E-9D);
    }

    private static double efficiency(long proficiency) {
        return SpellPowerUtility.getProficiencyEfficiency(TechniqueMasteryStage.ENTRY, proficiency);
    }
}
