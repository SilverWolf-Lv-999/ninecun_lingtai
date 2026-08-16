package net.seraphina.ninecun_lingtai.utility;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpiritualRealmUtilityTest {
    @Test
    void createsContinuousFourThousandBlockSettlementRegions() {
        assertTrue(SpiritualRealmUtility.isSettlementPosition(0, 0));
        assertTrue(SpiritualRealmUtility.isSettlementPosition(-2_048, -2_048));
        assertTrue(SpiritualRealmUtility.isSettlementPosition(2_047, 2_047));
        assertFalse(SpiritualRealmUtility.isSettlementPosition(2_048, 0));
        assertTrue(SpiritualRealmUtility.isSettlementPosition(8_192, 8_192));
    }

    @Test
    void spiritualRealmIsNotManagedAsAThreeThousandRealm() {
        assertFalse(ThreeThousandRealmUtility.isGeneratedThreeThousandRealmId(
                SpiritualRealmUtility.SPIRITUAL_REALM_ID
        ));
    }

    @Test
    void createsContinuousRoadGridInsideSettlementRegions() {
        assertTrue(SpiritualRealmUtility.isSettlementRoad(0, 11));
        assertTrue(SpiritualRealmUtility.isSettlementRoad(2, 11));
        assertFalse(SpiritualRealmUtility.isSettlementRoad(3, 11));
        assertTrue(SpiritualRealmUtility.isSettlementRoad(11, 32));
    }

    @Test
    void settlementHouseSelectionIsStable() {
        boolean firstResult = SpiritualRealmUtility.shouldGenerateSettlementHouse(12345L, 17, -9);
        boolean secondResult = SpiritualRealmUtility.shouldGenerateSettlementHouse(12345L, 17, -9);

        assertTrue(firstResult == secondResult);
    }

    @Test
    void alternatesLargeMineshaftAndDeepDarkRegions() {
        assertTrue(SpiritualRealmUtility.isMineshaftPosition(0, 0));
        assertFalse(SpiritualRealmUtility.isDeepDarkPosition(0, 0));
        assertFalse(SpiritualRealmUtility.isMineshaftPosition(4_096, 4_096));
        assertTrue(SpiritualRealmUtility.isDeepDarkPosition(4_096, 4_096));
    }

    @Test
    void settlementVillagerSelectionIsStable() {
        boolean firstResult = SpiritualRealmUtility.shouldGenerateSettlementVillager(98765L, -24, 51);
        boolean secondResult = SpiritualRealmUtility.shouldGenerateSettlementVillager(98765L, -24, 51);

        assertTrue(firstResult == secondResult);
    }
}
