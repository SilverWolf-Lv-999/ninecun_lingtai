package com.seraphina.ninecun_lingtai.common.item;

import com.seraphina.ninecun_lingtai.api.technique.TechniqueClientData;
import com.seraphina.ninecun_lingtai.common.data.TechniqueManager;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.player.PlayerImprintedTechniqueData;
import com.seraphina.ninecun_lingtai.utility.TechniqueItemUtility;
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

public final class TechniqueItem extends Item {
    public TechniqueItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(itemStack);
        }

        Optional<ResourceLocation> techniqueId = TechniqueItemUtility.getTechniqueId(itemStack);
        if (techniqueId.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.invalid_technique_item"),
                    true
            );
            return InteractionResultHolder.fail(itemStack);
        }
        return TechniqueManager.INSTANCE.getTechnique(techniqueId.get())
                .map(technique -> {
                    ServerPlayer serverPlayer = (ServerPlayer) player;
                    ModNetwork.sendOpenTechniqueScreen(
                            serverPlayer,
                            TechniqueClientData.fromDefinition(technique),
                            PlayerImprintedTechniqueData.hasTechnique(serverPlayer, technique.id())
                    );
                    return InteractionResultHolder.consume(itemStack);
                })
                .orElseGet(() -> {
                    player.displayClientMessage(
                            Component.translatable("message.ninecun_lingtai.missing_technique"),
                            true
                    );
                    return InteractionResultHolder.fail(itemStack);
                });
    }

    @Override
    public Component getName(ItemStack itemStack) {
        Optional<String> techniqueName = TechniqueItemUtility.getTechniqueName(itemStack);
        return techniqueName.<Component>map(Component::literal)
                .orElseGet(() -> super.getName(itemStack));
    }

    @Override
    public void appendHoverText(
            ItemStack itemStack,
            Level level,
            List<Component> tooltip,
            TooltipFlag tooltipFlag
    ) {
        TechniqueItemUtility.getTechniqueDescription(itemStack)
                .filter(description -> !description.isBlank())
                .ifPresent(description -> tooltip.add(
                        Component.literal(description).withStyle(ChatFormatting.GRAY)
                ));
        if (tooltipFlag.isAdvanced()) {
            TechniqueItemUtility.getTechniqueId(itemStack).ifPresent(techniqueId -> tooltip.add(
                    Component.literal(techniqueId.toString()).withStyle(ChatFormatting.DARK_GRAY)
            ));
        }
    }
}
