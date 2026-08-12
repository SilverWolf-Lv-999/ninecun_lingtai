package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.common.ability.PushingMountainsManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record PushingMountainsThrowPacket(int entityId) {
    public static void encode(PushingMountainsThrowPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
    }

    public static PushingMountainsThrowPacket decode(FriendlyByteBuf buffer) {
        return new PushingMountainsThrowPacket(buffer.readVarInt());
    }

    public static void handle(PushingMountainsThrowPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null) {
            PushingMountainsManager.throwRegion(player, packet.entityId());
        }
    }
}
