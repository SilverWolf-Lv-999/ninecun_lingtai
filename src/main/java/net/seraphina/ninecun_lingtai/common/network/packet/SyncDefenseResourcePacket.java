package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.client.player.ClientDefenseResourceManager;
import net.seraphina.ninecun_lingtai.common.player.DefenseResourceSnapshot;
import net.seraphina.ninecun_lingtai.common.player.DefenseResourceType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncDefenseResourcePacket(DefenseResourceSnapshot snapshot) {
    public static void encode(SyncDefenseResourcePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.snapshot.type().ordinal());
        buffer.writeVarInt(packet.snapshot.current());
        buffer.writeVarInt(packet.snapshot.maximum());
        buffer.writeVarInt(packet.snapshot.elementMask());
    }

    public static SyncDefenseResourcePacket decode(FriendlyByteBuf buffer) {
        DefenseResourceType type = decodeType(buffer.readVarInt());
        return new SyncDefenseResourcePacket(new DefenseResourceSnapshot(
                type,
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt()
        ));
    }

    public static void handle(
            SyncDefenseResourcePacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientDefenseResourceManager.accept(packet.snapshot)
        );
    }

    private static DefenseResourceType decodeType(int typeIndex) {
        DefenseResourceType[] types = DefenseResourceType.values();
        return typeIndex >= 0 && typeIndex < types.length
                ? types[typeIndex]
                : DefenseResourceType.NONE;
    }
}
