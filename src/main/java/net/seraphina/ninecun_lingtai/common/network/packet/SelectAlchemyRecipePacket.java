package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.common.block.entity.AlchemyFurnaceBlockEntity;
import net.seraphina.ninecun_lingtai.common.menu.AlchemyFurnaceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SelectAlchemyRecipePacket(
        BlockPos blockPosition,
        ResourceLocation recipeId
) {
    public static void encode(SelectAlchemyRecipePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.blockPosition);
        buffer.writeResourceLocation(packet.recipeId);
    }

    public static SelectAlchemyRecipePacket decode(FriendlyByteBuf buffer) {
        return new SelectAlchemyRecipePacket(
                buffer.readBlockPos(),
                buffer.readResourceLocation()
        );
    }

    public static void handle(
            SelectAlchemyRecipePacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player == null
                || !(player.containerMenu instanceof AlchemyFurnaceMenu menu)
                || !menu.getBlockPosition().equals(packet.blockPosition)
                || !menu.stillValid(player)
                || !(player.level().getBlockEntity(packet.blockPosition)
                instanceof AlchemyFurnaceBlockEntity furnace)) {
            return;
        }
        furnace.selectRecipe(player, packet.recipeId);
    }
}
