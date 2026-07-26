package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.api.element.FiveElementValues;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpiritualEnergyUtilityTest {
    private static final FiveElementValues UNEVEN_ENVIRONMENT = new FiveElementValues(
            1.0F,
            2.0F,
            3.0F,
            4.0F,
            5.0F
    );

    @Test
    void ordinaryRootOnlyAbsorbsItsMatchingFiveElementEnergy() {
        SpiritualRoot spiritualRoot = new SpiritualRoot(Set.of(ElementType.WATER), 9.0F);

        assertEquals(
                Map.of(ElementType.WATER, 3.0D),
                SpiritualEnergyUtility.createCultivationAbsorptionRates(
                        spiritualRoot,
                        UNEVEN_ENVIRONMENT
                )
        );
    }

    @Test
    void yinYangRootOnlyConvertsTheBalancedFiveElementShare() {
        SpiritualRoot spiritualRoot = new SpiritualRoot(Set.of(ElementType.YIN_YANG), 9.0F);

        assertEquals(
                Map.of(ElementType.YIN_YANG, 5.0D),
                SpiritualEnergyUtility.createCultivationAbsorptionRates(
                        spiritualRoot,
                        UNEVEN_ENVIRONMENT
                )
        );
    }

    @Test
    void yinYangAndOrdinaryRootsKeepIndependentEnergyPools() {
        SpiritualRoot spiritualRoot = new SpiritualRoot(
                Set.of(ElementType.METAL, ElementType.YIN_YANG),
                9.0F
        );

        assertEquals(
                Map.of(
                        ElementType.METAL, 1.0D,
                        ElementType.YIN_YANG, 5.0D
                ),
                SpiritualEnergyUtility.createCultivationAbsorptionRates(
                        spiritualRoot,
                        UNEVEN_ENVIRONMENT
                )
        );
    }

    @Test
    void chaosRootCombinesAllFiveElementEnergyIntoChaosEnergy() {
        SpiritualRoot spiritualRoot = new SpiritualRoot(SpiritualRoot.SELECTABLE_ELEMENTS, 9.0F);

        assertEquals(
                Map.of(ElementType.CHAOS, 15.0D),
                SpiritualEnergyUtility.createCultivationAbsorptionRates(
                        spiritualRoot,
                        UNEVEN_ENVIRONMENT
                )
        );
    }
}
