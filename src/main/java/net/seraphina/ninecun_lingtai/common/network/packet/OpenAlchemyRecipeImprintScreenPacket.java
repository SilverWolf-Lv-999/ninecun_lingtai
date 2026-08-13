package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeClientData;
import net.seraphina.ninecun_lingtai.client.player.ClientAlchemyRecipeManager;
import net.seraphina.ninecun_lingtai.utility.AlchemyRecipeNetworkUtility;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public record OpenAlchemyRecipeImprintScreenPacket(
        InteractionHand hand,
        List<AlchemyRecipeClientData> recipes
) {
    public OpenAlchemyRecipeImprintScreenPacket {
        recipes = List.copyOf(recipes);
    }

    public static void encode(
            OpenAlchemyRecipeImprintScreenPacket packet,
            FriendlyByteBuf buffer
    ) {
        buffer.writeEnum(packet.hand);
        AlchemyRecipeNetworkUtility.writeRecipes(buffer, packet.recipes);
    }

    public static OpenAlchemyRecipeImprintScreenPacket decode(FriendlyByteBuf buffer) {
        return new OpenAlchemyRecipeImprintScreenPacket(
                buffer.readEnum(InteractionHand.class),
                AlchemyRecipeNetworkUtility.readRecipes(buffer)
        );
    }

    public static void handle(
            OpenAlchemyRecipeImprintScreenPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientAlchemyRecipeManager.openImprintScreen(packet.hand, packet.recipes)
        );
    }
}
