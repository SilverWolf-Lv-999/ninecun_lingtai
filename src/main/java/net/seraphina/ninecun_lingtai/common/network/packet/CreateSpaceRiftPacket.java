package net.seraphina.ninecun_lingtai.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.seraphina.ninecun_lingtai.common.ability.SpaceRiftManager;

import java.util.function.Supplier;

public record CreateSpaceRiftPacket(ResourceLocation dimensionId) {
    public static void encode(CreateSpaceRiftPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimensionId());
    }

    public static CreateSpaceRiftPacket decode(FriendlyByteBuf buffer) {
        return new CreateSpaceRiftPacket(buffer.readResourceLocation());
    }

    public static void handle(CreateSpaceRiftPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null) {
            SpaceRiftManager.create(player, packet.dimensionId());
        }
    }
}
