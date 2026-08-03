package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class RequestAlchemyRecipeLibraryPacket {
    public static final RequestAlchemyRecipeLibraryPacket INSTANCE =
            new RequestAlchemyRecipeLibraryPacket();

    private RequestAlchemyRecipeLibraryPacket() {
    }

    public static void encode(RequestAlchemyRecipeLibraryPacket packet, FriendlyByteBuf buffer) {
    }

    public static RequestAlchemyRecipeLibraryPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(
            RequestAlchemyRecipeLibraryPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null) {
            ModNetwork.sendAlchemyRecipes(player);
        }
    }
}
