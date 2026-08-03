package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeClientData;
import com.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeDefinition;
import com.seraphina.ninecun_lingtai.common.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class AlchemyRecipeItemUtility {
    private static final String RECIPE_ID_KEY = ModMain.MOD_ID + ":alchemy_recipe_id";
    private static final String RECIPE_NAME_KEY = ModMain.MOD_ID + ":alchemy_recipe_name";
    private static final String RECIPE_DESCRIPTION_KEY = ModMain.MOD_ID + ":alchemy_recipe_description";

    private AlchemyRecipeItemUtility() {
    }

    public static ItemStack createStack(AlchemyRecipeDefinition recipe) {
        return createStack(recipe.id(), recipe.name(), recipe.description());
    }

    public static ItemStack createStack(AlchemyRecipeClientData recipe) {
        return createStack(recipe.id(), recipe.name(), recipe.description());
    }

    public static Optional<ResourceLocation> getRecipeId(ItemStack itemStack) {
        if (!itemStack.is(ModItems.ALCHEMY_RECIPE.get()) || !itemStack.hasTag()) {
            return Optional.empty();
        }
        return Optional.ofNullable(ResourceLocation.tryParse(
                itemStack.getTag().getString(RECIPE_ID_KEY)
        ));
    }

    public static Optional<String> getRecipeName(ItemStack itemStack) {
        return getText(itemStack, RECIPE_NAME_KEY);
    }

    public static Optional<String> getRecipeDescription(ItemStack itemStack) {
        return getText(itemStack, RECIPE_DESCRIPTION_KEY);
    }

    private static ItemStack createStack(
            ResourceLocation recipeId,
            String name,
            String description
    ) {
        ItemStack itemStack = new ItemStack(ModItems.ALCHEMY_RECIPE.get());
        CompoundTag itemData = itemStack.getOrCreateTag();
        itemData.putString(RECIPE_ID_KEY, recipeId.toString());
        itemData.putString(RECIPE_NAME_KEY, name);
        itemData.putString(RECIPE_DESCRIPTION_KEY, description);
        return itemStack;
    }

    private static Optional<String> getText(ItemStack itemStack, String key) {
        if (!itemStack.is(ModItems.ALCHEMY_RECIPE.get()) || !itemStack.hasTag()) {
            return Optional.empty();
        }
        String value = itemStack.getTag().getString(key);
        return value.isBlank() ? Optional.empty() : Optional.of(value);
    }
}
