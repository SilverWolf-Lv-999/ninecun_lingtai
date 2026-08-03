package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.common.block.entity.AlchemyFurnaceBlockEntity;
import com.seraphina.ninecun_lingtai.common.menu.AlchemyFurnaceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record AlchemyFurnaceActionPacket(BlockPos blockPosition, Action action) {
    public static void encode(AlchemyFurnaceActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.blockPosition);
        buffer.writeEnum(packet.action);
    }

    public static AlchemyFurnaceActionPacket decode(FriendlyByteBuf buffer) {
        return new AlchemyFurnaceActionPacket(
                buffer.readBlockPos(),
                buffer.readEnum(Action.class)
        );
    }

    public static void handle(
            AlchemyFurnaceActionPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player == null
                || !(player.containerMenu instanceof AlchemyFurnaceMenu menu)
                || !menu.getBlockPosition().equals(packet.blockPosition)
                || !(player.level().getBlockEntity(packet.blockPosition) instanceof AlchemyFurnaceBlockEntity furnace)) {
            return;
        }
        furnace.handleAction(player, packet.action);
    }

    public enum Action {
        START_REFINING,
        FINISH_REFINING
    }
}
