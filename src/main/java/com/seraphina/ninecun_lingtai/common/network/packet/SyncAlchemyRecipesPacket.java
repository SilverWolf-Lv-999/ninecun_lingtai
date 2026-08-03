package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeClientData;
import com.seraphina.ninecun_lingtai.client.player.ClientAlchemyRecipeManager;
import com.seraphina.ninecun_lingtai.utility.AlchemyRecipeNetworkUtility;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public record SyncAlchemyRecipesPacket(List<AlchemyRecipeClientData> recipes) {
    public SyncAlchemyRecipesPacket {
        recipes = List.copyOf(recipes);
    }

    public static void encode(SyncAlchemyRecipesPacket packet, FriendlyByteBuf buffer) {
        AlchemyRecipeNetworkUtility.writeRecipes(buffer, packet.recipes);
    }

    public static SyncAlchemyRecipesPacket decode(FriendlyByteBuf buffer) {
        return new SyncAlchemyRecipesPacket(AlchemyRecipeNetworkUtility.readRecipes(buffer));
    }

    public static void handle(
            SyncAlchemyRecipesPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientAlchemyRecipeManager.acceptRecipes(packet.recipes)
        );
    }
}
