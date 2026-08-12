package net.seraphina.ninecun_lingtai.api.technique;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record TechniqueProgress(int comprehensionScore, ResourceLocation mastery, String feedback) {
    public TechniqueProgress {
        if (comprehensionScore < TechniqueComprehension.MINIMUM_SCORE
                || comprehensionScore > TechniqueComprehension.MAXIMUM_SCORE) {
            throw new IllegalArgumentException("Technique comprehension score must be between zero and one hundred");
        }
        Objects.requireNonNull(mastery, "mastery");
        feedback = Objects.requireNonNull(feedback, "feedback");
    }
}
