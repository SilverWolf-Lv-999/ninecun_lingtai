package net.seraphina.ninecun_lingtai.common.item;

import net.seraphina.ninecun_lingtai.api.technique.TechniqueClientData;
import net.seraphina.ninecun_lingtai.client.render.TechniqueItemCustomModel;
import net.seraphina.ninecun_lingtai.common.data.TechniqueManager;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.player.PlayerImprintedTechniqueData;
import net.seraphina.ninecun_lingtai.utility.TechniqueItemUtility;
import net.seraphina.ninecun_lingtai.utility.TechniqueUtility;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
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
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

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

        ServerPlayer serverPlayer = (ServerPlayer) player;
        Optional<ResourceLocation> techniqueId = TechniqueItemUtility.getTechniqueId(itemStack);
        if (techniqueId.isEmpty()) {
            List<TechniqueClientData> imprintableTechniques = TechniqueUtility
                    .getImprintableTechniques(serverPlayer)
                    .stream()
                    .map(TechniqueClientData::fromDefinition)
                    .toList();
            if (imprintableTechniques.isEmpty()) {
                player.displayClientMessage(
                        Component.translatable("message.ninecun_lingtai.no_learned_techniques_to_imprint"),
                        true
                );
                return InteractionResultHolder.fail(itemStack);
            }
            ModNetwork.sendOpenTechniqueImprintScreen(serverPlayer, hand, imprintableTechniques);
            return InteractionResultHolder.consume(itemStack);
        }
        return TechniqueManager.INSTANCE.getTechnique(techniqueId.get())
                .map(technique -> {
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
    public void appendHoverText(
            ItemStack itemStack,
            Level level,
            List<Component> tooltip,
            TooltipFlag tooltipFlag
    ) {
        Optional<ResourceLocation> techniqueId = TechniqueItemUtility.getTechniqueId(itemStack);
        if (techniqueId.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.ninecun_lingtai.empty_jade_slip")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        Optional<String> techniqueName = TechniqueItemUtility.getTechniqueName(itemStack);
        tooltip.add(Component.literal(techniqueName.orElse(techniqueId.get().toString()))
                .withStyle(ChatFormatting.AQUA));
        TechniqueItemUtility.getTechniqueDescription(itemStack)
                .filter(description -> !description.isBlank())
                .ifPresent(description -> tooltip.add(
                        Component.literal(description).withStyle(ChatFormatting.GRAY)
                ));
        if (tooltipFlag.isAdvanced()) {
            tooltip.add(Component.literal(techniqueId.get().toString())
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return TechniqueItemCustomModel.INSTANCE;
            }
        });
    }
}
