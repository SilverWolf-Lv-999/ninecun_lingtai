package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.client.player.ClientLightFlightManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record SyncLightFlightPacket(
        UUID playerId,
        boolean flying,
        int color,
        boolean boosting
) {
    public static void encode(SyncLightFlightPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.playerId);
        buffer.writeBoolean(packet.flying);
        buffer.writeVarInt(packet.color);
        buffer.writeBoolean(packet.boosting);
    }

    public static SyncLightFlightPacket decode(FriendlyByteBuf buffer) {
        return new SyncLightFlightPacket(
                buffer.readUUID(),
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readBoolean()
        );
    }

    public static void handle(SyncLightFlightPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientLightFlightManager.setFlying(
                        packet.playerId,
                        packet.flying,
                        packet.color,
                        packet.boosting
                )
        );
    }
}
