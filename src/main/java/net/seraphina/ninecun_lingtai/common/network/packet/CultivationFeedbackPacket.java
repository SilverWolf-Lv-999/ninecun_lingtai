package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.client.player.ClientCultivationFeedbackManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record CultivationFeedbackPacket(boolean success, Component message) {
    public static void encode(CultivationFeedbackPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.success);
        buffer.writeComponent(packet.message);
    }

    public static CultivationFeedbackPacket decode(FriendlyByteBuf buffer) {
        return new CultivationFeedbackPacket(buffer.readBoolean(), buffer.readComponent());
    }

    public static void handle(
            CultivationFeedbackPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientCultivationFeedbackManager.accept(packet.success, packet.message)
        );
    }
}
