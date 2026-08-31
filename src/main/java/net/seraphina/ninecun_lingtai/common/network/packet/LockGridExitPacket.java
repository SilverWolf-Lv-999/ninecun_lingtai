package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.common.seal.lockgrid.LockGridSessionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C→S：主动收手（惊扰 +1、无伤）或转入力破（潜行硬磨，必触发自毁判定）。
 */
public record LockGridExitPacket(long sessionNonce, byte modeOrdinal) {

    public enum Mode {
        RETREAT,
        FORCE_BREAK
    }

    public static LockGridExitPacket of(long sessionNonce, Mode mode) {
        return new LockGridExitPacket(sessionNonce, (byte) mode.ordinal());
    }

    public Mode mode() {
        Mode[] values = Mode.values();
        return this.modeOrdinal >= 0 && this.modeOrdinal < values.length
                ? values[this.modeOrdinal]
                : Mode.RETREAT;
    }

    public static void encode(LockGridExitPacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.sessionNonce);
        buffer.writeByte(packet.modeOrdinal);
    }

    public static LockGridExitPacket decode(FriendlyByteBuf buffer) {
        return new LockGridExitPacket(buffer.readLong(), buffer.readByte());
    }

    public static void handle(LockGridExitPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(() -> {
            ServerPlayer player = contextSupplier.get().getSender();
            if (player != null) {
                LockGridSessionManager.handleExit(player, packet);
            }
        });
        contextSupplier.get().setPacketHandled(true);
    }
}
