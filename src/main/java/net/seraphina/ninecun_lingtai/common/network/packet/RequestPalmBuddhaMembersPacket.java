package net.seraphina.ninecun_lingtai.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.spell.palm_buddha.PalmBuddhaMemberPacketLimits;

import java.util.function.Supplier;

/** 按页请求施法者自己佛国的成员摘要。 */
public record RequestPalmBuddhaMembersPacket(int pageIndex) {
    public static final RequestPalmBuddhaMembersPacket FIRST_PAGE =
            new RequestPalmBuddhaMembersPacket(0);

    public RequestPalmBuddhaMembersPacket {
        pageIndex = Math.max(
                0,
                Math.min(PalmBuddhaMemberPacketLimits.MAXIMUM_PAGE_COUNT - 1, pageIndex)
        );
    }

    public static void encode(
            RequestPalmBuddhaMembersPacket packet,
            FriendlyByteBuf buffer
    ) {
        buffer.writeVarInt(packet.pageIndex());
    }

    public static RequestPalmBuddhaMembersPacket decode(FriendlyByteBuf buffer) {
        return new RequestPalmBuddhaMembersPacket(buffer.readVarInt());
    }

    public static void handle(
            RequestPalmBuddhaMembersPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer owner = context.getSender();
            if (owner != null) {
                ModNetwork.sendPalmBuddhaMembers(owner, packet.pageIndex());
            }
        });
        context.setPacketHandled(true);
    }
}
