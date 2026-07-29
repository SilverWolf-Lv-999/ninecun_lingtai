package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.common.ability.PushingMountainsManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class PushingMountainsEntityizePacket {
    public static final PushingMountainsEntityizePacket INSTANCE = new PushingMountainsEntityizePacket();

    private PushingMountainsEntityizePacket() {
    }

    public static void encode(PushingMountainsEntityizePacket packet, FriendlyByteBuf buffer) {
    }

    public static PushingMountainsEntityizePacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(PushingMountainsEntityizePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null) {
            PushingMountainsManager.entityizeSelection(player);
        }
    }
}
