package com.seraphina.ninecun_lingtai.api.technique;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TechniqueComprehensionTest {
    @Test
    void mapsScoreToHighestReachedMasteryAndPerfectScoreToMinorMastery() {
        ResourceLocation initial = ResourceLocation.fromNamespaceAndPath("test", "initial");
        ResourceLocation proficient = ResourceLocation.fromNamespaceAndPath("test", "proficient");
        ResourceLocation minorMastery = ResourceLocation.fromNamespaceAndPath("test", "minor_mastery");
        TechniqueComprehension comprehension = new TechniqueComprehension(
                60,
                minorMastery,
                List.of(),
                List.of(),
                List.of(
                        new TechniqueMasteryThreshold(proficient, 80),
                        new TechniqueMasteryThreshold(initial, 60)
                )
        );

        assertTrue(comprehension.getMastery(59).isEmpty());
        assertEquals(initial, comprehension.getMastery(60).orElseThrow());
        assertEquals(proficient, comprehension.getMastery(99).orElseThrow());
        assertEquals(minorMastery, comprehension.getMastery(100).orElseThrow());
    }
}
