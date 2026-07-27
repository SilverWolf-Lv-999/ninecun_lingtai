package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.client.player.ClientShrinkEarthManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record SyncShrinkEarthPacket(
        UUID playerId,
        boolean active,
        int compressionScale
) {
    public static void encode(SyncShrinkEarthPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.playerId);
        buffer.writeBoolean(packet.active);
        buffer.writeVarInt(packet.compressionScale);
    }

    public static SyncShrinkEarthPacket decode(FriendlyByteBuf buffer) {
        return new SyncShrinkEarthPacket(
                buffer.readUUID(),
                buffer.readBoolean(),
                buffer.readVarInt()
        );
    }

    public static void handle(SyncShrinkEarthPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientShrinkEarthManager.setActive(
                        packet.playerId,
                        packet.active,
                        packet.compressionScale
                )
        );
    }
}
