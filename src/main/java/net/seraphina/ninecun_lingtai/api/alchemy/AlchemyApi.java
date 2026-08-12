package net.seraphina.ninecun_lingtai.api.alchemy;

import net.seraphina.ninecun_lingtai.common.data.AlchemyRecipeManager;
import net.seraphina.ninecun_lingtai.utility.AlchemyRecipeItemUtility;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public final class AlchemyApi {
    public static Optional<AlchemyRecipeDefinition> getRecipe(ResourceLocation recipeId) {
        return AlchemyRecipeManager.INSTANCE.getRecipe(recipeId);
    }

    public static List<AlchemyRecipeDefinition> getRecipes() {
        return AlchemyRecipeManager.INSTANCE.getRecipes();
    }

    public static Optional<AlchemyRecipeDefinition> getRecipeForOutputItem(ResourceLocation outputItem) {
        return AlchemyRecipeManager.INSTANCE.getRecipeForOutputItem(outputItem);
    }

    public static Optional<ItemStack> createRecipeItem(ResourceLocation recipeId) {
        return getRecipe(recipeId).map(AlchemyRecipeItemUtility::createStack);
    }
}
