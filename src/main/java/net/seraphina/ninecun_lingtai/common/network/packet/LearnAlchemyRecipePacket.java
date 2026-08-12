package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.common.data.AlchemyRecipeManager;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.player.PlayerLearnedAlchemyRecipeData;
import net.seraphina.ninecun_lingtai.common.player.PlayerSoulRoamingManager;
import net.seraphina.ninecun_lingtai.utility.AlchemyRecipeItemUtility;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record LearnAlchemyRecipePacket(ResourceLocation recipeId) {
    public static void encode(LearnAlchemyRecipePacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.recipeId);
    }

    public static LearnAlchemyRecipePacket decode(FriendlyByteBuf buffer) {
        return new LearnAlchemyRecipePacket(buffer.readResourceLocation());
    }

    public static void handle(
            LearnAlchemyRecipePacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player == null || PlayerSoulRoamingManager.isSoulRoaming(player)) {
            return;
        }
        if (AlchemyRecipeManager.INSTANCE.getRecipe(packet.recipeId).isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.missing_alchemy_recipe"),
                    true
            );
            ModNetwork.sendAlchemyRecipes(player);
            return;
        }
        boolean holdingRecipe = AlchemyRecipeItemUtility.isRecipe(
                player.getMainHandItem(),
                packet.recipeId
        ) || AlchemyRecipeItemUtility.isRecipe(player.getOffhandItem(), packet.recipeId);
        if (!holdingRecipe) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.alchemy_recipe_not_held"),
                    true
            );
            ModNetwork.sendAlchemyRecipes(player);
            return;
        }

        boolean added = PlayerLearnedAlchemyRecipeData.learn(player, packet.recipeId);
        player.displayClientMessage(
                Component.translatable(added
                        ? "message.ninecun_lingtai.alchemy_recipe_learned"
                        : "message.ninecun_lingtai.alchemy_recipe_already_learned"),
                true
        );
        ModNetwork.sendAlchemyRecipes(player);
    }
}
