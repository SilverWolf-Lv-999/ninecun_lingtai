package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class RequestPlayerValueControlSettingsPacket {
    public static final RequestPlayerValueControlSettingsPacket INSTANCE =
            new RequestPlayerValueControlSettingsPacket();

    private RequestPlayerValueControlSettingsPacket() {
    }

    public static void encode(RequestPlayerValueControlSettingsPacket packet, FriendlyByteBuf buffer) {
    }

    public static RequestPlayerValueControlSettingsPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(
            RequestPlayerValueControlSettingsPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null) {
            ModNetwork.sendPlayerValueControlSettings(player);
        }
    }
}
