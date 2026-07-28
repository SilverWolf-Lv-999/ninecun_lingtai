package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.client.tribulation.ClientTribulationManager;
import com.seraphina.ninecun_lingtai.common.tribulation.TribulationPhase;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.DistExecutor;

import java.util.UUID;
import java.util.function.Supplier;

public record SyncTribulationStatePacket(
        UUID playerId,
        boolean active,
        int phaseIndex,
        int phaseRemainingTicks,
        int totalStrikes,
        int completedStrikes,
        double centerX,
        double centerY,
        double centerZ,
        float radius,
        float intensity
) {
    public static void encode(SyncTribulationStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.playerId);
        buffer.writeBoolean(packet.active);
        buffer.writeInt(packet.phaseIndex);
        buffer.writeInt(packet.phaseRemainingTicks);
        buffer.writeInt(packet.totalStrikes);
        buffer.writeInt(packet.completedStrikes);
        buffer.writeDouble(packet.centerX);
        buffer.writeDouble(packet.centerY);
        buffer.writeDouble(packet.centerZ);
        buffer.writeFloat(packet.radius);
        buffer.writeFloat(packet.intensity);
    }

    public static SyncTribulationStatePacket decode(FriendlyByteBuf buffer) {
        return new SyncTribulationStatePacket(
                buffer.readUUID(),
                buffer.readBoolean(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readFloat(),
                buffer.readFloat()
        );
    }

    public static void handle(
            SyncTribulationStatePacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        contextSupplier.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientTribulationManager.accept(packet)
        ));
        contextSupplier.get().setPacketHandled(true);
    }

    public TribulationPhase phase() {
        TribulationPhase[] phases = TribulationPhase.values();
        return phaseIndex >= 0 && phaseIndex < phases.length
                ? phases[phaseIndex]
                : TribulationPhase.PREPARING;
    }
}
