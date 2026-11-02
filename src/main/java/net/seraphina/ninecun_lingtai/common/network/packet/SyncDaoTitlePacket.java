package net.seraphina.ninecun_lingtai.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.daotitle.DaoTitleColors;
import net.seraphina.ninecun_lingtai.api.daotitle.DaoTitleSuffix;
import net.seraphina.ninecun_lingtai.client.player.ClientDaoTitleManager;
import net.seraphina.ninecun_lingtai.common.player.DaoTitleSnapshot;
import net.seraphina.ninecun_lingtai.utility.DaoTitleUtility;

import java.util.UUID;
import java.util.function.Supplier;

public record SyncDaoTitlePacket(UUID playerId, DaoTitleSnapshot snapshot) {
    public static void encode(SyncDaoTitlePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.playerId);
        buffer.writeUtf(packet.snapshot.customName(), 64);
        buffer.writeVarInt(packet.snapshot.immortalSuffix().ordinal());
        buffer.writeVarInt(packet.snapshot.realm() == null ? -1 : packet.snapshot.realm().ordinal());
        buffer.writeInt(packet.snapshot.colors().firstColor());
        buffer.writeInt(packet.snapshot.colors().secondColor());
    }

    public static SyncDaoTitlePacket decode(FriendlyByteBuf buffer) {
        UUID playerId = buffer.readUUID();
        String customName = buffer.readUtf(64);
        DaoTitleSuffix suffix = DaoTitleUtility.getSuffix(buffer.readVarInt());
        int realmIndex = buffer.readVarInt();
        CultivationRealm[] realms = CultivationRealm.values();
        CultivationRealm realm = realmIndex >= 0 && realmIndex < realms.length ? realms[realmIndex] : null;
        DaoTitleColors colors = new DaoTitleColors(buffer.readInt(), buffer.readInt());
        return new SyncDaoTitlePacket(playerId, new DaoTitleSnapshot(customName, suffix, realm, colors));
    }

    public static void handle(SyncDaoTitlePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientDaoTitleManager.accept(packet.playerId, packet.snapshot)));
        context.setPacketHandled(true);
    }
}
