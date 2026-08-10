package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.common.ring.RingStorageManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record StorageRingInteractionPacket(
        BlockPos localPosition,
        Direction direction,
        Vec3 localHitLocation,
        boolean use
) {
    public static void encode(StorageRingInteractionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.localPosition);
        buffer.writeEnum(packet.direction);
        buffer.writeDouble(packet.localHitLocation.x);
        buffer.writeDouble(packet.localHitLocation.y);
        buffer.writeDouble(packet.localHitLocation.z);
        buffer.writeBoolean(packet.use);
    }

    public static StorageRingInteractionPacket decode(FriendlyByteBuf buffer) {
        return new StorageRingInteractionPacket(
                buffer.readBlockPos(),
                buffer.readEnum(Direction.class),
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                buffer.readBoolean()
        );
    }

    public static void handle(
            StorageRingInteractionPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                RingStorageManager.interact(
                        player,
                        packet.localPosition,
                        packet.direction,
                        packet.localHitLocation,
                        packet.use
                );
            }
        });
        context.setPacketHandled(true);
    }
}
