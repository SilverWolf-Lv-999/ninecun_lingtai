package com.seraphina.ninecun_lingtai.client.utility;

import com.seraphina.ninecun_lingtai.common.item.AlchemyRecipeCreativeTabCache;
import com.seraphina.ninecun_lingtai.common.item.TechniqueCreativeTabCache;
import com.seraphina.ninecun_lingtai.common.registry.ModCreativeModeTabs;
import com.seraphina.ninecun_lingtai.common.registry.ModItems;
import com.seraphina.ninecun_lingtai.utility.AlchemyPillUtility;
import com.seraphina.ninecun_lingtai.utility.SpiritualItemUtility;
import com.seraphina.ninecun_lingtai.utility.SpiritualStoneUtility;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

public final class ComplexNbtCreativeTabUtility {
    private static final List<ComplexNbtItemProvider> COMPLEX_NBT_ITEM_PROVIDERS = List.of(
            new ComplexNbtItemProvider(
                    ModItems.HAN_SWORD::get,
                    () -> SpiritualItemUtility.getCreativeItemStacks(ModItems.HAN_SWORD.get())
            ),
            new ComplexNbtItemProvider(ModItems.SPIRITUAL_STONE::get, SpiritualStoneUtility::getCreativeItemStacks),
            new ComplexNbtItemProvider(ModItems.JADE_SLIP::get, TechniqueCreativeTabCache::getItemStacks),
            new ComplexNbtItemProvider(
                    ModItems.ALCHEMY_RECIPE::get,
                    AlchemyRecipeCreativeTabCache::getItemStacks
            )
    );

    public static boolean appendKnownComplexNbtStacks(
            CreativeModeTab creativeModeTab,
            NonNullList<ItemStack> displayedItemStacks
    ) {
        if (creativeModeTab != ModCreativeModeTabs.NINECUN_LINGTAI.get()) {
            return false;
        }

        boolean changed = false;
        for (ComplexNbtItemProvider provider : COMPLEX_NBT_ITEM_PROVIDERS) {
            changed |= appendComplexNbtStacksWhenBaseItemVisible(
                    displayedItemStacks,
                    provider.baseItem().get(),
                    provider.itemStacks().get()
            );
        }
        changed |= appendPillStacks(displayedItemStacks);
        if (changed) {
            displayedItemStacks.sort(Comparator.comparingInt(itemStack -> Item.getId(itemStack.getItem())));
        }
        return changed;
    }

    private static boolean appendPillStacks(NonNullList<ItemStack> displayedItemStacks) {
        boolean changed = false;
        for (Item pill : ModItems.getPillItems()) {
            changed |= appendComplexNbtStacksWhenBaseItemVisible(
                    displayedItemStacks,
                    pill,
                    AlchemyPillUtility.getCreativeItemStacks(pill)
            );
        }
        return changed;
    }

    public static boolean appendComplexNbtStacksWhenBaseItemVisible(
            NonNullList<ItemStack> displayedItemStacks,
            Item baseItem,
            Collection<ItemStack> complexItemStacks
    ) {
        if (!containsBaseItem(displayedItemStacks, baseItem)) {
            return false;
        }

        boolean changed = false;
        for (ItemStack itemStack : complexItemStacks) {
            if (!isComplexNbtStackForBaseItem(itemStack, baseItem)) {
                continue;
            }
            if (containsSameItemAndTags(displayedItemStacks, itemStack)) {
                continue;
            }
            displayedItemStacks.add(itemStack.copy());
            changed = true;
        }
        return changed;
    }

    private static boolean containsBaseItem(Collection<ItemStack> displayedItemStacks, Item baseItem) {
        return displayedItemStacks.stream().anyMatch(itemStack -> itemStack.is(baseItem));
    }

    private static boolean isComplexNbtStackForBaseItem(ItemStack itemStack, Item baseItem) {
        return !itemStack.isEmpty() && itemStack.is(baseItem) && itemStack.hasTag();
    }

    private static boolean containsSameItemAndTags(Collection<ItemStack> displayedItemStacks, ItemStack checkedItemStack) {
        return displayedItemStacks.stream()
                .anyMatch(itemStack -> ItemStack.isSameItemSameTags(itemStack, checkedItemStack));
    }

    private record ComplexNbtItemProvider(
            Supplier<Item> baseItem,
            Supplier<Collection<ItemStack>> itemStacks
    ) {
    }
}
