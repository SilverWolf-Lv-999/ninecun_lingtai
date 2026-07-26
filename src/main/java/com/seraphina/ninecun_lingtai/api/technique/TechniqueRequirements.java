package com.seraphina.ninecun_lingtai.api.technique;

import com.seraphina.ninecun_lingtai.api.aptitude.SoulStrength;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record TechniqueRequirements(
        Optional<CultivationStage> minimumCultivation,
        int minimumSoulStrength,
        Set<ElementType> requiredElements,
        Set<ResourceLocation> requiredTechniques
) {
    public TechniqueRequirements {
        minimumCultivation = Objects.requireNonNull(minimumCultivation, "minimumCultivation");
        requiredElements = Set.copyOf(requiredElements);
        requiredTechniques = Set.copyOf(requiredTechniques);
        if (minimumSoulStrength < 0 || minimumSoulStrength > SoulStrength.MAXIMUM_ILLUMINATED_STARS) {
            throw new IllegalArgumentException("Minimum soul strength must be between zero and seven stars");
        }
    }

    public static TechniqueRequirements none() {
        return new TechniqueRequirements(Optional.empty(), 0, Set.of(), Set.of());
    }
}
