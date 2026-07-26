package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.common.player.PlayerMeditationManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public record SetMeditationStatePacket(boolean meditating, Optional<ResourceLocation> techniqueId) {
    public SetMeditationStatePacket(boolean meditating) {
        this(meditating, Optional.empty());
    }

    public static void encode(SetMeditationStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.meditating);
        buffer.writeBoolean(packet.techniqueId.isPresent());
        packet.techniqueId.ifPresent(buffer::writeResourceLocation);
    }

    public static SetMeditationStatePacket decode(FriendlyByteBuf buffer) {
        boolean meditating = buffer.readBoolean();
        Optional<ResourceLocation> techniqueId = buffer.readBoolean()
                ? Optional.of(buffer.readResourceLocation())
                : Optional.empty();
        return new SetMeditationStatePacket(meditating, techniqueId);
    }

    public static void handle(SetMeditationStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player == null) {
            return;
        }
        if (packet.meditating) {
            PlayerMeditationManager.startMeditating(player, packet.techniqueId.orElse(null));
        } else {
            PlayerMeditationManager.stopMeditating(player);
        }
    }
}
