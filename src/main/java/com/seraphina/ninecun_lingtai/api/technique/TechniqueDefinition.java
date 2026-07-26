package com.seraphina.ninecun_lingtai.api.technique;

import com.seraphina.ninecun_lingtai.api.element.ElementType;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record TechniqueDefinition(
        ResourceLocation id,
        String name,
        String description,
        String content,
        ResourceLocation cultivationPath,
        Set<ElementType> compatibleElements,
        TechniqueRequirements requirements,
        TechniqueComprehension comprehension,
        List<TechniqueEffect> effects,
        Optional<TechniqueBreathing> breathing,
        Optional<TechniqueSpell> spell
) {
    public TechniqueDefinition {
        Objects.requireNonNull(id, "id");
        name = requireText(name, "name");
        description = Objects.requireNonNull(description, "description");
        content = requireText(content, "content");
        Objects.requireNonNull(cultivationPath, "cultivationPath");
        compatibleElements = Set.copyOf(compatibleElements);
        Objects.requireNonNull(requirements, "requirements");
        Objects.requireNonNull(comprehension, "comprehension");
        effects = List.copyOf(effects);
        Objects.requireNonNull(breathing, "breathing");
        Objects.requireNonNull(spell, "spell");
        if (breathing.isPresent() && spell.isPresent()) {
            throw new IllegalArgumentException(
                    "A technique cannot be both a breathing technique and a spell"
            );
        }
    }

    public boolean isBreathingTechnique() {
        return this.breathing.isPresent();
    }

    public boolean isSpellTechnique() {
        return this.spell.isPresent();
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
