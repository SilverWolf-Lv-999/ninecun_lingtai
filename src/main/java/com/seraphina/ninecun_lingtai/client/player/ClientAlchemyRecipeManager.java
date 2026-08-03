package com.seraphina.ninecun_lingtai.client.player;

import com.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeClientData;
import com.seraphina.ninecun_lingtai.client.screen.AlchemyRecipeReadingScreen;
import com.seraphina.ninecun_lingtai.client.utility.ClientCreativeTabUtility;
import com.seraphina.ninecun_lingtai.common.item.AlchemyRecipeCreativeTabCache;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ClientAlchemyRecipeManager {
    private static Map<ResourceLocation, AlchemyRecipeClientData> recipes = Map.of();

    private ClientAlchemyRecipeManager() {
    }

    public static void acceptRecipes(List<AlchemyRecipeClientData> updatedRecipes) {
        Map<ResourceLocation, AlchemyRecipeClientData> recipeMap = new LinkedHashMap<>();
        updatedRecipes.stream()
                .sorted(Comparator.comparing(recipe -> recipe.id().toString()))
                .forEach(recipe -> recipeMap.put(recipe.id(), recipe));
        recipes = Map.copyOf(recipeMap);
        AlchemyRecipeCreativeTabCache.setRecipes(updatedRecipes);
        ClientCreativeTabUtility.rebuildCreativeTabs();
    }

    public static void openRecipe(AlchemyRecipeClientData recipe) {
        Map<ResourceLocation, AlchemyRecipeClientData> recipeMap = new LinkedHashMap<>(recipes);
        recipeMap.put(recipe.id(), recipe);
        recipes = Map.copyOf(recipeMap);
        Minecraft.getInstance().setScreen(new AlchemyRecipeReadingScreen(recipe));
    }

    public static Optional<AlchemyRecipeClientData> getRecipe(ResourceLocation recipeId) {
        return Optional.ofNullable(recipes.get(recipeId));
    }

    public static void reset() {
        recipes = Map.of();
        AlchemyRecipeCreativeTabCache.clear();
    }
}
