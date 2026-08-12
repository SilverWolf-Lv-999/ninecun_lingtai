package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.api.element.FiveElementValues;
import net.seraphina.ninecun_lingtai.client.player.ClientMeditationManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record SyncMeditationStatePacket(
        UUID playerId,
        boolean meditating,
        FiveElementValues environmentValues,
        FiveElementValues movementAffinities,
        int realmLevel
) {
    public static void encode(SyncMeditationStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.playerId);
        buffer.writeBoolean(packet.meditating);
        writeFiveElementValues(buffer, packet.environmentValues);
        writeFiveElementValues(buffer, packet.movementAffinities);
        buffer.writeVarInt(packet.realmLevel);
    }

    public static SyncMeditationStatePacket decode(FriendlyByteBuf buffer) {
        return new SyncMeditationStatePacket(
                buffer.readUUID(),
                buffer.readBoolean(),
                readFiveElementValues(buffer),
                readFiveElementValues(buffer),
                buffer.readVarInt()
        );
    }

    public static void handle(SyncMeditationStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientMeditationManager.setMeditating(
                        packet.playerId,
                        packet.meditating,
                        packet.environmentValues,
                        packet.movementAffinities,
                        packet.realmLevel
                )
        );
    }

    private static void writeFiveElementValues(FriendlyByteBuf buffer, FiveElementValues values) {
        buffer.writeFloat(values.metal());
        buffer.writeFloat(values.wood());
        buffer.writeFloat(values.water());
        buffer.writeFloat(values.fire());
        buffer.writeFloat(values.earth());
    }

    private static FiveElementValues readFiveElementValues(FriendlyByteBuf buffer) {
        return new FiveElementValues(
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat()
        );
    }
}
