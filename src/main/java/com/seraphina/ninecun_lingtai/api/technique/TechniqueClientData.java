package com.seraphina.ninecun_lingtai.api.technique;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record TechniqueClientData(
        ResourceLocation id,
        String name,
        String description,
        String content
) {
    public TechniqueClientData {
        Objects.requireNonNull(id, "id");
        name = requireText(name, "name");
        description = Objects.requireNonNull(description, "description");
        content = requireText(content, "content");
    }

    public static TechniqueClientData fromDefinition(TechniqueDefinition technique) {
        return new TechniqueClientData(
                technique.id(),
                technique.name(),
                technique.description(),
                technique.content()
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
