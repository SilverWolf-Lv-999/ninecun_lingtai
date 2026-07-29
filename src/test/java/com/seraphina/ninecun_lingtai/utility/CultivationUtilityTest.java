package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void usesTheNextTargetRequirementAsTheAbsorptionLimit() {
        assertEquals(
                BigInteger.valueOf(35L),
                CultivationUtility.getMaximumAbsorbableSpiritualEnergy(
                        Optional.of(new CultivationStage(CultivationRealm.QI_REFINING, 1))
                )
        );
        assertEquals(
                BigInteger.valueOf(3860L),
                CultivationUtility.getMaximumAbsorbableSpiritualEnergy(
                        Optional.of(new CultivationStage(CultivationRealm.QI_REFINING, 9))
                )
        );
        assertEquals(
                CultivationUtility.getRequiredSpiritualEnergy(
                        new CultivationStage(CultivationRealm.IMMORTAL, CultivationStage.MAXIMUM_LAYER)
                ),
                CultivationUtility.getMaximumAbsorbableSpiritualEnergy(
                        Optional.of(new CultivationStage(CultivationRealm.IMMORTAL, 9))
                )
        );
    }

    @Test
    void calculatesBreakthroughCostForEveryIndependentRoot() {
        CultivationStage firstStage = CultivationStage.first();
        SpiritualRoot singleRoot = new SpiritualRoot(Set.of(ElementType.METAL), 9.0F);
        SpiritualRoot doubleRoot = new SpiritualRoot(
                Set.of(ElementType.METAL, ElementType.WATER),
                9.0F
        );
        SpiritualRoot yinYangRoot = new SpiritualRoot(Set.of(ElementType.YIN_YANG), 9.0F);
        SpiritualRoot chaosRoot = new SpiritualRoot(SpiritualRoot.SELECTABLE_ELEMENTS, 9.0F);

        assertEquals(BigInteger.valueOf(25L), CultivationUtility.getCombinedRequiredSpiritualEnergy(
                firstStage,
                singleRoot
        ));
        assertEquals(BigInteger.valueOf(50L), CultivationUtility.getCombinedRequiredSpiritualEnergy(
                firstStage,
                doubleRoot
        ));
        assertEquals(BigInteger.valueOf(25L), CultivationUtility.getCombinedRequiredSpiritualEnergy(
                firstStage,
                yinYangRoot
        ));
        assertEquals(BigInteger.valueOf(25L), CultivationUtility.getCombinedRequiredSpiritualEnergy(
                firstStage,
                chaosRoot
        ));
    }

    @Test
    void requiresEveryIndependentRootToBeCompleteForEveryBreakthrough() {
        SpiritualRoot doubleRoot = new SpiritualRoot(
                Set.of(ElementType.METAL, ElementType.WATER),
                9.0F
        );

        for (CultivationRealm realm : CultivationRealm.values()) {
            for (int layer = CultivationStage.MINIMUM_LAYER;
                 layer <= CultivationStage.MAXIMUM_LAYER;
                 layer++) {
                CultivationStage targetStage = new CultivationStage(realm, layer);
                double requiredEnergy = CultivationUtility.getRequiredSpiritualEnergy(targetStage).doubleValue();

                assertFalse(CultivationUtility.areAllSpiritualRootsComplete(
                        targetStage,
                        doubleRoot,
                        Map.of(ElementType.METAL, requiredEnergy * 2.0D)
                ));
                assertFalse(CultivationUtility.areAllSpiritualRootsComplete(
                        targetStage,
                        doubleRoot,
                        Map.of(
                                ElementType.METAL, requiredEnergy,
                                ElementType.WATER, Math.nextDown(requiredEnergy)
                        )
                ));
                assertTrue(CultivationUtility.areAllSpiritualRootsComplete(
                        targetStage,
                        doubleRoot,
                        Map.of(
                                ElementType.METAL, requiredEnergy,
                                ElementType.WATER, requiredEnergy
                        )
                ));
            }
        }
    }

    @Test
    void letsASingleRootEnterQiRefiningOnlyAfterReachingTwentyFiveEnergy() {
        CultivationStage firstStage = CultivationStage.first();
        SpiritualRoot singleRoot = new SpiritualRoot(Set.of(ElementType.METAL), 9.0F);

        assertFalse(CultivationUtility.areAllSpiritualRootsComplete(
                firstStage,
                singleRoot,
                Map.of(ElementType.METAL, Math.nextDown(25.0D))
        ));
        assertTrue(CultivationUtility.areAllSpiritualRootsComplete(
                firstStage,
                singleRoot,
                Map.of(ElementType.METAL, 25.0D)
        ));
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

    @Test
    void formatsAllCultivationNumbersWithChineseCharacters() {
        assertEquals("九寸", ChineseNumberUtility.formatTenths(9.0F, "寸", "分"));
        assertEquals("八寸五分", ChineseNumberUtility.formatTenths(8.5F, "寸", "分"));
        assertEquals("一千二百三十四万五千六百七十八", ChineseNumberUtility.toChineseInteger(12_345_678L));
        assertEquals("一亿零一万零一", ChineseNumberUtility.toChineseInteger(100_010_001L));
        assertEquals("二十五点五", ChineseNumberUtility.toChineseDecimal(25.5D, 1));
        String maximumRequirement = ChineseNumberUtility.toChineseInteger(
                CultivationUtility.getRequiredSpiritualEnergy(
                        new CultivationStage(CultivationRealm.IMMORTAL, CultivationStage.MAXIMUM_LAYER)
                )
        );
        assertFalse(maximumRequirement.matches(".*[0-9].*"));
    }
}
