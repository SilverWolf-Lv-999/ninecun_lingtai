package net.seraphina.ninecun_lingtai.client.player;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.seraphina.ninecun_lingtai.api.book.BookClientData;
import net.seraphina.ninecun_lingtai.api.book.BookPageData;
import net.seraphina.ninecun_lingtai.client.screen.BookReadingScreen;
import net.seraphina.ninecun_lingtai.client.utility.ClientCreativeTabUtility;
import net.seraphina.ninecun_lingtai.common.item.BookCreativeTabCache;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ClientBookManager {
    private static Map<ResourceLocation, BookClientData> books = Map.of();

    private ClientBookManager() {
    }

    public static void acceptCatalog(List<BookClientData> updatedBooks) {
        Map<ResourceLocation, BookClientData> bookMap = new LinkedHashMap<>();
        updatedBooks.stream()
                .sorted(Comparator.comparing(book -> book.id().toString()))
                .forEach(book -> bookMap.put(book.id(), book));
        books = Map.copyOf(bookMap);
        BookCreativeTabCache.setBooks(updatedBooks);
        ClientCreativeTabUtility.rebuildCreativeTabs();
    }

    public static void openPage(BookPageData page) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof BookReadingScreen screen && screen.isReading(page.id())) {
            screen.updatePage(page);
            return;
        }
        minecraft.setScreen(new BookReadingScreen(page));
    }

    public static Optional<BookClientData> getBook(ResourceLocation bookId) {
        return Optional.ofNullable(books.get(bookId));
    }

    public static void reset() {
        books = Map.of();
        BookCreativeTabCache.clear();
    }
}
