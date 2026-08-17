package net.seraphina.ninecun_lingtai.api.book;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record BookPageData(
        ResourceLocation id,
        String title,
        String author,
        int pageIndex,
        int pageCount,
        String content
) {
    public BookPageData {
        Objects.requireNonNull(id, "id");
        title = requireTitle(title);
        author = Objects.requireNonNullElse(author, "");
        content = Objects.requireNonNullElse(content, "");
        if (pageIndex < 0 || pageCount <= 0 || pageIndex >= pageCount) {
            throw new IllegalArgumentException("invalid book page bounds");
        }
    }

    private static String requireTitle(String value) {
        Objects.requireNonNull(value, "title");
        if (value.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        return value;
    }
}
