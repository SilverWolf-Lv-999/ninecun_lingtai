package com.seraphina.ninecun_lingtai.client.player;

import com.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeClientData;
import com.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeJeiData;
import com.seraphina.ninecun_lingtai.client.compat.jei.AlchemyRecipeJeiRuntimeManager;
import com.seraphina.ninecun_lingtai.client.screen.AlchemyRecipeReadingScreen;
import com.seraphina.ninecun_lingtai.client.utility.ClientCreativeTabUtility;
import com.seraphina.ninecun_lingtai.common.item.AlchemyRecipeCreativeTabCache;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.network.packet.RequestAlchemyRecipeLibraryPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class ClientAlchemyRecipeManager {
    private static Map<ResourceLocation, AlchemyRecipeClientData> recipes = Map.of();
    private static Set<ResourceLocation> learnedRecipeIds = Set.of();
    private static Map<ResourceLocation, AlchemyRecipeJeiData> learnedJeiRecipes = Map.of();
    private static boolean initialized;
    private static int revision;

    private ClientAlchemyRecipeManager() {
    }

    public static void beginOpenLibrary() {
        requestRefresh();
    }

    public static void requestRefresh() {
        ModNetwork.sendToServer(RequestAlchemyRecipeLibraryPacket.INSTANCE);
    }

    public static void acceptRecipes(
            List<AlchemyRecipeClientData> updatedRecipes,
            Set<ResourceLocation> updatedLearnedRecipeIds,
            List<AlchemyRecipeJeiData> updatedLearnedJeiRecipes
    ) {
        Map<ResourceLocation, AlchemyRecipeClientData> recipeMap = new LinkedHashMap<>();
        updatedRecipes.stream()
                .sorted(Comparator.comparing(recipe -> recipe.id().toString()))
                .forEach(recipe -> recipeMap.put(recipe.id(), recipe));
        recipes = Map.copyOf(recipeMap);
        learnedRecipeIds = Set.copyOf(updatedLearnedRecipeIds);
        Map<ResourceLocation, AlchemyRecipeJeiData> jeiRecipeMap = new LinkedHashMap<>();
        updatedLearnedJeiRecipes.stream()
                .filter(recipe -> learnedRecipeIds.contains(recipe.id()))
                .sorted(Comparator.comparing(recipe -> recipe.id().toString()))
                .forEach(recipe -> jeiRecipeMap.put(recipe.id(), recipe));
        learnedJeiRecipes = Map.copyOf(jeiRecipeMap);
        initialized = true;
        revision++;
        AlchemyRecipeCreativeTabCache.setRecipes(updatedRecipes);
        ClientCreativeTabUtility.rebuildCreativeTabs();
        synchronizeJeiRecipes();
    }

    public static void openRecipe(AlchemyRecipeClientData recipe, boolean learned) {
        Map<ResourceLocation, AlchemyRecipeClientData> recipeMap = new LinkedHashMap<>(recipes);
        recipeMap.put(recipe.id(), recipe);
        recipes = Map.copyOf(recipeMap);
        if (learned) {
            HashSet<ResourceLocation> updatedLearnedRecipes = new HashSet<>(learnedRecipeIds);
            updatedLearnedRecipes.add(recipe.id());
            learnedRecipeIds = Set.copyOf(updatedLearnedRecipes);
        }
        Minecraft.getInstance().setScreen(new AlchemyRecipeReadingScreen(recipe, null, true));
    }

    public static Optional<AlchemyRecipeClientData> getRecipe(ResourceLocation recipeId) {
        return Optional.ofNullable(recipes.get(recipeId));
    }

    public static List<AlchemyRecipeClientData> getLearnedRecipes() {
        return learnedRecipeIds.stream()
                .map(recipes::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(AlchemyRecipeClientData::name)
                        .thenComparing(recipe -> recipe.id().toString()))
                .toList();
    }

    public static boolean isLearned(ResourceLocation recipeId) {
        return learnedRecipeIds.contains(recipeId);
    }

    public static List<AlchemyRecipeJeiData> getLearnedJeiRecipes() {
        return learnedJeiRecipes.values().stream()
                .sorted(Comparator.comparing(recipe -> recipe.id().toString()))
                .toList();
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static int getRevision() {
        return revision;
    }

    public static void reset() {
        recipes = Map.of();
        learnedRecipeIds = Set.of();
        learnedJeiRecipes = Map.of();
        initialized = false;
        revision++;
        AlchemyRecipeCreativeTabCache.clear();
        synchronizeJeiRecipes();
    }

    private static void synchronizeJeiRecipes() {
        if (ModList.get().isLoaded("jei")) {
            AlchemyRecipeJeiRuntimeManager.synchronizeRecipes(getLearnedJeiRecipes());
        }
    }
}
