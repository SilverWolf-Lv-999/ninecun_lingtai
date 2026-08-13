package net.seraphina.ninecun_lingtai.api.alchemy;

import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public record AlchemyRecipeClientData(
        ResourceLocation id,
        String name,
        String description,
        String content,
        AlchemyGrade grade,
        Set<CultivationRealm> allowedRealms,
        @Nullable ElementType primaryElement,
        List<HeatStage> heatStages,
        List<AlchemyFeedStep> feedPlan,
        int brewingDuration,
        double pillToxin
) {
    public AlchemyRecipeClientData {
        Objects.requireNonNull(id, "id");
        name = requireText(name, "name");
        description = Objects.requireNonNull(description, "description");
        content = requireText(content, "content");
        Objects.requireNonNull(grade, "grade");
        allowedRealms = Set.copyOf(allowedRealms);
        if (allowedRealms.isEmpty()) {
            throw new IllegalArgumentException("Alchemy recipe must allow at least one cultivation realm");
        }
        heatStages = List.copyOf(heatStages);
        feedPlan = List.copyOf(feedPlan);
    }

    public Optional<ElementType> getPrimaryElement() {
        return Optional.ofNullable(this.primaryElement);
    }

    public String getAllowedRealmNames() {
        return this.allowedRealms.stream()
                .sorted()
                .map(CultivationRealm::getChineseName)
                .collect(Collectors.joining("、"));
    }

    public static AlchemyRecipeClientData fromDefinition(AlchemyRecipeDefinition recipe) {
        return new AlchemyRecipeClientData(
                recipe.id(),
                recipe.name(),
                recipe.description(),
                recipe.content(),
                recipe.grade(),
                recipe.allowedRealms(),
                recipe.primaryElement(),
                recipe.heatStages(),
                recipe.feedPlan(),
                recipe.brewingDuration(),
                recipe.pillToxin()
        );
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
