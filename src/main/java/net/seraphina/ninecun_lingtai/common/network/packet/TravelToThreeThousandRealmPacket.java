package net.seraphina.ninecun_lingtai.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.item.DaoScrollItem;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.realm.ThreeThousandRealmManager;

import java.util.Objects;
import java.util.Random;
import java.util.function.Supplier;

public record TravelToThreeThousandRealmPacket(ResourceLocation realmId) {
    public TravelToThreeThousandRealmPacket {
        Objects.requireNonNull(realmId, "realmId");
    }

    public static void encode(TravelToThreeThousandRealmPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.realmId());
    }

    public static TravelToThreeThousandRealmPacket decode(FriendlyByteBuf buffer) {
        return new TravelToThreeThousandRealmPacket(buffer.readResourceLocation());
    }

    public static void handle(
            TravelToThreeThousandRealmPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player == null) {
            return;
        }

        boolean teleported = false;
        if (DaoScrollItem.isHeldBy(player)) {
            try {
                teleported = ThreeThousandRealmManager.get(player.server).placePlayerInRealm(
                        player,
                        packet.realmId(),
                        new Random()
                );
            } catch (RuntimeException exception) {
                ModMain.LOGGER.error(
                        "Unable to place player {} in three-thousand realm {}",
                        player.getGameProfile().getName(),
                        packet.realmId(),
                        exception
                );
            }
        }
        ModNetwork.sendThreeThousandRealmTravelResult(player, teleported);
        if (!teleported) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.three_thousand_realm.unavailable"),
                    true
            );
        }
    }
}
