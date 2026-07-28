package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.client.notification.ClientNotificationManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ShowNotificationPacket(Component title, Component content, int durationTicks) {
    public static void encode(ShowNotificationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeComponent(packet.title);
        buffer.writeComponent(packet.content);
        buffer.writeVarInt(packet.durationTicks);
    }

    public static ShowNotificationPacket decode(FriendlyByteBuf buffer) {
        return new ShowNotificationPacket(
                buffer.readComponent(),
                buffer.readComponent(),
                buffer.readVarInt()
        );
    }

    public static void handle(
            ShowNotificationPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientNotificationManager.accept(
                        packet.title,
                        packet.content,
                        packet.durationTicks
                )
        );
    }
}
