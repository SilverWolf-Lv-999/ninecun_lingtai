package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.common.player.PlayerSoulSenseData;
import net.seraphina.ninecun_lingtai.common.soul.SoulSenseManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SetSoulSenseFilterPacket(int filterMask) {
    public static void encode(SetSoulSenseFilterPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.filterMask);
    }

    public static SetSoulSenseFilterPacket decode(FriendlyByteBuf buffer) {
        return new SetSoulSenseFilterPacket(buffer.readVarInt());
    }

    public static void handle(SetSoulSenseFilterPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player == null) {
            return;
        }
        PlayerSoulSenseData.setFilterMask(player, packet.filterMask);
        SoulSenseManager.refresh(player);
    }
}
