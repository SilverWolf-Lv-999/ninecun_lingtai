package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.common.player.DefenseResourceType;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CultivationDefenseUtilityTest {
    @Test
    void addsHealthThroughFoundationAndStopsAtGoldenCore() {
        assertEquals(0.0D, CultivationDefenseUtility.getMaxHealthBonus(Optional.empty()));
        assertEquals(1.0D, CultivationDefenseUtility.getMaxHealthBonus(
                new CultivationStage(CultivationRealm.QI_REFINING, 1)
        ));
        assertEquals(9.0D, CultivationDefenseUtility.getMaxHealthBonus(
                new CultivationStage(CultivationRealm.QI_REFINING, 9)
        ));
        assertEquals(12.0D, CultivationDefenseUtility.getMaxHealthBonus(
                new CultivationStage(CultivationRealm.FOUNDATION_ESTABLISHMENT, 1)
        ));
        assertEquals(36.0D, CultivationDefenseUtility.getMaxHealthBonus(
                new CultivationStage(CultivationRealm.FOUNDATION_ESTABLISHMENT, 9)
        ));
        assertEquals(36.0D, CultivationDefenseUtility.getMaxHealthBonus(
                new CultivationStage(CultivationRealm.GOLDEN_CORE, 9)
        ));
        assertEquals(36.0D, CultivationDefenseUtility.getMaxHealthBonus(
                new CultivationStage(CultivationRealm.NASCENT_SOUL, 9)
        ));
    }

    @Test
    void goldenCoreBodyShieldOnlyRespondsToLowerCultivation() {
        CultivationStage defender = new CultivationStage(CultivationRealm.GOLDEN_CORE, 5);

        assertTrue(CultivationDefenseUtility.getDefensiveReaction(
                defender,
                Optional.empty(),
                4.0D
        ).orElseThrow().showsBodyShield());
        assertTrue(CultivationDefenseUtility.getDefensiveReaction(
                defender,
                Optional.of(new CultivationStage(CultivationRealm.GOLDEN_CORE, 4)),
                4.0D
        ).orElseThrow().showsBodyShield());
        assertFalse(CultivationDefenseUtility.getDefensiveReaction(
                defender,
                Optional.of(new CultivationStage(CultivationRealm.GOLDEN_CORE, 5)),
                4.0D
        ).isPresent());
        assertFalse(CultivationDefenseUtility.getDefensiveReaction(
                defender,
                Optional.of(new CultivationStage(CultivationRealm.GOLDEN_CORE, 6)),
                4.0D
        ).isPresent());
    }

    @Test
    void nascentSoulDefenseRequiresALowerMajorRealmAndHasNoBodyShield() {
        CultivationStage defender = new CultivationStage(CultivationRealm.NASCENT_SOUL, 1);

        assertTrue(CultivationDefenseUtility.getDefensiveReaction(
                defender,
                Optional.of(new CultivationStage(CultivationRealm.GOLDEN_CORE, 9)),
                4.0D
        ).orElseThrow().reflectedDamage() >= 4.0D);
        assertFalse(CultivationDefenseUtility.getDefensiveReaction(
                defender,
                Optional.of(new CultivationStage(CultivationRealm.GOLDEN_CORE, 9)),
                4.0D
        ).orElseThrow().showsBodyShield());
        assertFalse(CultivationDefenseUtility.getDefensiveReaction(
                defender,
                Optional.of(new CultivationStage(CultivationRealm.NASCENT_SOUL, 1)),
                4.0D
        ).isPresent());
        assertFalse(CultivationDefenseUtility.getDefensiveReaction(
                defender,
                Optional.of(new CultivationStage(CultivationRealm.NASCENT_SOUL, 9)),
                4.0D
        ).isPresent());
    }

    @Test
    void calculatesDefenseResourceMaximumsByRealmAndLayer() {
        assertEquals(0, CultivationDefenseUtility.getMaximumDefenseResource(Optional.empty()));
        assertEquals(0, CultivationDefenseUtility.getMaximumDefenseResource(
                new CultivationStage(CultivationRealm.FOUNDATION_ESTABLISHMENT, 9)
        ));
        assertEquals(500, CultivationDefenseUtility.getMaximumDefenseResource(
                new CultivationStage(CultivationRealm.GOLDEN_CORE, 1)
        ));
        assertEquals(1300, CultivationDefenseUtility.getMaximumDefenseResource(
                new CultivationStage(CultivationRealm.GOLDEN_CORE, 9)
        ));
        assertEquals(100, CultivationDefenseUtility.getMaximumDefenseResource(
                new CultivationStage(CultivationRealm.NASCENT_SOUL, 1)
        ));
        assertEquals(2500, CultivationDefenseUtility.getMaximumDefenseResource(
                new CultivationStage(CultivationRealm.NASCENT_SOUL, 9)
        ));
        assertEquals(50, CultivationDefenseUtility.getMaximumDefenseResource(
                new CultivationStage(CultivationRealm.SPIRIT_TRANSFORMATION, 1)
        ));
        assertEquals(3550, CultivationDefenseUtility.getMaximumDefenseResource(
                new CultivationStage(CultivationRealm.SPIRIT_TRANSFORMATION, 2)
        ));
        assertEquals(34050, CultivationDefenseUtility.getMaximumDefenseResource(
                new CultivationStage(CultivationRealm.DAO_INTEGRATION, 1)
        ));
    }

    @Test
    void highDamageImmunityCostChangesByRealm() {
        assertEquals(OptionalInt.of(250), CultivationDefenseUtility.getHighDamageImmunityCost(
                new CultivationStage(CultivationRealm.GOLDEN_CORE, 1)
        ));
        assertEquals(OptionalInt.of(10), CultivationDefenseUtility.getHighDamageImmunityCost(
                new CultivationStage(CultivationRealm.NASCENT_SOUL, 1)
        ));
        assertEquals(OptionalInt.of(1), CultivationDefenseUtility.getHighDamageImmunityCost(
                new CultivationStage(CultivationRealm.SPIRIT_TRANSFORMATION, 1)
        ));
        assertTrue(CultivationDefenseUtility.getHighDamageImmunityCost(
                new CultivationStage(CultivationRealm.DAO_INTEGRATION, 1)
        ).isEmpty());
        assertTrue(CultivationDefenseUtility.isFullyImmuneToHighDamage(
                new CultivationStage(CultivationRealm.DAO_INTEGRATION, 1)
        ));
        assertFalse(CultivationDefenseUtility.isFullyImmuneToHighDamage(
                new CultivationStage(CultivationRealm.SPIRIT_TRANSFORMATION, 9)
        ));
    }

    @Test
    void defenseIsActiveOnlyForMatchingUnbrokenResource() {
        CultivationStage goldenCore = new CultivationStage(CultivationRealm.GOLDEN_CORE, 1);
        CultivationStage nascentSoul = new CultivationStage(CultivationRealm.NASCENT_SOUL, 1);

        assertTrue(CultivationDefenseUtility.isActiveDefense(
                Optional.of(goldenCore),
                DefenseResourceType.BODY_WIND,
                1
        ));
        assertTrue(CultivationDefenseUtility.isActiveDefense(
                Optional.of(nascentSoul),
                DefenseResourceType.SPIRITUAL_POWER,
                1
        ));
        assertFalse(CultivationDefenseUtility.isActiveDefense(
                Optional.of(goldenCore),
                DefenseResourceType.BODY_WIND,
                0
        ));
        assertFalse(CultivationDefenseUtility.isActiveDefense(
                Optional.of(goldenCore),
                DefenseResourceType.SPIRITUAL_POWER,
                1
        ));
        assertFalse(CultivationDefenseUtility.isActiveDefense(
                Optional.empty(),
                DefenseResourceType.BODY_WIND,
                1
        ));
    }

    @Test
    void calculatesMeditationDefenseResourceRepairAmounts() {
        assertEquals(5, CultivationDefenseUtility.getDefenseResourceRepairAmount(
                Optional.of(new CultivationStage(CultivationRealm.GOLDEN_CORE, 1)),
                1.0D
        ));
        assertEquals(1, CultivationDefenseUtility.getDefenseResourceRepairAmount(
                Optional.of(new CultivationStage(CultivationRealm.NASCENT_SOUL, 1)),
                0.1D
        ));
        assertEquals(341, CultivationDefenseUtility.getDefenseResourceRepairAmount(
                Optional.of(new CultivationStage(CultivationRealm.DAO_INTEGRATION, 1)),
                1.0D
        ));
        assertEquals(50, CultivationDefenseUtility.getDefenseResourceRepairAmount(
                Optional.of(new CultivationStage(CultivationRealm.SPIRIT_TRANSFORMATION, 1)),
                100.0D
        ));
        assertEquals(0, CultivationDefenseUtility.getDefenseResourceRepairAmount(Optional.empty(), 1.0D));
        assertEquals(0, CultivationDefenseUtility.getDefenseResourceRepairAmount(
                Optional.of(new CultivationStage(CultivationRealm.GOLDEN_CORE, 1)),
                0.0D
        ));
    }
}
