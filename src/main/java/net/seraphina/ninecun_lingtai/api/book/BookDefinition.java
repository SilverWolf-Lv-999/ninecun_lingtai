package net.seraphina.ninecun_lingtai.api.book;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record BookDefinition(
        ResourceLocation id,
        String title,
        String author,
        List<String> pages
) {
    public BookDefinition {
        Objects.requireNonNull(id, "id");
        title = requireTitle(title);
        author = Objects.requireNonNullElse(author, "");
        pages = List.copyOf(pages);
        if (pages.isEmpty()) {
            throw new IllegalArgumentException("pages must not be empty");
        }
    }

    public Optional<BookPageData> getPage(int pageIndex) {
        if (pageIndex < 0 || pageIndex >= this.pages.size()) {
            return Optional.empty();
        }
        return Optional.of(new BookPageData(
                this.id,
                this.title,
                this.author,
                pageIndex,
                this.pages.size(),
                this.pages.get(pageIndex)
        ));
    }

    private static String requireTitle(String value) {
        Objects.requireNonNull(value, "title");
        if (value.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        return value;
    }
}
