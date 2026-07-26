package com.seraphina.ninecun_lingtai.api.technique;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record TechniqueMasteryThreshold(ResourceLocation mastery, int minimumScore) {
    public TechniqueMasteryThreshold {
        Objects.requireNonNull(mastery, "mastery");
        if (minimumScore < 0 || minimumScore > TechniqueComprehension.MAXIMUM_SCORE) {
            throw new IllegalArgumentException("Technique mastery score must be between zero and one hundred");
        }
    }
}
