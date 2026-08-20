package net.seraphina.ninecun_lingtai.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.seraphina.ninecun_lingtai.common.causality.GreatCausalityQueryService;
import net.seraphina.ninecun_lingtai.common.data.TechniqueManager;
import net.seraphina.ninecun_lingtai.common.spell.SpellCastManager;
import net.seraphina.ninecun_lingtai.utility.GreatCausalityUtility;

import java.util.UUID;
import java.util.function.Supplier;

public record RequestGreatCausalityPacket(UUID targetId) {
    public static void encode(RequestGreatCausalityPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.targetId != null);
        if (packet.targetId != null) {
            buffer.writeUUID(packet.targetId);
        }
    }

    public static RequestGreatCausalityPacket decode(FriendlyByteBuf buffer) {
        return new RequestGreatCausalityPacket(buffer.readBoolean() ? buffer.readUUID() : null);
    }

    public static void handle(
            RequestGreatCausalityPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            TechniqueManager.INSTANCE.getTechnique(GreatCausalityUtility.TECHNIQUE_ID)
                    .ifPresent(technique -> {
                        GreatCausalityQueryService.setPendingTarget(player, packet.targetId());
                        try {
                            SpellCastManager.castTechniqueFromInteraction(player, technique);
                        } finally {
                            GreatCausalityQueryService.clearPendingTarget(player);
                        }
                    });
        });
        context.setPacketHandled(true);
    }
}
