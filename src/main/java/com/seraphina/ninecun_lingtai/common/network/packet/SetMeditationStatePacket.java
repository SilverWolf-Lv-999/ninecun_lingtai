package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.common.player.PlayerMeditationManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SetMeditationStatePacket(boolean meditating) {
    public static void encode(SetMeditationStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.meditating);
    }

    public static SetMeditationStatePacket decode(FriendlyByteBuf buffer) {
        return new SetMeditationStatePacket(buffer.readBoolean());
    }

    public static void handle(SetMeditationStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player == null) {
            return;
        }
        if (packet.meditating) {
            PlayerMeditationManager.startMeditating(player);
        } else {
            PlayerMeditationManager.stopMeditating(player);
        }
    }
}
