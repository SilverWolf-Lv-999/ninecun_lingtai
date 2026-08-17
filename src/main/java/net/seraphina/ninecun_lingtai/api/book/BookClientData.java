package net.seraphina.ninecun_lingtai.api.book;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record BookClientData(ResourceLocation id, String title, String author) {
    public BookClientData {
        Objects.requireNonNull(id, "id");
        title = requireTitle(title);
        author = Objects.requireNonNullElse(author, "");
    }

    public static BookClientData fromDefinition(BookDefinition book) {
        return new BookClientData(book.id(), book.title(), book.author());
    }

    private static String requireTitle(String value) {
        Objects.requireNonNull(value, "title");
        if (value.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        return value;
    }
}
