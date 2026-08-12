package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.common.ability.PushingMountainsManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class PushingMountainsClearSelectionPacket {
    public static final PushingMountainsClearSelectionPacket INSTANCE = new PushingMountainsClearSelectionPacket();

    private PushingMountainsClearSelectionPacket() {
    }

    public static void encode(PushingMountainsClearSelectionPacket packet, FriendlyByteBuf buffer) {
    }

    public static PushingMountainsClearSelectionPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(PushingMountainsClearSelectionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null) {
            PushingMountainsManager.clearSelection(player);
        }
    }
}
