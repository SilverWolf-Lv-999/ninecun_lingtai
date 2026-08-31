package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.common.seal.lockgrid.LockGridSessionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C→S：落子。nonce + 自增序号双重防重放；服务端逐步判定，答案不在客户端。
 * holdMillis 用于服务端判定滞涩预警（魂强 ≥ 刻印者时轻触错点可免罚一次）。
 */
public record LockGridClickPacket(long sessionNonce, int clickSequence, int nodeIndex, int holdMillis) {

    public static void encode(LockGridClickPacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.sessionNonce);
        buffer.writeVarInt(packet.clickSequence);
        buffer.writeVarInt(packet.nodeIndex);
        buffer.writeVarInt(packet.holdMillis);
    }

    public static LockGridClickPacket decode(FriendlyByteBuf buffer) {
        return new LockGridClickPacket(
                buffer.readLong(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt()
        );
    }

    public static void handle(LockGridClickPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(() -> {
            ServerPlayer player = contextSupplier.get().getSender();
            if (player != null) {
                LockGridSessionManager.handleClick(player, packet);
            }
        });
        contextSupplier.get().setPacketHandled(true);
    }
}
