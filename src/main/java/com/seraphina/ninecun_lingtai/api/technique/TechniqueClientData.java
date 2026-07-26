package com.seraphina.ninecun_lingtai.api.technique;

import com.seraphina.ninecun_lingtai.api.spell.SpellCategory;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Optional;

public record TechniqueClientData(
        ResourceLocation id,
        String name,
        String description,
        String content,
        double breathingAbsorptionMultiplier,
        int spellGrade,
        String spellCategory
) {
    /** Multiplier value marking a technique that is not a breathing technique. */
    public static final double NOT_BREATHING = 0.0D;
    /** Grade value marking a technique that is not a castable spell. */
    public static final int NOT_SPELL = 0;
    /** Category value marking a technique that is not a castable spell. */
    public static final String NO_CATEGORY = "";

    public TechniqueClientData {
        Objects.requireNonNull(id, "id");
        name = requireText(name, "name");
        description = Objects.requireNonNull(description, "description");
        content = requireText(content, "content");
        if (!Double.isFinite(breathingAbsorptionMultiplier) || breathingAbsorptionMultiplier < 0.0D) {
            breathingAbsorptionMultiplier = NOT_BREATHING;
        }
        if (spellGrade < 0) {
            spellGrade = NOT_SPELL;
        }
        spellCategory = Objects.requireNonNullElse(spellCategory, NO_CATEGORY);
    }

    public Optional<SpellCategory> getSpellCategory() {
        return SpellCategory.fromSerializedName(this.spellCategory);
    }

    public boolean isBreathingTechnique() {
        return this.breathingAbsorptionMultiplier > NOT_BREATHING;
    }

    public boolean isSpellTechnique() {
        return this.spellGrade > NOT_SPELL;
    }

    public static TechniqueClientData fromDefinition(TechniqueDefinition technique) {
        return new TechniqueClientData(
                technique.id(),
                technique.name(),
                technique.description(),
                technique.content(),
                technique.breathing()
                        .map(TechniqueBreathing::absorptionMultiplier)
                        .orElse(NOT_BREATHING),
                technique.spell()
                        .map(spell -> spell.grade().getRank())
                        .orElse(NOT_SPELL),
                technique.spell()
                        .map(spell -> spell.category().getSerializedName())
                        .orElse(NO_CATEGORY)
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
