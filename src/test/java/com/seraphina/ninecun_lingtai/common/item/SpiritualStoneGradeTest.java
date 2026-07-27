package com.seraphina.ninecun_lingtai.common.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpiritualStoneGradeTest {
    @Test
    void definesRequestedGradeCapacities() {
        assertEquals(400.0D, SpiritualStoneGrade.HUMAN.getEnergyCapacity());
        assertEquals(5200.0D, SpiritualStoneGrade.EARTH.getEnergyCapacity());
        assertEquals(10000.0D, SpiritualStoneGrade.HEAVEN.getEnergyCapacity());
    }

    @Test
    void higherGradesAbsorbFaster() {
        assertTrue(SpiritualStoneGrade.HUMAN.getAbsorptionMultiplier()
                < SpiritualStoneGrade.EARTH.getAbsorptionMultiplier());
        assertTrue(SpiritualStoneGrade.EARTH.getAbsorptionMultiplier()
                < SpiritualStoneGrade.HEAVEN.getAbsorptionMultiplier());
    }
}
