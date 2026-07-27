package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.common.flight.LightFlightManager;
import com.seraphina.ninecun_lingtai.common.player.PlayerSoulRoamingManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class BoostLightFlightPacket {
    public static final BoostLightFlightPacket INSTANCE = new BoostLightFlightPacket();

    private BoostLightFlightPacket() {
    }

    public static void encode(BoostLightFlightPacket packet, FriendlyByteBuf buffer) {
    }

    public static BoostLightFlightPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(BoostLightFlightPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null && !PlayerSoulRoamingManager.isSoulRoaming(player)) {
            LightFlightManager.requestBoost(player);
        }
    }
}
