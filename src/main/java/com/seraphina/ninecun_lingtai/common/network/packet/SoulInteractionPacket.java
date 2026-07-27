package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.common.player.PlayerSoulRoamingManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SoulInteractionPacket(int targetEntityId) {
    public static void encode(SoulInteractionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.targetEntityId);
    }

    public static SoulInteractionPacket decode(FriendlyByteBuf buffer) {
        return new SoulInteractionPacket(buffer.readVarInt());
    }

    public static void handle(
            SoulInteractionPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                PlayerSoulRoamingManager.interactWithEntity(player, packet.targetEntityId);
            }
        });
        context.setPacketHandled(true);
    }
}
