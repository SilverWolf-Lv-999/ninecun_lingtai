package com.seraphina.ninecun_lingtai.common.item;

import com.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeClientData;
import com.seraphina.ninecun_lingtai.utility.AlchemyRecipeItemUtility;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class AlchemyRecipeCreativeTabCache {
    private static volatile List<ItemStack> itemStacks = List.of();

    public static void setRecipes(List<AlchemyRecipeClientData> recipes) {
        itemStacks = recipes.stream()
                .map(AlchemyRecipeItemUtility::createStack)
                .toList();
    }

    public static List<ItemStack> getItemStacks() {
        return itemStacks.stream().map(ItemStack::copy).toList();
    }

    public static void clear() {
        itemStacks = List.of();
    }
}
