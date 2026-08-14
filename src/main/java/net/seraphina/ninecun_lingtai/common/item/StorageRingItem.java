package net.seraphina.ninecun_lingtai.common.item;

import net.seraphina.ninecun_lingtai.client.SeraFont;
import net.seraphina.ninecun_lingtai.common.ring.RingStorageManager;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.client.extensions.common.IClientItemExtensions.FontContext;

import java.util.List;
import java.util.function.Consumer;

public final class StorageRingItem extends Item {
    public StorageRingItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            RingStorageManager.open(serverPlayer, itemStack);
        }
        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide);
    }

    @Override
    public void appendHoverText(
            ItemStack itemStack,
            Level level,
            List<Component> tooltipComponents,
            TooltipFlag tooltipFlag
    ) {
        tooltipComponents.add(Component.translatable("tooltip.ninecun_lingtai.storage_ring"));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public Font getFont(ItemStack itemStack, FontContext context) {
                return SeraFont.get(ItemFontStyle.STORAGE_RING);
            }
        });
    }
}
