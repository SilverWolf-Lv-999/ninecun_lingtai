package net.seraphina.ninecun_lingtai.common.debug;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record DaoScrollTechniqueData(
        ResourceLocation id,
        String name,
        boolean learned,
        int comprehensionScore,
        long proficiency,
        boolean imprinted
) {
    public DaoScrollTechniqueData {
        Objects.requireNonNull(id, "id");
        name = Objects.requireNonNullElse(name, id.toString());
        comprehensionScore = Math.max(0, Math.min(100, comprehensionScore));
        proficiency = Math.max(0L, proficiency);
    }
}
