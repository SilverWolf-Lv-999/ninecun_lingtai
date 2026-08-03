package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeClientData;
import com.seraphina.ninecun_lingtai.client.player.ClientAlchemyRecipeManager;
import com.seraphina.ninecun_lingtai.utility.AlchemyRecipeNetworkUtility;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public record SyncAlchemyRecipesPacket(
        List<AlchemyRecipeClientData> recipes,
        Set<net.minecraft.resources.ResourceLocation> learnedRecipeIds
) {
    public SyncAlchemyRecipesPacket {
        recipes = List.copyOf(recipes);
        learnedRecipeIds = Set.copyOf(learnedRecipeIds);
    }

    public static void encode(SyncAlchemyRecipesPacket packet, FriendlyByteBuf buffer) {
        AlchemyRecipeNetworkUtility.writeRecipes(buffer, packet.recipes);
        if (packet.learnedRecipeIds.size() > AlchemyRecipeNetworkUtility.getMaximumRecipes()) {
            throw new IllegalArgumentException("Too many learned alchemy recipes to synchronize");
        }
        buffer.writeVarInt(packet.learnedRecipeIds.size());
        packet.learnedRecipeIds.stream().sorted().forEach(buffer::writeResourceLocation);
    }

    public static SyncAlchemyRecipesPacket decode(FriendlyByteBuf buffer) {
        List<AlchemyRecipeClientData> recipes = AlchemyRecipeNetworkUtility.readRecipes(buffer);
        int learnedRecipeCount = buffer.readVarInt();
        if (learnedRecipeCount < 0
                || learnedRecipeCount > AlchemyRecipeNetworkUtility.getMaximumRecipes()) {
            throw new IllegalArgumentException(
                    "Invalid synchronized learned alchemy recipe count: " + learnedRecipeCount
            );
        }
        Set<net.minecraft.resources.ResourceLocation> learnedRecipeIds = new HashSet<>();
        for (int recipeIndex = 0; recipeIndex < learnedRecipeCount; recipeIndex++) {
            learnedRecipeIds.add(buffer.readResourceLocation());
        }
        return new SyncAlchemyRecipesPacket(recipes, learnedRecipeIds);
    }

    public static void handle(
            SyncAlchemyRecipesPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientAlchemyRecipeManager.acceptRecipes(
                        packet.recipes,
                        packet.learnedRecipeIds
                )
        );
    }
}
