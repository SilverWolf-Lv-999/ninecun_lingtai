package com.seraphina.ninecun_lingtai.common.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeDefinition;
import com.seraphina.ninecun_lingtai.utility.AlchemyRecipeJsonUtility;
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

public final class AlchemyRecipeManager extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final String DIRECTORY = "alchemy_recipes";

    public static final AlchemyRecipeManager INSTANCE = new AlchemyRecipeManager();

    private volatile Map<ResourceLocation, AlchemyRecipeDefinition> recipes = Map.of();
    private volatile Map<ResourceLocation, AlchemyRecipeDefinition> recipesByOutputItem = Map.of();

    private AlchemyRecipeManager() {
        super(GSON, DIRECTORY);
    }

    public Optional<AlchemyRecipeDefinition> getRecipe(ResourceLocation recipeId) {
        return Optional.ofNullable(this.recipes.get(recipeId));
    }

    public List<AlchemyRecipeDefinition> getRecipes() {
        return this.recipes.values().stream()
                .sorted(Comparator.comparing(recipe -> recipe.id().toString()))
                .toList();
    }

    public Optional<AlchemyRecipeDefinition> getRecipeForOutputItem(ResourceLocation outputItem) {
        return Optional.ofNullable(this.recipesByOutputItem.get(outputItem));
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> jsonElements,
            ResourceManager resourceManager,
            ProfilerFiller profilerFiller
    ) {
        Map<ResourceLocation, AlchemyRecipeDefinition> loadedRecipes = new LinkedHashMap<>();
        jsonElements.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> loadRecipe(entry.getKey(), entry.getValue(), loadedRecipes));
        this.recipes = Map.copyOf(loadedRecipes);
        this.recipesByOutputItem = createOutputItemIndex(loadedRecipes);
        LOGGER.info("Loaded {} alchemy recipes", this.recipes.size());
    }

    private static void loadRecipe(
            ResourceLocation recipeId,
            JsonElement jsonElement,
            Map<ResourceLocation, AlchemyRecipeDefinition> loadedRecipes
    ) {
        try {
            loadedRecipes.put(recipeId, AlchemyRecipeJsonUtility.parse(recipeId, jsonElement));
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to load alchemy recipe {}", recipeId, exception);
        }
    }

    private static Map<ResourceLocation, AlchemyRecipeDefinition> createOutputItemIndex(
            Map<ResourceLocation, AlchemyRecipeDefinition> loadedRecipes
    ) {
        Map<ResourceLocation, AlchemyRecipeDefinition> outputItemRecipes = new LinkedHashMap<>();
        for (AlchemyRecipeDefinition recipe : loadedRecipes.values()) {
            AlchemyRecipeDefinition conflictingRecipe = outputItemRecipes.putIfAbsent(
                    recipe.outputItem(),
                    recipe
            );
            if (conflictingRecipe != null) {
                LOGGER.error(
                        "Ignoring alchemy recipe {} because {} already defines output item {}",
                        recipe.id(),
                        conflictingRecipe.id(),
                        recipe.outputItem()
                );
            }
        }
        return Map.copyOf(outputItemRecipes);
    }
}
