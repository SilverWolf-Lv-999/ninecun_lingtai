package net.seraphina.ninecun_lingtai.api.alchemy;

import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record AlchemyRecipeClientData(
        ResourceLocation id,
        String name,
        String description,
        String content,
        AlchemyGrade grade,
        Set<CultivationRealm> allowedRealms
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
                recipe.allowedRealms()
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
