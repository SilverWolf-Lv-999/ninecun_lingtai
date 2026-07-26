package com.seraphina.ninecun_lingtai.api.technique;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record TechniqueEffect(ResourceLocation type, JsonObject parameters) {
    public TechniqueEffect {
        Objects.requireNonNull(type, "type");
        parameters = Objects.requireNonNull(parameters, "parameters").deepCopy();
    }

    @Override
    public JsonObject parameters() {
        return this.parameters.deepCopy();
    }
}
