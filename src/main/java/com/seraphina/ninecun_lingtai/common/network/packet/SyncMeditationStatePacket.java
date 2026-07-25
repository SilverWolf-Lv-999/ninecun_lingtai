package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.client.player.ClientMeditationManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record SyncMeditationStatePacket(UUID playerId, boolean meditating) {
    public static void encode(SyncMeditationStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.playerId);
        buffer.writeBoolean(packet.meditating);
    }

    public static SyncMeditationStatePacket decode(FriendlyByteBuf buffer) {
        return new SyncMeditationStatePacket(buffer.readUUID(), buffer.readBoolean());
    }

    public static void handle(SyncMeditationStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientMeditationManager.setMeditating(packet.playerId, packet.meditating)
        );
    }
}
