package com.seraphina.ninecun_lingtai.common.item;

import com.seraphina.ninecun_lingtai.api.technique.TechniqueClientData;
import com.seraphina.ninecun_lingtai.utility.TechniqueItemUtility;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class TechniqueCreativeTabCache {
    private static volatile List<ItemStack> itemStacks = List.of();

    private TechniqueCreativeTabCache() {
    }

    public static void setTechniques(List<TechniqueClientData> techniques) {
        itemStacks = techniques.stream()
                .map(TechniqueItemUtility::createStack)
                .toList();
    }

    public static List<ItemStack> getItemStacks() {
        return itemStacks.stream().map(ItemStack::copy).toList();
    }

    public static void clear() {
        itemStacks = List.of();
    }
}
