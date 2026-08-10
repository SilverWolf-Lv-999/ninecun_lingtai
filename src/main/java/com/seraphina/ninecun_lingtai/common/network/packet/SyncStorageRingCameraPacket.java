package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.client.ring.ClientStorageRingProjectionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record SyncStorageRingCameraPacket(
        UUID ringId,
        Vec3 cameraPosition,
        float cameraYRot,
        float cameraXRot
) {
    public static void encode(SyncStorageRingCameraPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.ringId);
        buffer.writeDouble(packet.cameraPosition.x);
        buffer.writeDouble(packet.cameraPosition.y);
        buffer.writeDouble(packet.cameraPosition.z);
        buffer.writeFloat(packet.cameraYRot);
        buffer.writeFloat(packet.cameraXRot);
    }

    public static SyncStorageRingCameraPacket decode(FriendlyByteBuf buffer) {
        return new SyncStorageRingCameraPacket(
                buffer.readUUID(),
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                buffer.readFloat(),
                buffer.readFloat()
        );
    }

    public static void handle(
            SyncStorageRingCameraPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientStorageRingProjectionManager.acceptCamera(packet)
        ));
        context.setPacketHandled(true);
    }
}
