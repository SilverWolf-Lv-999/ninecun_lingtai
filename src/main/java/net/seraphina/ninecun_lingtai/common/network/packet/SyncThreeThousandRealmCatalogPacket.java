package net.seraphina.ninecun_lingtai.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.seraphina.ninecun_lingtai.client.screen.ClientThreeThousandRealmScreenManager;
import net.seraphina.ninecun_lingtai.common.network.ThreeThousandRealmPacketUtility;
import net.seraphina.ninecun_lingtai.common.realm.ThreeThousandRealmCatalogSnapshot;

import java.util.function.Supplier;

public record SyncThreeThousandRealmCatalogPacket(ThreeThousandRealmCatalogSnapshot snapshot) {
    public static void encode(SyncThreeThousandRealmCatalogPacket packet, FriendlyByteBuf buffer) {
        ThreeThousandRealmPacketUtility.writeCatalogSnapshot(buffer, packet.snapshot());
    }

    public static SyncThreeThousandRealmCatalogPacket decode(FriendlyByteBuf buffer) {
        return new SyncThreeThousandRealmCatalogPacket(
                ThreeThousandRealmPacketUtility.readCatalogSnapshot(buffer)
        );
    }

    public static void handle(
            SyncThreeThousandRealmCatalogPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientThreeThousandRealmScreenManager.update(packet.snapshot())
        );
    }
}
