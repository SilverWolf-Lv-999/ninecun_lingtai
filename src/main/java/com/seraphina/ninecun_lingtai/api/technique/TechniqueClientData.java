package com.seraphina.ninecun_lingtai.api.technique;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record TechniqueClientData(
        ResourceLocation id,
        String name,
        String description,
        String content,
        double breathingAbsorptionMultiplier
) {
    /** Multiplier value marking a technique that is not a breathing technique. */
    public static final double NOT_BREATHING = 0.0D;

    public TechniqueClientData {
        Objects.requireNonNull(id, "id");
        name = requireText(name, "name");
        description = Objects.requireNonNull(description, "description");
        content = requireText(content, "content");
        if (!Double.isFinite(breathingAbsorptionMultiplier) || breathingAbsorptionMultiplier < 0.0D) {
            breathingAbsorptionMultiplier = NOT_BREATHING;
        }
    }

    public boolean isBreathingTechnique() {
        return this.breathingAbsorptionMultiplier > NOT_BREATHING;
    }

    public static TechniqueClientData fromDefinition(TechniqueDefinition technique) {
        return new TechniqueClientData(
                technique.id(),
                technique.name(),
                technique.description(),
                technique.content(),
                technique.breathing()
                        .map(TechniqueBreathing::absorptionMultiplier)
                        .orElse(NOT_BREATHING)
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
