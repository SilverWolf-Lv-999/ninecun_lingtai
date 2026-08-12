package net.seraphina.ninecun_lingtai.common.player;

import net.seraphina.ninecun_lingtai.ModMain;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;

public final class PlayerLearnedAlchemyRecipeData {
    private static final String LEARNED_RECIPES_KEY = ModMain.MOD_ID + ":learned_alchemy_recipes";

    private PlayerLearnedAlchemyRecipeData() {
    }

    public static Set<ResourceLocation> getRecipes(Player player) {
        ListTag recipeTags = player.getPersistentData().getList(
                LEARNED_RECIPES_KEY,
                Tag.TAG_STRING
        );
        Set<ResourceLocation> recipes = new HashSet<>();
        for (Tag recipeTag : recipeTags) {
            ResourceLocation recipeId = ResourceLocation.tryParse(recipeTag.getAsString());
            if (recipeId != null) {
                recipes.add(recipeId);
            }
        }
        return Set.copyOf(recipes);
    }

    public static boolean hasRecipe(Player player, ResourceLocation recipeId) {
        return getRecipes(player).contains(recipeId);
    }

    public static boolean learn(Player player, ResourceLocation recipeId) {
        Set<ResourceLocation> recipes = new HashSet<>(getRecipes(player));
        boolean added = recipes.add(recipeId);
        saveRecipes(player, recipes);
        return added;
    }

    public static void clear(Player player) {
        player.getPersistentData().remove(LEARNED_RECIPES_KEY);
    }

    public static void copy(Player sourcePlayer, Player targetPlayer) {
        if (sourcePlayer.getPersistentData().contains(LEARNED_RECIPES_KEY, Tag.TAG_LIST)) {
            targetPlayer.getPersistentData().put(
                    LEARNED_RECIPES_KEY,
                    sourcePlayer.getPersistentData().getList(
                            LEARNED_RECIPES_KEY,
                            Tag.TAG_STRING
                    ).copy()
            );
        }
    }

    private static void saveRecipes(Player player, Set<ResourceLocation> recipes) {
        ListTag recipeTags = new ListTag();
        recipes.stream()
                .sorted()
                .map(ResourceLocation::toString)
                .map(StringTag::valueOf)
                .forEach(recipeTags::add);
        player.getPersistentData().put(LEARNED_RECIPES_KEY, recipeTags);
    }
}
