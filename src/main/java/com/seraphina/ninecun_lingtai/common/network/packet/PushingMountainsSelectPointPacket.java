package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.common.ability.PushingMountainsManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record PushingMountainsSelectPointPacket(BlockPos position) {
    public static void encode(PushingMountainsSelectPointPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.position);
    }

    public static PushingMountainsSelectPointPacket decode(FriendlyByteBuf buffer) {
        return new PushingMountainsSelectPointPacket(buffer.readBlockPos());
    }

    public static void handle(PushingMountainsSelectPointPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null) {
            PushingMountainsManager.selectPoint(player, packet.position());
        }
    }
}
