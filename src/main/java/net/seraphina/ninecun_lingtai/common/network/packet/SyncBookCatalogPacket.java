package net.seraphina.ninecun_lingtai.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.seraphina.ninecun_lingtai.api.book.BookClientData;
import net.seraphina.ninecun_lingtai.client.player.ClientBookManager;
import net.seraphina.ninecun_lingtai.common.network.BookNetworkUtility;

import java.util.List;
import java.util.function.Supplier;

public record SyncBookCatalogPacket(List<BookClientData> books) {
    public SyncBookCatalogPacket {
        books = List.copyOf(books);
    }

    public static void encode(SyncBookCatalogPacket packet, FriendlyByteBuf buffer) {
        BookNetworkUtility.writeCatalog(buffer, packet.books());
    }

    public static SyncBookCatalogPacket decode(FriendlyByteBuf buffer) {
        return new SyncBookCatalogPacket(BookNetworkUtility.readCatalog(buffer));
    }

    public static void handle(
            SyncBookCatalogPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientBookManager.acceptCatalog(packet.books())
        );
    }
}
