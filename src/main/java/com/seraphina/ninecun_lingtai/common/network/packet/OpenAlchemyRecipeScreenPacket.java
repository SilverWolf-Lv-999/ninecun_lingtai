package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeClientData;
import com.seraphina.ninecun_lingtai.client.player.ClientAlchemyRecipeManager;
import com.seraphina.ninecun_lingtai.utility.AlchemyRecipeNetworkUtility;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record OpenAlchemyRecipeScreenPacket(
        AlchemyRecipeClientData recipe,
        boolean learned
) {
    public static void encode(OpenAlchemyRecipeScreenPacket packet, FriendlyByteBuf buffer) {
        AlchemyRecipeNetworkUtility.writeRecipe(buffer, packet.recipe);
        buffer.writeBoolean(packet.learned);
    }

    public static OpenAlchemyRecipeScreenPacket decode(FriendlyByteBuf buffer) {
        return new OpenAlchemyRecipeScreenPacket(
                AlchemyRecipeNetworkUtility.readRecipe(buffer),
                buffer.readBoolean()
        );
    }

    public static void handle(
            OpenAlchemyRecipeScreenPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientAlchemyRecipeManager.openRecipe(packet.recipe, packet.learned)
        );
    }
}
