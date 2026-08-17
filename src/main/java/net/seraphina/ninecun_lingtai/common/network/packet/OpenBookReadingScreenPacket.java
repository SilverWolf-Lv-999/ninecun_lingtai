package net.seraphina.ninecun_lingtai.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.seraphina.ninecun_lingtai.api.book.BookPageData;
import net.seraphina.ninecun_lingtai.client.player.ClientBookManager;
import net.seraphina.ninecun_lingtai.common.network.BookNetworkUtility;

import java.util.function.Supplier;

public record OpenBookReadingScreenPacket(BookPageData page) {
    public static void encode(OpenBookReadingScreenPacket packet, FriendlyByteBuf buffer) {
        BookNetworkUtility.writePage(buffer, packet.page());
    }

    public static OpenBookReadingScreenPacket decode(FriendlyByteBuf buffer) {
        return new OpenBookReadingScreenPacket(BookNetworkUtility.readPage(buffer));
    }

    public static void handle(
            OpenBookReadingScreenPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientBookManager.openPage(packet.page())
        );
    }
}
