package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CultivationCombatUtilityTest {
    @Test
    void spiritTransformationAndHigherInstantlyKillNascentSoulAndLower() {
        assertTrue(CultivationCombatUtility.canInstantlyKillLowerRealmCultivator(
                stage(CultivationRealm.SPIRIT_TRANSFORMATION, 1),
                stage(CultivationRealm.NASCENT_SOUL, 9)
        ));
        assertTrue(CultivationCombatUtility.canInstantlyKillLowerRealmCultivator(
                stage(CultivationRealm.DAO_INTEGRATION, 1),
                stage(CultivationRealm.GOLDEN_CORE, 9)
        ));
        assertTrue(CultivationCombatUtility.canInstantlyKillLowerRealmCultivator(
                stage(CultivationRealm.IMMORTAL, 9),
                Optional.empty()
        ));
    }

    @Test
    void spiritTransformationTargetsAreBeyondTheInstantKillBoundary() {
        assertFalse(CultivationCombatUtility.canInstantlyKillLowerRealmCultivator(
                stage(CultivationRealm.SPIRIT_TRANSFORMATION, 9),
                stage(CultivationRealm.SPIRIT_TRANSFORMATION, 1)
        ));
        assertFalse(CultivationCombatUtility.canInstantlyKillLowerRealmCultivator(
                stage(CultivationRealm.DAO_INTEGRATION, 9),
                stage(CultivationRealm.SPIRIT_TRANSFORMATION, 1)
        ));
    }

    @Test
    void cultivatorsBelowSpiritTransformationCannotUseRealmExecution() {
        assertFalse(CultivationCombatUtility.canInstantlyKillLowerRealmCultivator(
                stage(CultivationRealm.NASCENT_SOUL, 9),
                stage(CultivationRealm.QI_REFINING, 1)
        ));
        assertFalse(CultivationCombatUtility.canInstantlyKillLowerRealmCultivator(
                Optional.empty(),
                stage(CultivationRealm.QI_REFINING, 1)
        ));
    }

    private static Optional<CultivationStage> stage(CultivationRealm realm, int layer) {
        return Optional.of(new CultivationStage(realm, layer));
    }
}
