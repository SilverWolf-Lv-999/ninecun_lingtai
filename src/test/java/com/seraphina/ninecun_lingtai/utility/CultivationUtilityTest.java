package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CultivationUtilityTest {
    private static final long[] QI_REFINING_REQUIREMENTS = {
            25L,
            35L,
            50L,
            70L,
            130L,
            250L,
            490L,
            970L,
            1930L
    };

    @Test
    void usesSpecifiedQiRefiningRequirements() {
        for (int layer = 1; layer <= CultivationStage.MAXIMUM_LAYER; layer++) {
            assertEquals(
                    BigInteger.valueOf(QI_REFINING_REQUIREMENTS[layer - 1]),
                    CultivationUtility.getRequiredSpiritualEnergy(
                            new CultivationStage(CultivationRealm.QI_REFINING, layer)
                    )
            );
        }
    }

    @Test
    void doublesThePreviousPeakForTheFirstLayerOfEachNewRealm() {
        assertEquals(
                BigInteger.valueOf(3860L),
                CultivationUtility.getRequiredSpiritualEnergy(
                        new CultivationStage(CultivationRealm.FOUNDATION_ESTABLISHMENT, 1)
                )
        );

        CultivationRealm[] realms = CultivationRealm.values();
        for (int realmIndex = 1; realmIndex < realms.length; realmIndex++) {
            BigInteger previousPeak = CultivationUtility.getRequiredSpiritualEnergy(
                    new CultivationStage(realms[realmIndex - 1], CultivationStage.MAXIMUM_LAYER)
            );
            BigInteger newRealmFirstLayer = CultivationUtility.getRequiredSpiritualEnergy(
                    new CultivationStage(realms[realmIndex], CultivationStage.MINIMUM_LAYER)
            );
            assertEquals(previousPeak.multiply(BigInteger.TWO), newRealmFirstLayer);
        }
    }

    @Test
    void increasesTheMinorRealmMultiplierForEachMajorRealm() {
        assertEquals(
                BigInteger.valueOf(11570L),
                CultivationUtility.getRequiredSpiritualEnergy(
                        new CultivationStage(CultivationRealm.FOUNDATION_ESTABLISHMENT, 2)
                )
        );

        CultivationRealm[] realms = CultivationRealm.values();
        for (int realmIndex = 1; realmIndex < realms.length; realmIndex++) {
            BigInteger multiplier = BigInteger.valueOf(realmIndex + 2L);
            for (int layer = 2; layer <= CultivationStage.MAXIMUM_LAYER; layer++) {
                BigInteger previousRequirement = CultivationUtility.getRequiredSpiritualEnergy(
                        new CultivationStage(realms[realmIndex], layer - 1)
                );
                BigInteger currentRequirement = CultivationUtility.getRequiredSpiritualEnergy(
                        new CultivationStage(realms[realmIndex], layer)
                );
                assertEquals(
                        previousRequirement.multiply(multiplier).subtract(BigInteger.TEN),
                        currentRequirement
                );
            }
        }
    }

    @Test
    void namesLayersAndHeavenlyLayersCorrectly() {
        assertEquals(
                "练气一层",
                CultivationUtility.getDisplayName(new CultivationStage(CultivationRealm.QI_REFINING, 1))
        );
        assertEquals(
                "筑基七层",
                CultivationUtility.getDisplayName(new CultivationStage(CultivationRealm.FOUNDATION_ESTABLISHMENT, 7))
        );
        assertEquals(
                "化神一重天",
                CultivationUtility.getDisplayName(new CultivationStage(CultivationRealm.SPIRIT_TRANSFORMATION, 1))
        );
        assertEquals(
                "仙九重天",
                CultivationUtility.getDisplayName(new CultivationStage(CultivationRealm.IMMORTAL, 9))
        );
    }

    @Test
    void progressesAcrossMajorRealmsAndStopsAtTheMaximum() {
        assertEquals(
                Optional.of(new CultivationStage(CultivationRealm.FOUNDATION_ESTABLISHMENT, 1)),
                new CultivationStage(CultivationRealm.QI_REFINING, 9).next()
        );
        assertTrue(new CultivationStage(CultivationRealm.IMMORTAL, 9).next().isEmpty());
    }
}
