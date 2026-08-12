package net.seraphina.ninecun_lingtai.api.technique;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Objects;

public record LearnedTechniqueClientData(
        ResourceLocation id,
        int comprehensionScore,
        long proficiency,
        ResourceLocation mastery
) {
    public LearnedTechniqueClientData {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(mastery, "mastery");
        comprehensionScore = Mth.clamp(
                comprehensionScore,
                TechniqueComprehension.MINIMUM_SCORE,
                TechniqueComprehension.MAXIMUM_SCORE
        );
        proficiency = Math.max(0L, proficiency);
    }
}
