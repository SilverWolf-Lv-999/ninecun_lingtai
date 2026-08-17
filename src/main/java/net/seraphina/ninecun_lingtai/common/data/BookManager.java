package net.seraphina.ninecun_lingtai.common.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.seraphina.ninecun_lingtai.api.book.BookDefinition;
import net.seraphina.ninecun_lingtai.api.book.BookPageData;
import net.seraphina.ninecun_lingtai.utility.BookJsonUtility;
import net.seraphina.ninecun_lingtai.utility.BookPageUtility;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class BookManager extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final String DIRECTORY = "books";

    public static final BookManager INSTANCE = new BookManager();

    private volatile Map<ResourceLocation, BookDefinition> books = Map.of();

    private BookManager() {
        super(GSON, DIRECTORY);
    }

    public Optional<BookDefinition> getBook(ResourceLocation bookId) {
        return Optional.ofNullable(this.books.get(bookId));
    }

    public Optional<BookPageData> getPage(ResourceLocation bookId, int pageIndex) {
        return getBook(bookId).flatMap(book -> book.getPage(pageIndex));
    }

    public List<BookDefinition> getBooks() {
        return this.books.values().stream()
                .sorted(Comparator.comparing(book -> book.id().toString()))
                .toList();
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> jsonElements,
            ResourceManager resourceManager,
            ProfilerFiller profilerFiller
    ) {
        Map<ResourceLocation, BookDefinition> loadedBooks = new LinkedHashMap<>();
        jsonElements.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> loadBook(
                        entry.getKey(),
                        entry.getValue(),
                        resourceManager,
                        loadedBooks
                ));
        this.books = Map.copyOf(loadedBooks);
        LOGGER.info("Loaded {} books", this.books.size());
    }

    private static void loadBook(
            ResourceLocation bookId,
            JsonElement jsonElement,
            ResourceManager resourceManager,
            Map<ResourceLocation, BookDefinition> loadedBooks
    ) {
        try {
            BookJsonUtility.BookMetadata bookMetadata = BookJsonUtility.parse(bookId, jsonElement);
            String content = readContent(resourceManager, bookMetadata.content());
            loadedBooks.put(
                    bookId,
                    new BookDefinition(
                            bookId,
                            bookMetadata.title(),
                            bookMetadata.author(),
                            BookPageUtility.splitIntoPages(content)
                    )
            );
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to load book {}", bookId, exception);
        }
    }

    private static String readContent(ResourceManager resourceManager, ResourceLocation contentLocation) {
        Resource resource = resourceManager.getResource(contentLocation)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Missing book content resource " + contentLocation
                ));
        try (InputStream inputStream = resource.open()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Failed to read book content resource " + contentLocation,
                    exception
            );
        }
    }
}
