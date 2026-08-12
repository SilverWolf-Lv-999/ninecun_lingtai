package net.seraphina.ninecun_lingtai.api.alchemy;

import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record AlchemyRecipeDefinition(
        ResourceLocation id,
        String name,
        String description,
        String content,
        AlchemyGrade grade,
        Set<CultivationRealm> allowedRealms,
        CultivationRealm minimumRefiningRealm,
        AlchemyIngredient primaryIngredient,
        List<AlchemyIngredient> secondaryIngredients,
        List<AlchemyIngredient> auxiliaryIngredients,
        ResourceLocation outputItem,
        int outputCount,
        int brewingDuration,
        AlchemyPillEffect pillEffect
) {
    public AlchemyRecipeDefinition {
        Objects.requireNonNull(id, "id");
        name = requireText(name, "name");
        description = Objects.requireNonNull(description, "description");
        content = requireText(content, "content");
        Objects.requireNonNull(grade, "grade");
        allowedRealms = Set.copyOf(allowedRealms);
        if (allowedRealms.isEmpty()) {
            throw new IllegalArgumentException("Alchemy recipe must allow at least one cultivation realm");
        }
        Objects.requireNonNull(minimumRefiningRealm, "minimumRefiningRealm");
        Objects.requireNonNull(primaryIngredient, "primaryIngredient");
        secondaryIngredients = List.copyOf(secondaryIngredients);
        if (secondaryIngredients.size() != 2) {
            throw new IllegalArgumentException("Alchemy recipe must define exactly two secondary ingredients");
        }
        auxiliaryIngredients = List.copyOf(auxiliaryIngredients);
        if (auxiliaryIngredients.size() != 2) {
            throw new IllegalArgumentException("Alchemy recipe must define exactly two auxiliary ingredients");
        }
        Objects.requireNonNull(outputItem, "outputItem");
        if (outputCount < 1 || outputCount > 64) {
            throw new IllegalArgumentException("Alchemy output count must be between one and 64");
        }
        if (brewingDuration < 20 || brewingDuration > 72000) {
            throw new IllegalArgumentException("Alchemy brewing duration must be between 20 and 72000 ticks");
        }
        Objects.requireNonNull(pillEffect, "pillEffect");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    public String getAllowedRealmNames() {
        return this.allowedRealms.stream()
                .sorted()
                .map(CultivationRealm::getChineseName)
                .collect(Collectors.joining("、"));
    }

    public String getMinimumRefiningRealmName() {
        return this.minimumRefiningRealm.getChineseName();
    }
}
