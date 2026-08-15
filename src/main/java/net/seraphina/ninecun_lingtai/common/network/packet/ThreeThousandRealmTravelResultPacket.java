package net.seraphina.ninecun_lingtai.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.seraphina.ninecun_lingtai.client.screen.ClientThreeThousandRealmScreenManager;

import java.util.function.Supplier;

public record ThreeThousandRealmTravelResultPacket(boolean successful) {
    public static void encode(
            ThreeThousandRealmTravelResultPacket packet,
            FriendlyByteBuf buffer
    ) {
        buffer.writeBoolean(packet.successful());
    }

    public static ThreeThousandRealmTravelResultPacket decode(FriendlyByteBuf buffer) {
        return new ThreeThousandRealmTravelResultPacket(buffer.readBoolean());
    }

    public static void handle(
            ThreeThousandRealmTravelResultPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientThreeThousandRealmScreenManager.handleTravelResult(
                        packet.successful()
                )
        );
    }
}
