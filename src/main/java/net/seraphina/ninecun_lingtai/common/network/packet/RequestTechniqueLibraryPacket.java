package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class RequestTechniqueLibraryPacket {
    public static final RequestTechniqueLibraryPacket INSTANCE = new RequestTechniqueLibraryPacket();

    private RequestTechniqueLibraryPacket() {
    }

    public static void encode(RequestTechniqueLibraryPacket packet, FriendlyByteBuf buffer) {
    }

    public static RequestTechniqueLibraryPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(
            RequestTechniqueLibraryPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null) {
            ModNetwork.sendTechniqueLibrary(player);
        }
    }
}
