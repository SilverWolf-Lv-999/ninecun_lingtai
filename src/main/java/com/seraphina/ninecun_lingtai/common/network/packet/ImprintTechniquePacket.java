package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.common.data.TechniqueManager;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.player.PlayerImprintedTechniqueData;
import com.seraphina.ninecun_lingtai.common.player.PlayerSoulRoamingManager;
import com.seraphina.ninecun_lingtai.utility.TechniqueItemUtility;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ImprintTechniquePacket(ResourceLocation techniqueId) {
    public static void encode(ImprintTechniquePacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.techniqueId);
    }

    public static ImprintTechniquePacket decode(FriendlyByteBuf buffer) {
        return new ImprintTechniquePacket(buffer.readResourceLocation());
    }

    public static void handle(
            ImprintTechniquePacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player == null) {
            return;
        }
        if (PlayerSoulRoamingManager.isSoulRoaming(player)) {
            return;
        }
        if (TechniqueManager.INSTANCE.getTechnique(packet.techniqueId).isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.missing_technique"),
                    true
            );
            ModNetwork.sendTechniqueLibrary(player);
            return;
        }
        boolean holdingTechnique = TechniqueItemUtility.isTechnique(
                player.getMainHandItem(),
                packet.techniqueId
        ) || TechniqueItemUtility.isTechnique(player.getOffhandItem(), packet.techniqueId);
        if (!holdingTechnique) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.technique_not_held"),
                    true
            );
            ModNetwork.sendTechniqueLibrary(player);
            return;
        }

        boolean added = PlayerImprintedTechniqueData.imprint(player, packet.techniqueId);
        player.displayClientMessage(
                Component.translatable(added
                        ? "message.ninecun_lingtai.technique_imprinted"
                        : "message.ninecun_lingtai.technique_already_imprinted"),
                true
        );
        ModNetwork.sendTechniqueLibrary(player);
    }
}
