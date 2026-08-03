package com.seraphina.ninecun_lingtai.api.alchemy;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record AlchemyIngredient(ResourceLocation item, int count) {
    public AlchemyIngredient {
        Objects.requireNonNull(item, "item");
        if (count < 1 || count > 64) {
            throw new IllegalArgumentException("Alchemy ingredient count must be between one and 64");
        }
    }
}
