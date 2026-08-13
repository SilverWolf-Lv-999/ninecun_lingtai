package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.api.alchemy.HeatControlMode;
import net.seraphina.ninecun_lingtai.common.block.entity.AlchemyFurnaceBlockEntity;
import net.seraphina.ninecun_lingtai.common.menu.AlchemyFurnaceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record MaintainHeatPacket(BlockPos blockPosition, HeatControlMode mode) {
    public static void encode(MaintainHeatPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.blockPosition);
        buffer.writeEnum(packet.mode);
    }

    public static MaintainHeatPacket decode(FriendlyByteBuf buffer) {
        return new MaintainHeatPacket(
                buffer.readBlockPos(),
                buffer.readEnum(HeatControlMode.class)
        );
    }

    public static void handle(
            MaintainHeatPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player == null
                || !(player.containerMenu instanceof AlchemyFurnaceMenu menu)
                || !menu.getBlockPosition().equals(packet.blockPosition)
                || !menu.stillValid(player)
                || !(player.level().getBlockEntity(packet.blockPosition) instanceof AlchemyFurnaceBlockEntity furnace)) {
            return;
        }
        furnace.setHeatControl(player, packet.mode);
    }
}
