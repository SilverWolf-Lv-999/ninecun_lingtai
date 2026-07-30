package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.client.player.ClientPressureManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncPressureStatePacket(boolean affected, float severity, boolean immobilized) {
    public static void encode(SyncPressureStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.affected);
        buffer.writeFloat(packet.severity);
        buffer.writeBoolean(packet.immobilized);
    }

    public static SyncPressureStatePacket decode(FriendlyByteBuf buffer) {
        return new SyncPressureStatePacket(
                buffer.readBoolean(),
                buffer.readFloat(),
                buffer.readBoolean()
        );
    }

    public static void handle(SyncPressureStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientPressureManager.setPressure(
                        packet.affected,
                        packet.severity,
                        packet.immobilized
                )
        );
    }
}
