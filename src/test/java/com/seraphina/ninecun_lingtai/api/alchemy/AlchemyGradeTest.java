package com.seraphina.ninecun_lingtai.api.alchemy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlchemyGradeTest {
    @Test
    void exposesAllEightyOneGradeVariantsInOrder() {
        List<AlchemyGrade> grades = AlchemyGrade.getAllGrades();

        assertEquals(81, grades.size());
        assertEquals(new AlchemyGrade(1, 1), grades.get(0));
        assertEquals(new AlchemyGrade(9, 9), grades.get(80));
        assertEquals(81L, grades.stream().distinct().count());
    }

    @Test
    void gradePotencyIncreasesAcrossTheFullGradeRange() {
        assertEquals(1.0D, AlchemyGrade.LOWEST.getEffectMultiplier());
        assertEquals(5.0D, new AlchemyGrade(9, 9).getEffectMultiplier());
        assertTrue(
                new AlchemyGrade(5, 5).getEffectMultiplier()
                        > new AlchemyGrade(5, 4).getEffectMultiplier()
        );
    }
}
