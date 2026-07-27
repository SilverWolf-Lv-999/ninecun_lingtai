package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.common.flight.LightFlightManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SetLightFlightPacket(boolean flying) {
    public static void encode(SetLightFlightPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.flying);
    }

    public static SetLightFlightPacket decode(FriendlyByteBuf buffer) {
        return new SetLightFlightPacket(buffer.readBoolean());
    }

    public static void handle(SetLightFlightPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null) {
            LightFlightManager.requestSetFlying(player, packet.flying);
        }
    }
}
