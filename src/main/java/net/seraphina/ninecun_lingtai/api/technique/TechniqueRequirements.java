package net.seraphina.ninecun_lingtai.api.technique;

import net.seraphina.ninecun_lingtai.api.aptitude.SoulStrength;
import net.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record TechniqueRequirements(
        Optional<CultivationStage> minimumCultivation,
        int minimumSoulStrength,
        Set<ElementType> requiredElements,
        Set<ResourceLocation> requiredTechniques,
        int minimumSpiritualRootElements,
        int maximumSpiritualRootElements
) {
    public TechniqueRequirements {
        minimumCultivation = Objects.requireNonNull(minimumCultivation, "minimumCultivation");
        requiredElements = Set.copyOf(requiredElements);
        requiredTechniques = Set.copyOf(requiredTechniques);
        if (minimumSoulStrength < 0 || minimumSoulStrength > SoulStrength.MAXIMUM_ILLUMINATED_STARS) {
            throw new IllegalArgumentException("Minimum soul strength must be between zero and seven stars");
        }
        int maximumSelectableElements = SpiritualRoot.SELECTABLE_ELEMENTS.size();
        if (minimumSpiritualRootElements < 0 || minimumSpiritualRootElements > maximumSelectableElements) {
            throw new IllegalArgumentException("Minimum spiritual root elements are out of range");
        }
        if (maximumSpiritualRootElements < 0 || maximumSpiritualRootElements > maximumSelectableElements) {
            throw new IllegalArgumentException("Maximum spiritual root elements are out of range");
        }
        if (maximumSpiritualRootElements > 0
                && minimumSpiritualRootElements > maximumSpiritualRootElements) {
            throw new IllegalArgumentException("Minimum spiritual root elements exceed the maximum");
        }
    }

    public static TechniqueRequirements none() {
        return new TechniqueRequirements(Optional.empty(), 0, Set.of(), Set.of(), 0, 0);
    }
}
