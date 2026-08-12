package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.common.ring.RingStorageManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record StorageRingCameraPacket(
        float leftImpulse,
        float forwardImpulse,
        boolean movingUp,
        boolean movingDown,
        float yRot,
        float xRot
) {
    public static void encode(StorageRingCameraPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.leftImpulse);
        buffer.writeFloat(packet.forwardImpulse);
        buffer.writeBoolean(packet.movingUp);
        buffer.writeBoolean(packet.movingDown);
        buffer.writeFloat(packet.yRot);
        buffer.writeFloat(packet.xRot);
    }

    public static StorageRingCameraPacket decode(FriendlyByteBuf buffer) {
        return new StorageRingCameraPacket(
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readFloat(),
                buffer.readFloat()
        );
    }

    public static void handle(
            StorageRingCameraPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                RingStorageManager.updateCamera(
                        player,
                        packet.leftImpulse,
                        packet.forwardImpulse,
                        packet.movingUp,
                        packet.movingDown,
                        packet.yRot,
                        packet.xRot
                );
            }
        });
        context.setPacketHandled(true);
    }
}
