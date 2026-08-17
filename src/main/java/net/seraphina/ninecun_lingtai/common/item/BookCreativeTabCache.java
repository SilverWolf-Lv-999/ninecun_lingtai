package net.seraphina.ninecun_lingtai.common.item;

import net.minecraft.world.item.ItemStack;
import net.seraphina.ninecun_lingtai.api.book.BookClientData;
import net.seraphina.ninecun_lingtai.common.registry.ModItems;
import net.seraphina.ninecun_lingtai.utility.BookItemUtility;

import java.util.ArrayList;
import java.util.List;

public final class BookCreativeTabCache {
    private static volatile List<ItemStack> itemStacks = List.of();

    public static void setBooks(List<BookClientData> books) {
        itemStacks = books.stream()
                .map(BookItemUtility::createStack)
                .toList();
    }

    public static List<ItemStack> getItemStacks() {
        List<ItemStack> displayedItemStacks = new ArrayList<>(itemStacks.size() + 1);
        displayedItemStacks.add(new ItemStack(ModItems.BOOK.get()));
        itemStacks.stream().map(ItemStack::copy).forEach(displayedItemStacks::add);
        return List.copyOf(displayedItemStacks);
    }

    public static void clear() {
        itemStacks = List.of();
    }
}
