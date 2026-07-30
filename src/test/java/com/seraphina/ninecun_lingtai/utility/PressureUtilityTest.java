package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PressureUtilityTest {
    @Test
    void pressureRangeStartsAtFiveBlocksAndGrowsByLayerAndRealm() {
        assertEquals(5.0D, PressureUtility.getPressureRange(stage(CultivationRealm.QI_REFINING, 1)));
        assertEquals(13.0D, PressureUtility.getPressureRange(stage(CultivationRealm.QI_REFINING, 9)));
        assertEquals(35.0D, PressureUtility.getPressureRange(
                stage(CultivationRealm.FOUNDATION_ESTABLISHMENT, 1)
        ));
    }

    @Test
    void pressureOnlyImmobilizesLowerTargetsAfterFoundationEstablishment() {
        assertFalse(PressureUtility.hasAdvancedEffect(stage(CultivationRealm.QI_REFINING, 9)));
        assertTrue(PressureUtility.hasAdvancedEffect(stage(CultivationRealm.FOUNDATION_ESTABLISHMENT, 1)));
        assertTrue(PressureUtility.isLower(
                stage(CultivationRealm.FOUNDATION_ESTABLISHMENT, 1),
                Optional.of(stage(CultivationRealm.QI_REFINING, 9))
        ));
    }

    @Test
    void twoMajorRealmsOfDifferenceTriggerPressureExecution() {
        assertTrue(PressureUtility.shouldInstantlyKill(
                stage(CultivationRealm.GOLDEN_CORE, 1),
                Optional.of(stage(CultivationRealm.QI_REFINING, 9))
        ));
        assertFalse(PressureUtility.shouldInstantlyKill(
                stage(CultivationRealm.FOUNDATION_ESTABLISHMENT, 9),
                Optional.of(stage(CultivationRealm.QI_REFINING, 1))
        ));
    }

    private static CultivationStage stage(CultivationRealm realm, int layer) {
        return new CultivationStage(realm, layer);
    }
}
