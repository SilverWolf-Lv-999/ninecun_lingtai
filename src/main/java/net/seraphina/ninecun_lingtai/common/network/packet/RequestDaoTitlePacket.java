package net.seraphina.ninecun_lingtai.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;

import java.util.function.Supplier;

public final class RequestDaoTitlePacket {
    public static final RequestDaoTitlePacket INSTANCE = new RequestDaoTitlePacket();

    private RequestDaoTitlePacket() {
    }

    public static void encode(RequestDaoTitlePacket packet, FriendlyByteBuf buffer) {
    }

    public static RequestDaoTitlePacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(RequestDaoTitlePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            ModNetwork.sendDaoTitle(player, player);
        }
        context.setPacketHandled(true);
    }
}
