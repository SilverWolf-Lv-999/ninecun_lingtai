package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.common.player.PlayerAptitudeData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public record SubmitAptitudePacket(int elementMask, float lengthInches) {
    public static void encode(SubmitAptitudePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.elementMask);
        buffer.writeFloat(packet.lengthInches);
    }

    public static SubmitAptitudePacket decode(FriendlyByteBuf buffer) {
        return new SubmitAptitudePacket(buffer.readVarInt(), buffer.readFloat());
    }

    public static void handle(SubmitAptitudePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player == null || PlayerAptitudeData.getSpiritualRoot(player).isPresent()) {
            return;
        }
        Optional<SpiritualRoot> spiritualRoot = SpiritualRoot.fromElementMask(
                packet.elementMask,
                packet.lengthInches
        );
        spiritualRoot.ifPresent(root -> PlayerAptitudeData.setSpiritualRoot(player, root));
    }
}
