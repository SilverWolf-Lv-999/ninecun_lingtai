package net.seraphina.ninecun_lingtai.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.seraphina.ninecun_lingtai.api.daotitle.DaoTitleSuffix;
import net.seraphina.ninecun_lingtai.common.player.PlayerDaoTitleData;
import net.seraphina.ninecun_lingtai.utility.DaoTitleUtility;

import java.util.function.Supplier;

public record SetDaoTitlePacket(String customName, int suffixIndex) {
    public static void encode(SetDaoTitlePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.customName == null ? "" : packet.customName, 64);
        buffer.writeVarInt(packet.suffixIndex);
    }

    public static SetDaoTitlePacket decode(FriendlyByteBuf buffer) {
        return new SetDaoTitlePacket(buffer.readUtf(64), buffer.readVarInt());
    }

    public static void handle(SetDaoTitlePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null && DaoTitleUtility.getSnapshot(player).isUnlocked()) {
            PlayerDaoTitleData.set(player, packet.customName, DaoTitleUtility.getSuffix(packet.suffixIndex));
        }
        context.setPacketHandled(true);
    }
}
