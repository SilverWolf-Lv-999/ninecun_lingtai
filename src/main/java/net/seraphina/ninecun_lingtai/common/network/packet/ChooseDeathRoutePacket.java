package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.common.death.DeathRoute;
import net.seraphina.ninecun_lingtai.common.death.DeathRouteManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C→S：死亡路线选择。nonce 防重放/防伪造不可选路线，服务端二次校验资格。
 */
public record ChooseDeathRoutePacket(long sessionNonce, int routeOrdinal) {

    public static void encode(ChooseDeathRoutePacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.sessionNonce);
        buffer.writeVarInt(packet.routeOrdinal);
    }

    public static ChooseDeathRoutePacket decode(FriendlyByteBuf buffer) {
        return new ChooseDeathRoutePacket(buffer.readLong(), buffer.readVarInt());
    }

    public static void handle(ChooseDeathRoutePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(() -> {
            ServerPlayer player = contextSupplier.get().getSender();
            if (player != null) {
                DeathRouteManager.handleChoice(
                        player,
                        packet.sessionNonce,
                        DeathRoute.fromOrdinal(packet.routeOrdinal)
                );
            }
        });
        contextSupplier.get().setPacketHandled(true);
    }
}
