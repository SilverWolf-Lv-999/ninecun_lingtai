package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.common.player.PlayerSoulRoamingManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class StartSoulRoamingPacket {
    public static final StartSoulRoamingPacket INSTANCE = new StartSoulRoamingPacket();

    private StartSoulRoamingPacket() {
    }

    public static void encode(StartSoulRoamingPacket packet, FriendlyByteBuf buffer) {
    }

    public static StartSoulRoamingPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(
            StartSoulRoamingPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null) {
            PlayerSoulRoamingManager.startSoulRoaming(player);
        }
    }
}
