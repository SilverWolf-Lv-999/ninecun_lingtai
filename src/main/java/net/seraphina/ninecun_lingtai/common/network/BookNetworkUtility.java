package net.seraphina.ninecun_lingtai.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.seraphina.ninecun_lingtai.api.book.BookClientData;
import net.seraphina.ninecun_lingtai.api.book.BookPageData;
import net.seraphina.ninecun_lingtai.utility.BookPageUtility;

import java.util.ArrayList;
import java.util.List;

public final class BookNetworkUtility {
    private static final int MAXIMUM_BOOK_COUNT = 2048;
    private static final int MAXIMUM_TITLE_LENGTH = 256;
    private static final int MAXIMUM_AUTHOR_LENGTH = 256;

    public static void writeCatalog(FriendlyByteBuf buffer, List<BookClientData> books) {
        if (books.size() > MAXIMUM_BOOK_COUNT) {
            throw new IllegalArgumentException("Too many book catalog entries");
        }
        buffer.writeVarInt(books.size());
        for (BookClientData book : books) {
            buffer.writeResourceLocation(book.id());
            buffer.writeUtf(book.title(), MAXIMUM_TITLE_LENGTH);
            buffer.writeUtf(book.author(), MAXIMUM_AUTHOR_LENGTH);
        }
    }

    public static List<BookClientData> readCatalog(FriendlyByteBuf buffer) {
        int bookCount = buffer.readVarInt();
        if (bookCount < 0 || bookCount > MAXIMUM_BOOK_COUNT) {
            throw new IllegalArgumentException("Invalid book catalog entry count");
        }

        List<BookClientData> books = new ArrayList<>(bookCount);
        for (int index = 0; index < bookCount; index++) {
            ResourceLocation bookId = buffer.readResourceLocation();
            String title = buffer.readUtf(MAXIMUM_TITLE_LENGTH);
            String author = buffer.readUtf(MAXIMUM_AUTHOR_LENGTH);
            books.add(new BookClientData(bookId, title, author));
        }
        return List.copyOf(books);
    }

    public static void writePage(FriendlyByteBuf buffer, BookPageData page) {
        buffer.writeResourceLocation(page.id());
        buffer.writeUtf(page.title(), MAXIMUM_TITLE_LENGTH);
        buffer.writeUtf(page.author(), MAXIMUM_AUTHOR_LENGTH);
        buffer.writeVarInt(page.pageIndex());
        buffer.writeVarInt(page.pageCount());
        buffer.writeUtf(page.content(), BookPageUtility.MAXIMUM_PAGE_CONTENT_LENGTH);
    }

    public static BookPageData readPage(FriendlyByteBuf buffer) {
        return new BookPageData(
                buffer.readResourceLocation(),
                buffer.readUtf(MAXIMUM_TITLE_LENGTH),
                buffer.readUtf(MAXIMUM_AUTHOR_LENGTH),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readUtf(BookPageUtility.MAXIMUM_PAGE_CONTENT_LENGTH)
        );
    }
}
