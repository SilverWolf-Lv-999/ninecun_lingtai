package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.common.ability.PushingMountainsManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record PushingMountainsRestorePacket(int entityId) {
    public static void encode(PushingMountainsRestorePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
    }

    public static PushingMountainsRestorePacket decode(FriendlyByteBuf buffer) {
        return new PushingMountainsRestorePacket(buffer.readVarInt());
    }

    public static void handle(PushingMountainsRestorePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null) {
            PushingMountainsManager.restore(player, packet.entityId());
        }
    }
}
