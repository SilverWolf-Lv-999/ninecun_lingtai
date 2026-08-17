package net.seraphina.ninecun_lingtai.utility;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.api.book.BookClientData;
import net.seraphina.ninecun_lingtai.api.book.BookDefinition;
import net.seraphina.ninecun_lingtai.common.registry.ModItems;

import java.util.Optional;

public final class BookItemUtility {
    private static final String BOOK_ID_KEY = ModMain.MOD_ID + ":book_id";
    private static final String BOOK_TITLE_KEY = ModMain.MOD_ID + ":book_title";
    private static final String BOOK_AUTHOR_KEY = ModMain.MOD_ID + ":book_author";

    public static ItemStack createStack(BookDefinition book) {
        return createStack(BookClientData.fromDefinition(book));
    }

    public static ItemStack createStack(BookClientData book) {
        ItemStack itemStack = new ItemStack(ModItems.BOOK.get());
        CompoundTag itemData = itemStack.getOrCreateTag();
        itemData.putString(BOOK_ID_KEY, book.id().toString());
        itemData.putString(BOOK_TITLE_KEY, book.title());
        itemData.putString(BOOK_AUTHOR_KEY, book.author());
        return itemStack;
    }

    public static Optional<ResourceLocation> getBookId(ItemStack itemStack) {
        if (!itemStack.is(ModItems.BOOK.get()) || !itemStack.hasTag()) {
            return Optional.empty();
        }
        return Optional.ofNullable(ResourceLocation.tryParse(itemStack.getTag().getString(BOOK_ID_KEY)));
    }

    public static Optional<String> getTitle(ItemStack itemStack) {
        return getText(itemStack, BOOK_TITLE_KEY);
    }

    public static Optional<String> getAuthor(ItemStack itemStack) {
        return getText(itemStack, BOOK_AUTHOR_KEY);
    }

    public static boolean isBook(ItemStack itemStack, ResourceLocation bookId) {
        return getBookId(itemStack).filter(bookId::equals).isPresent();
    }

    private static Optional<String> getText(ItemStack itemStack, String key) {
        if (!itemStack.is(ModItems.BOOK.get()) || !itemStack.hasTag()) {
            return Optional.empty();
        }
        String value = itemStack.getTag().getString(key);
        return value.isBlank() ? Optional.empty() : Optional.of(value);
    }
}
