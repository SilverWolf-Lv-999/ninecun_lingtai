package net.seraphina.ninecun_lingtai.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.utility.BookItemUtility;

import java.util.Objects;
import java.util.function.Supplier;

public record RequestBookPagePacket(ResourceLocation bookId, int pageIndex) {
    public RequestBookPagePacket {
        Objects.requireNonNull(bookId, "bookId");
        if (pageIndex < 0) {
            throw new IllegalArgumentException("pageIndex must not be negative");
        }
    }

    public static void encode(RequestBookPagePacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.bookId());
        buffer.writeVarInt(packet.pageIndex());
    }

    public static RequestBookPagePacket decode(FriendlyByteBuf buffer) {
        return new RequestBookPagePacket(buffer.readResourceLocation(), buffer.readVarInt());
    }

    public static void handle(
            RequestBookPagePacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player == null || !isHoldingBook(player, packet.bookId())) {
            return;
        }
        ModNetwork.sendBookPage(player, packet.bookId(), packet.pageIndex());
    }

    private static boolean isHoldingBook(ServerPlayer player, ResourceLocation bookId) {
        return BookItemUtility.isBook(player.getMainHandItem(), bookId)
                || BookItemUtility.isBook(player.getOffhandItem(), bookId);
    }
}
