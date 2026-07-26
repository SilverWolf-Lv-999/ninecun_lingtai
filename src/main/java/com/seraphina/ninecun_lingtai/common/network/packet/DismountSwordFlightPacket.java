package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.common.spell.SwordFlightManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 客户端双击潜行请求收剑；是否贴地由服务端复核。 */
public final class DismountSwordFlightPacket {
    public static final DismountSwordFlightPacket INSTANCE = new DismountSwordFlightPacket();

    private DismountSwordFlightPacket() {
    }

    public static void encode(DismountSwordFlightPacket packet, FriendlyByteBuf buffer) {
    }

    public static DismountSwordFlightPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(DismountSwordFlightPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null) {
            SwordFlightManager.requestDismount(player);
        }
    }
}
