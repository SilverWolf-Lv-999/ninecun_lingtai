package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.client.player.ClientBodyShieldManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record TriggerBodyShieldPacket(int entityId) {
    public static void encode(TriggerBodyShieldPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
    }

    public static TriggerBodyShieldPacket decode(FriendlyByteBuf buffer) {
        return new TriggerBodyShieldPacket(buffer.readVarInt());
    }

    public static void handle(TriggerBodyShieldPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientBodyShieldManager.trigger(packet.entityId)
        );
    }
}
