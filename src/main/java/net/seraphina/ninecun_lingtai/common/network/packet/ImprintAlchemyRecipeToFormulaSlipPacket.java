package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeDefinition;
import net.seraphina.ninecun_lingtai.common.data.AlchemyRecipeManager;
import net.seraphina.ninecun_lingtai.common.player.PlayerLearnedAlchemyRecipeData;
import net.seraphina.ninecun_lingtai.common.player.PlayerSoulRoamingManager;
import net.seraphina.ninecun_lingtai.utility.AlchemyRecipeItemUtility;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public record ImprintAlchemyRecipeToFormulaSlipPacket(
        ResourceLocation recipeId,
        InteractionHand hand
) {
    public static void encode(
            ImprintAlchemyRecipeToFormulaSlipPacket packet,
            FriendlyByteBuf buffer
    ) {
        buffer.writeResourceLocation(packet.recipeId);
        buffer.writeEnum(packet.hand);
    }

    public static ImprintAlchemyRecipeToFormulaSlipPacket decode(FriendlyByteBuf buffer) {
        return new ImprintAlchemyRecipeToFormulaSlipPacket(
                buffer.readResourceLocation(),
                buffer.readEnum(InteractionHand.class)
        );
    }

    public static void handle(
            ImprintAlchemyRecipeToFormulaSlipPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player == null || PlayerSoulRoamingManager.isSoulRoaming(player)) {
            return;
        }
        Optional<AlchemyRecipeDefinition> recipe = AlchemyRecipeManager.INSTANCE.getRecipe(
                packet.recipeId
        );
        if (recipe.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.missing_alchemy_recipe"),
                    true
            );
            return;
        }
        if (!PlayerLearnedAlchemyRecipeData.hasRecipe(player, packet.recipeId)) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.alchemy_recipe_not_learned"),
                    true
            );
            return;
        }
        ItemStack itemStack = player.getItemInHand(packet.hand);
        if (!AlchemyRecipeItemUtility.isEmptyAlchemyRecipe(itemStack)) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.empty_alchemy_recipe_not_held"),
                    true
            );
            return;
        }

        AlchemyRecipeItemUtility.imprintRecipe(itemStack, recipe.get());
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        player.displayClientMessage(
                Component.translatable("message.ninecun_lingtai.alchemy_recipe_imprinted_to_formula_slip"),
                true
        );
    }
}
