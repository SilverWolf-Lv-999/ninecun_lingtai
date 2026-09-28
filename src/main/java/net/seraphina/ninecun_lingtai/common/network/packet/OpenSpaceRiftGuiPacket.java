package net.seraphina.ninecun_lingtai.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.seraphina.ninecun_lingtai.client.space.ClientSpaceRiftManager;

import java.util.List;
import java.util.function.Supplier;

public record OpenSpaceRiftGuiPacket(List<ResourceLocation> dimensions) {
    public OpenSpaceRiftGuiPacket {
        dimensions = List.copyOf(dimensions);
    }

    public static void encode(OpenSpaceRiftGuiPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(Math.min(packet.dimensions.size(), 128));
        packet.dimensions.stream().limit(128).forEach(buffer::writeResourceLocation);
    }

    public static OpenSpaceRiftGuiPacket decode(FriendlyByteBuf buffer) {
        int size = Math.min(buffer.readVarInt(), 128);
        java.util.ArrayList<ResourceLocation> dimensions = new java.util.ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            dimensions.add(buffer.readResourceLocation());
        }
        return new OpenSpaceRiftGuiPacket(dimensions);
    }

    public static void handle(OpenSpaceRiftGuiPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSpaceRiftManager.open(packet.dimensions()));
    }
}
