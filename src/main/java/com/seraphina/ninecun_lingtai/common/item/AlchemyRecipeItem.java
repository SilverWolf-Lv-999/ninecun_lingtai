package com.seraphina.ninecun_lingtai.common.item;

import com.seraphina.ninecun_lingtai.common.data.AlchemyRecipeManager;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.player.PlayerLearnedAlchemyRecipeData;
import com.seraphina.ninecun_lingtai.utility.AlchemyRecipeItemUtility;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public final class AlchemyRecipeItem extends Item {
    public AlchemyRecipeItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack itemStack) {
        return AlchemyRecipeItemUtility.getRecipeName(itemStack)
                .<Component>map(recipeName -> Component.literal(recipeName + "•丹方"))
                .orElseGet(() -> super.getName(itemStack));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(itemStack);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(itemStack);
        }

        Optional<ResourceLocation> recipeId = AlchemyRecipeItemUtility.getRecipeId(itemStack);
        if (recipeId.isEmpty()) {
            List<com.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeClientData> learnedRecipes =
                    PlayerLearnedAlchemyRecipeData.getRecipes(serverPlayer).stream()
                            .flatMap(id -> AlchemyRecipeManager.INSTANCE.getRecipe(id).stream())
                            .map(com.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeClientData::fromDefinition)
                            .toList();
            if (learnedRecipes.isEmpty()) {
                player.displayClientMessage(
                        Component.translatable(
                                "message.ninecun_lingtai.no_learned_alchemy_recipes_to_imprint"
                        ),
                        true
                );
                return InteractionResultHolder.fail(itemStack);
            }
            ModNetwork.sendOpenAlchemyRecipeImprintScreen(serverPlayer, hand, learnedRecipes);
            return InteractionResultHolder.consume(itemStack);
        }
        return AlchemyRecipeManager.INSTANCE.getRecipe(recipeId.get())
                .map(recipe -> {
                    ModNetwork.sendOpenAlchemyRecipeScreen(
                            serverPlayer,
                            recipe,
                            PlayerLearnedAlchemyRecipeData.hasRecipe(serverPlayer, recipe.id())
                    );
                    return InteractionResultHolder.consume(itemStack);
                })
                .orElseGet(() -> {
                    player.displayClientMessage(
                            Component.translatable("message.ninecun_lingtai.missing_alchemy_recipe"),
                            true
                    );
                    return InteractionResultHolder.fail(itemStack);
                });
    }

    @Override
    public void appendHoverText(
            ItemStack itemStack,
            Level level,
            List<Component> tooltip,
            TooltipFlag tooltipFlag
    ) {
        Optional<ResourceLocation> recipeId = AlchemyRecipeItemUtility.getRecipeId(itemStack);
        if (recipeId.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.ninecun_lingtai.empty_alchemy_recipe")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        tooltip.add(Component.translatable("tooltip.ninecun_lingtai.alchemy_recipe")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(
                AlchemyRecipeItemUtility.getRecipeName(itemStack).orElse(recipeId.get().toString())
        ).withStyle(ChatFormatting.AQUA));
        AlchemyRecipeItemUtility.getRecipeDescription(itemStack)
                .filter(description -> !description.isBlank())
                .ifPresent(description -> tooltip.add(
                        Component.literal(description).withStyle(ChatFormatting.GRAY)
                ));
        if (tooltipFlag.isAdvanced()) {
            tooltip.add(Component.literal(recipeId.get().toString())
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
