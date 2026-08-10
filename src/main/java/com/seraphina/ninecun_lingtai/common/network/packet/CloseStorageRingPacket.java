package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.common.ring.RingStorageManager;
import com.seraphina.ninecun_lingtai.common.ring.RingStorageSavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record CloseStorageRingPacket(RingStorageSavedData.CameraState cameraState) {
    public static final CloseStorageRingPacket INSTANCE = new CloseStorageRingPacket(null);

    public static CloseStorageRingPacket withCameraState(Vec3 localPosition, float yRot, float xRot) {
        return new CloseStorageRingPacket(new RingStorageSavedData.CameraState(localPosition, yRot, xRot));
    }

    public static void encode(CloseStorageRingPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.cameraState != null);
        if (packet.cameraState == null) {
            return;
        }

        Vec3 localPosition = packet.cameraState.localPosition();
        buffer.writeDouble(localPosition.x);
        buffer.writeDouble(localPosition.y);
        buffer.writeDouble(localPosition.z);
        buffer.writeFloat(packet.cameraState.yRot());
        buffer.writeFloat(packet.cameraState.xRot());
    }

    public static CloseStorageRingPacket decode(FriendlyByteBuf buffer) {
        if (!buffer.readBoolean()) {
            return INSTANCE;
        }

        return withCameraState(
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                buffer.readFloat(),
                buffer.readFloat()
        );
    }

    public static void handle(
            CloseStorageRingPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                RingStorageManager.close(player, packet.cameraState);
            }
        });
        context.setPacketHandled(true);
    }
}
