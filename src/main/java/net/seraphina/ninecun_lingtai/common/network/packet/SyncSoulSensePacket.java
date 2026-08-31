package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.client.player.ClientSoulSenseManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record SyncSoulSensePacket(
        boolean active,
        int filterMask,
        List<EntityEntry> entities,
        List<BlockEntry> blocks
) {
    public record EntityEntry(int entityId, byte categoryOrdinal) {
    }

    public record BlockEntry(BlockPos pos, byte categoryOrdinal) {
    }

    public static void encode(SyncSoulSensePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.active);
        buffer.writeVarInt(packet.filterMask);
        buffer.writeVarInt(packet.entities.size());
        for (EntityEntry entry : packet.entities) {
            buffer.writeVarInt(entry.entityId);
            buffer.writeByte(entry.categoryOrdinal);
        }
        buffer.writeVarInt(packet.blocks.size());
        for (BlockEntry entry : packet.blocks) {
            buffer.writeBlockPos(entry.pos);
            buffer.writeByte(entry.categoryOrdinal);
        }
    }

    public static SyncSoulSensePacket decode(FriendlyByteBuf buffer) {
        boolean active = buffer.readBoolean();
        int filterMask = buffer.readVarInt();
        int entityCount = buffer.readVarInt();
        List<EntityEntry> entities = new ArrayList<>(entityCount);
        for (int index = 0; index < entityCount; index++) {
            entities.add(new EntityEntry(buffer.readVarInt(), buffer.readByte()));
        }
        int blockCount = buffer.readVarInt();
        List<BlockEntry> blocks = new ArrayList<>(blockCount);
        for (int index = 0; index < blockCount; index++) {
            blocks.add(new BlockEntry(buffer.readBlockPos(), buffer.readByte()));
        }
        return new SyncSoulSensePacket(active, filterMask, entities, blocks);
    }

    public static void handle(SyncSoulSensePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientSoulSenseManager.applySync(
                        packet.active,
                        packet.filterMask,
                        packet.entities,
                        packet.blocks
                )
        );
    }
}
