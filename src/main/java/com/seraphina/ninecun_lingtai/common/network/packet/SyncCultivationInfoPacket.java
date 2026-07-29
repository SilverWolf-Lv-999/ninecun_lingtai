package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.client.player.ClientCultivationInfoManager;
import com.seraphina.ninecun_lingtai.common.lifespan.LifespanSnapshot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncCultivationInfoPacket(LifespanSnapshot lifespan) {
    public static void encode(SyncCultivationInfoPacket packet, FriendlyByteBuf buffer) {
        LifespanSnapshot lifespan = packet.lifespan;
        buffer.writeVarLong(lifespan.ageTicks());
        buffer.writeVarInt(lifespan.maximumLifespanYears());
        buffer.writeBoolean(lifespan.immortal());
    }

    public static SyncCultivationInfoPacket decode(FriendlyByteBuf buffer) {
        return new SyncCultivationInfoPacket(new LifespanSnapshot(
                buffer.readVarLong(),
                buffer.readVarInt(),
                buffer.readBoolean()
        ));
    }

    public static void handle(
            SyncCultivationInfoPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientCultivationInfoManager.accept(packet.lifespan)
        );
    }
}
