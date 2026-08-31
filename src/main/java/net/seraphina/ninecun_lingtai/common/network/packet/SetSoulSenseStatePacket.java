package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.common.soul.SoulSenseManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SetSoulSenseStatePacket(boolean active) {
    public static void encode(SetSoulSenseStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.active);
    }

    public static SetSoulSenseStatePacket decode(FriendlyByteBuf buffer) {
        return new SetSoulSenseStatePacket(buffer.readBoolean());
    }

    public static void handle(SetSoulSenseStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null) {
            SoulSenseManager.setActive(player, packet.active);
        }
    }
}
