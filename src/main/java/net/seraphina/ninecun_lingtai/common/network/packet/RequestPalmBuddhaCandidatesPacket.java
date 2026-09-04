package net.seraphina.ninecun_lingtai.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;

import java.util.function.Supplier;

public final class RequestPalmBuddhaCandidatesPacket {
    public static final RequestPalmBuddhaCandidatesPacket INSTANCE = new RequestPalmBuddhaCandidatesPacket();

    private RequestPalmBuddhaCandidatesPacket() {
    }

    public static void encode(RequestPalmBuddhaCandidatesPacket packet, FriendlyByteBuf buffer) {
    }

    public static RequestPalmBuddhaCandidatesPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(
            RequestPalmBuddhaCandidatesPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer owner = context.getSender();
            if (owner != null) {
                ModNetwork.sendPalmBuddhaCandidates(owner);
            }
        });
        context.setPacketHandled(true);
    }
}
