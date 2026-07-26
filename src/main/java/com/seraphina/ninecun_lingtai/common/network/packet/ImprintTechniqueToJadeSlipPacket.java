package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.api.technique.TechniqueDefinition;
import com.seraphina.ninecun_lingtai.common.data.TechniqueManager;
import com.seraphina.ninecun_lingtai.utility.TechniqueItemUtility;
import com.seraphina.ninecun_lingtai.utility.TechniqueUtility;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public record ImprintTechniqueToJadeSlipPacket(
        ResourceLocation techniqueId,
        InteractionHand hand
) {
    public static void encode(
            ImprintTechniqueToJadeSlipPacket packet,
            FriendlyByteBuf buffer
    ) {
        buffer.writeResourceLocation(packet.techniqueId);
        buffer.writeEnum(packet.hand);
    }

    public static ImprintTechniqueToJadeSlipPacket decode(FriendlyByteBuf buffer) {
        return new ImprintTechniqueToJadeSlipPacket(
                buffer.readResourceLocation(),
                buffer.readEnum(InteractionHand.class)
        );
    }

    public static void handle(
            ImprintTechniqueToJadeSlipPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player == null) {
            return;
        }
        Optional<TechniqueDefinition> technique = TechniqueManager.INSTANCE.getTechnique(
                packet.techniqueId
        );
        if (technique.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.missing_technique"),
                    true
            );
            return;
        }
        if (!TechniqueUtility.canImprintToJadeSlip(player, packet.techniqueId)) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.technique_not_learned"),
                    true
            );
            return;
        }
        ItemStack itemStack = player.getItemInHand(packet.hand);
        if (!TechniqueItemUtility.isEmptyJadeSlip(itemStack)) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.empty_jade_slip_not_held"),
                    true
            );
            return;
        }

        TechniqueItemUtility.imprintTechnique(itemStack, technique.get());
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        player.displayClientMessage(
                Component.translatable("message.ninecun_lingtai.technique_imprinted_to_jade_slip"),
                true
        );
    }
}
