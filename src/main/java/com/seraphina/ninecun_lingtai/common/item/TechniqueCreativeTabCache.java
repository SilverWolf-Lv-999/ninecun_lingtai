package com.seraphina.ninecun_lingtai.common.item;

import com.seraphina.ninecun_lingtai.api.technique.TechniqueClientData;
import com.seraphina.ninecun_lingtai.common.registry.ModItems;
import com.seraphina.ninecun_lingtai.utility.TechniqueItemUtility;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class TechniqueCreativeTabCache {
    private static volatile List<ItemStack> itemStacks = List.of();

    public static void setTechniques(List<TechniqueClientData> techniques) {
        itemStacks = techniques.stream()
                .map(TechniqueItemUtility::createStack)
                .toList();
    }

    public static List<ItemStack> getItemStacks() {
        List<ItemStack> displayedItemStacks = new ArrayList<>(itemStacks.size() + 1);
        displayedItemStacks.add(new ItemStack(ModItems.JADE_SLIP.get()));
        itemStacks.stream().map(ItemStack::copy).forEach(displayedItemStacks::add);
        return List.copyOf(displayedItemStacks);
    }

    public static void clear() {
        itemStacks = List.of();
    }
}
