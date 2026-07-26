package com.seraphina.ninecun_lingtai.common.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueDefinition;
import com.seraphina.ninecun_lingtai.utility.TechniqueJsonUtility;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class TechniqueManager extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final String DIRECTORY = "techniques";

    public static final TechniqueManager INSTANCE = new TechniqueManager();

    private volatile Map<ResourceLocation, TechniqueDefinition> techniques = Map.of();

    private TechniqueManager() {
        super(GSON, DIRECTORY);
    }

    public Optional<TechniqueDefinition> getTechnique(ResourceLocation techniqueId) {
        return Optional.ofNullable(this.techniques.get(techniqueId));
    }

    public List<TechniqueDefinition> getTechniques() {
        return this.techniques.values().stream()
                .sorted(Comparator.comparing(technique -> technique.id().toString()))
                .toList();
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> jsonElements,
            ResourceManager resourceManager,
            ProfilerFiller profilerFiller
    ) {
        Map<ResourceLocation, TechniqueDefinition> loadedTechniques = new LinkedHashMap<>();
        jsonElements.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> loadTechnique(entry.getKey(), entry.getValue(), loadedTechniques));
        this.techniques = Map.copyOf(loadedTechniques);
        LOGGER.info("Loaded {} cultivation techniques", this.techniques.size());
    }

    private static void loadTechnique(
            ResourceLocation techniqueId,
            JsonElement jsonElement,
            Map<ResourceLocation, TechniqueDefinition> loadedTechniques
    ) {
        try {
            loadedTechniques.put(techniqueId, TechniqueJsonUtility.parse(techniqueId, jsonElement));
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to load cultivation technique {}", techniqueId, exception);
        }
    }
}
