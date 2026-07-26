package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class RequestCultivationInfoPacket {
    public static final RequestCultivationInfoPacket INSTANCE = new RequestCultivationInfoPacket();

    private RequestCultivationInfoPacket() {
    }

    public static void encode(RequestCultivationInfoPacket packet, FriendlyByteBuf buffer) {
    }

    public static RequestCultivationInfoPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(
            RequestCultivationInfoPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null) {
            ModNetwork.sendCultivationInfo(player);
        }
    }
}
