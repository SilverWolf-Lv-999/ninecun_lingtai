package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.common.ring.RingStorageManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SetStorageRingImmersionPacket(boolean immersive) {
    public static void encode(SetStorageRingImmersionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.immersive);
    }

    public static SetStorageRingImmersionPacket decode(FriendlyByteBuf buffer) {
        return new SetStorageRingImmersionPacket(buffer.readBoolean());
    }

    public static void handle(
            SetStorageRingImmersionPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                RingStorageManager.setImmersive(player, packet.immersive);
            }
        });
        context.setPacketHandled(true);
    }
}
