package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CultivationVisionUtilityTest {
    @Test
    void onlyFoundationEstablishmentAndAboveSeeNightAsDay() {
        assertFalse(CultivationVisionUtility.seesNightAsDay(Optional.empty()));
        assertFalse(CultivationVisionUtility.seesNightAsDay(Optional.of(new CultivationStage(
                CultivationRealm.QI_REFINING,
                CultivationStage.MAXIMUM_LAYER
        ))));
        assertTrue(CultivationVisionUtility.seesNightAsDay(Optional.of(new CultivationStage(
                CultivationRealm.FOUNDATION_ESTABLISHMENT,
                CultivationStage.MINIMUM_LAYER
        ))));
        assertTrue(CultivationVisionUtility.seesNightAsDay(Optional.of(new CultivationStage(
                CultivationRealm.GOLDEN_CORE,
                CultivationStage.MINIMUM_LAYER
        ))));
    }
}
