package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.client.player.ClientPossessionManager;
import net.seraphina.ninecun_lingtai.common.player.PossessionSyncData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;
import java.util.UUID;

public record SyncPossessionPacket(PossessionSyncData syncData) {
    public static void encode(SyncPossessionPacket packet, FriendlyByteBuf buffer) {
        PossessionSyncData syncData = packet.syncData;
        buffer.writeBoolean(syncData.active());
        buffer.writeUUID(syncData.possessorUuid());
        if (syncData.active()) {
            buffer.writeGameProfile(syncData.possessedProfile());
        }
    }

    public static SyncPossessionPacket decode(FriendlyByteBuf buffer) {
        boolean active = buffer.readBoolean();
        UUID possessorUuid = buffer.readUUID();
        if (!active) {
            return new SyncPossessionPacket(PossessionSyncData.inactive(possessorUuid));
        }
        return new SyncPossessionPacket(new PossessionSyncData(
                true,
                possessorUuid,
                buffer.readGameProfile()
        ));
    }

    public static void handle(
            SyncPossessionPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientPossessionManager.accept(packet.syncData)
        );
        context.setPacketHandled(true);
    }
}
