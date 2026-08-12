package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.common.player.PlayerMeditationManager;
import net.seraphina.ninecun_lingtai.common.player.PlayerSoulRoamingManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public record SetMeditationStatePacket(
        boolean meditating,
        Optional<ResourceLocation> techniqueId,
        boolean cultivatingRealm
) {
    public SetMeditationStatePacket(boolean meditating) {
        this(meditating, Optional.empty(), false);
    }

    public SetMeditationStatePacket(boolean meditating, Optional<ResourceLocation> techniqueId) {
        this(meditating, techniqueId, false);
    }

    public static void encode(SetMeditationStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.meditating);
        buffer.writeBoolean(packet.cultivatingRealm);
        buffer.writeBoolean(packet.techniqueId.isPresent());
        packet.techniqueId.ifPresent(buffer::writeResourceLocation);
    }

    public static SetMeditationStatePacket decode(FriendlyByteBuf buffer) {
        boolean meditating = buffer.readBoolean();
        boolean cultivatingRealm = buffer.readBoolean();
        Optional<ResourceLocation> techniqueId = buffer.readBoolean()
                ? Optional.of(buffer.readResourceLocation())
                : Optional.empty();
        return new SetMeditationStatePacket(meditating, techniqueId, cultivatingRealm);
    }

    public static void handle(SetMeditationStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player == null) {
            return;
        }
        if (PlayerSoulRoamingManager.isSoulRoaming(player)) {
            return;
        }
        if (packet.meditating) {
            PlayerMeditationManager.startMeditating(
                    player,
                    packet.techniqueId.orElse(null),
                    packet.cultivatingRealm
            );
        } else {
            PlayerMeditationManager.stopMeditating(player);
        }
    }
}
