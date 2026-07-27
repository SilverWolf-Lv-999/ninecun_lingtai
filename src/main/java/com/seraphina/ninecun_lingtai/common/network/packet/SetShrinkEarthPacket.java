package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.common.player.PlayerSoulRoamingManager;
import com.seraphina.ninecun_lingtai.common.spell.ShrinkEarthManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SetShrinkEarthPacket(boolean active) {
    public static void encode(SetShrinkEarthPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.active);
    }

    public static SetShrinkEarthPacket decode(FriendlyByteBuf buffer) {
        return new SetShrinkEarthPacket(buffer.readBoolean());
    }

    public static void handle(SetShrinkEarthPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null && !PlayerSoulRoamingManager.isSoulRoaming(player)) {
            ShrinkEarthManager.requestSetActive(player, packet.active);
        }
    }
}
