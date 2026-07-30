package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.common.ability.PressureManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SetPressureStatePacket(boolean active) {
    public static void encode(SetPressureStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.active);
    }

    public static SetPressureStatePacket decode(FriendlyByteBuf buffer) {
        return new SetPressureStatePacket(buffer.readBoolean());
    }

    public static void handle(SetPressureStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null) {
            PressureManager.setActive(player, packet.active);
        }
    }
}
