package net.seraphina.ninecun_lingtai.client.compat.jei;

import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeJeiData;
import net.seraphina.ninecun_lingtai.client.player.ClientAlchemyRecipeManager;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AlchemyRecipeJeiRuntimeManager {
    private static final Map<ResourceLocation, AlchemyRecipeJeiData> registeredRecipes =
            new LinkedHashMap<>();

    @Nullable
    private static IJeiRuntime runtime;

    private AlchemyRecipeJeiRuntimeManager() {
    }

    public static void setInitialRecipes(List<AlchemyRecipeJeiData> recipes) {
        registeredRecipes.clear();
        registeredRecipes.putAll(createRecipeMap(recipes));
    }

    public static void onRuntimeAvailable(IJeiRuntime updatedRuntime) {
        runtime = updatedRuntime;
        synchronizeRecipes(ClientAlchemyRecipeManager.getLearnedJeiRecipes());
    }

    public static void onRuntimeUnavailable() {
        runtime = null;
        registeredRecipes.clear();
    }

    public static void synchronizeRecipes(List<AlchemyRecipeJeiData> recipes) {
        if (runtime == null) {
            return;
        }

        Map<ResourceLocation, AlchemyRecipeJeiData> updatedRecipes = createRecipeMap(recipes);
        IRecipeManager recipeManager = runtime.getRecipeManager();
        List<AlchemyRecipeJeiData> removedRecipes = registeredRecipes.entrySet().stream()
                .filter(entry -> !updatedRecipes.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();
        if (!removedRecipes.isEmpty()) {
            recipeManager.hideRecipes(AlchemyRecipeJeiPlugin.RECIPE_TYPE, removedRecipes);
        }

        for (Map.Entry<ResourceLocation, AlchemyRecipeJeiData> entry : updatedRecipes.entrySet()) {
            AlchemyRecipeJeiData registeredRecipe = registeredRecipes.get(entry.getKey());
            if (registeredRecipe == null) {
                recipeManager.addRecipes(AlchemyRecipeJeiPlugin.RECIPE_TYPE, List.of(entry.getValue()));
                continue;
            }
            if (registeredRecipe.equals(entry.getValue())) {
                recipeManager.unhideRecipes(AlchemyRecipeJeiPlugin.RECIPE_TYPE, List.of(registeredRecipe));
                continue;
            }
            recipeManager.hideRecipes(AlchemyRecipeJeiPlugin.RECIPE_TYPE, List.of(registeredRecipe));
            recipeManager.addRecipes(AlchemyRecipeJeiPlugin.RECIPE_TYPE, List.of(entry.getValue()));
        }

        registeredRecipes.clear();
        registeredRecipes.putAll(updatedRecipes);
    }

    private static Map<ResourceLocation, AlchemyRecipeJeiData> createRecipeMap(
            List<AlchemyRecipeJeiData> recipes
    ) {
        Map<ResourceLocation, AlchemyRecipeJeiData> recipeMap = new LinkedHashMap<>();
        recipes.stream()
                .sorted(Comparator.comparing(recipe -> recipe.id().toString()))
                .forEach(recipe -> recipeMap.put(recipe.id(), recipe));
        return recipeMap;
    }
}
