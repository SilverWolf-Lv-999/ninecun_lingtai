package com.seraphina.ninecun_lingtai.common.item;

import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.network.packet.OpenDaoScrollScreenPacket;
import com.seraphina.ninecun_lingtai.utility.DaoScrollUtility;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class DaoScrollItem extends Item {
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

    public static boolean isHeldBy(ServerPlayer player) {
        return player.getMainHandItem().is(com.seraphina.ninecun_lingtai.common.registry.ModItems.DAO_SCROLL.get())
                || player.getOffhandItem().is(com.seraphina.ninecun_lingtai.common.registry.ModItems.DAO_SCROLL.get());
    }
}
