package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.common.ring.RingStorageManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class CloseStorageRingPacket {
    public static final CloseStorageRingPacket INSTANCE = new CloseStorageRingPacket();

    private CloseStorageRingPacket() {
    }

    public static void encode(CloseStorageRingPacket packet, FriendlyByteBuf buffer) {
    }

    public static CloseStorageRingPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(
            CloseStorageRingPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null) {
            RingStorageManager.close(player);
        }
    }
}
