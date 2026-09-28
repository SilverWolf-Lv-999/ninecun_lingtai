package net.seraphina.ninecun_lingtai.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.seraphina.ninecun_lingtai.common.ability.SpaceRiftManager;

import java.util.function.Supplier;

public final class RequestSpaceRiftGuiPacket {
    public static final RequestSpaceRiftGuiPacket INSTANCE = new RequestSpaceRiftGuiPacket();

    private RequestSpaceRiftGuiPacket() {
    }

    public static void encode(RequestSpaceRiftGuiPacket packet, FriendlyByteBuf buffer) {
    }

    public static RequestSpaceRiftGuiPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(RequestSpaceRiftGuiPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null) {
            SpaceRiftManager.sendDimensions(player);
        }
    }
}
