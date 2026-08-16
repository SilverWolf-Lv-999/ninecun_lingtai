package net.seraphina.ninecun_lingtai.common.item;

import net.seraphina.ninecun_lingtai.client.SeraFont;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.realm.ThreeThousandRealmManager;
import net.seraphina.ninecun_lingtai.utility.DaoScrollUtility;
import net.minecraft.client.gui.Font;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.seraphina.ninecun_lingtai.common.registry.ModItems;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.client.extensions.common.IClientItemExtensions.FontContext;

import java.util.function.Consumer;

public final class DaoScrollItem extends LuminousItem {
    public DaoScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack itemStack,
            Player player,
            LivingEntity target,
            InteractionHand hand
    ) {
        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        boolean editingSelf = player.isShiftKeyDown();
        LivingEntity editedEntity = editingSelf ? player : target;
        ModNetwork.sendOpenDaoScrollScreen(
                serverPlayer,
                DaoScrollUtility.capture(editedEntity),
                editingSelf
        );
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(itemStack, true);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            ThreeThousandRealmManager.get(serverPlayer.server).openRealmCatalog(serverPlayer);
            return InteractionResultHolder.consume(itemStack);
        }
        return InteractionResultHolder.pass(itemStack);
    }

    public static boolean isHeldBy(ServerPlayer player) {
        return player.getMainHandItem().is(ModItems.DAO_SCROLL.get())
                || player.getOffhandItem().is(ModItems.DAO_SCROLL.get());
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public Font getFont(ItemStack itemStack, FontContext context) {
                return SeraFont.get(ItemFontStyle.DAO_SCROLL);
            }
        });
    }
}
