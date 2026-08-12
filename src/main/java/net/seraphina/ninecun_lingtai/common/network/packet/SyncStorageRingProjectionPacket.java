package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.client.ring.ClientStorageRingProjectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public record SyncStorageRingProjectionPacket(
        boolean active,
        UUID ringId,
        Vec3 cameraPosition,
        float cameraYRot,
        float cameraXRot,
        List<BlockSnapshot> blocks
) {
    private static final int MAXIMUM_BLOCKS = 8_192;

    public static SyncStorageRingProjectionPacket inactive() {
        return new SyncStorageRingProjectionPacket(
                false,
                new UUID(0L, 0L),
                Vec3.ZERO,
                0.0F,
                0.0F,
                List.of()
        );
    }

    public static void encode(SyncStorageRingProjectionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.active);
        if (!packet.active) {
            return;
        }

        buffer.writeUUID(packet.ringId);
        buffer.writeDouble(packet.cameraPosition.x);
        buffer.writeDouble(packet.cameraPosition.y);
        buffer.writeDouble(packet.cameraPosition.z);
        buffer.writeFloat(packet.cameraYRot);
        buffer.writeFloat(packet.cameraXRot);
        buffer.writeVarInt(packet.blocks.size());
        for (BlockSnapshot block : packet.blocks) {
            buffer.writeBlockPos(block.localPosition);
            buffer.writeVarInt(block.blockStateId);
        }
    }

    public static SyncStorageRingProjectionPacket decode(FriendlyByteBuf buffer) {
        if (!buffer.readBoolean()) {
            return inactive();
        }

        UUID ringId = buffer.readUUID();
        Vec3 cameraPosition = new Vec3(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble()
        );
        float cameraYRot = buffer.readFloat();
        float cameraXRot = buffer.readFloat();
        int blockCount = buffer.readVarInt();
        if (blockCount < 0 || blockCount > MAXIMUM_BLOCKS) {
            throw new IllegalArgumentException("Invalid storage ring projection block count: " + blockCount);
        }

        List<BlockSnapshot> blocks = new ArrayList<>(blockCount);
        for (int index = 0; index < blockCount; index++) {
            blocks.add(new BlockSnapshot(buffer.readBlockPos(), buffer.readVarInt()));
        }
        return new SyncStorageRingProjectionPacket(
                true,
                ringId,
                cameraPosition,
                cameraYRot,
                cameraXRot,
                List.copyOf(blocks)
        );
    }

    public static void handle(
            SyncStorageRingProjectionPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientStorageRingProjectionManager.acceptProjection(packet)
        ));
        context.setPacketHandled(true);
    }

    public record BlockSnapshot(BlockPos localPosition, int blockStateId) {
    }
}
