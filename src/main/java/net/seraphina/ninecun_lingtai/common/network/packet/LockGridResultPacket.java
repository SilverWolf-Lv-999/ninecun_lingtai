package net.seraphina.ninecun_lingtai.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S→C：锁纹判定结果帧（命中/滞涩免罚/回退/稳固/层破/失败/弹出/尽破/自毁）。
 * progress 与 tolerance 是客户端唯一可见的进度事实。
 */
public record LockGridResultPacket(byte resultOrdinal, int nodeIndex, int progress, int tolerance) {

    public enum Result {
        HIT,
        TREMOR,
        SETBACK,
        STABLE,
        LAYER_BROKEN,
        FAILED,
        EXPELLED,
        COMPLETE,
        DETONATED
    }

    public static LockGridResultPacket of(Result result, int nodeIndex, int progress, int tolerance) {
        return new LockGridResultPacket((byte) result.ordinal(), nodeIndex, progress, tolerance);
    }

    public Result result() {
        Result[] values = Result.values();
        return this.resultOrdinal >= 0 && this.resultOrdinal < values.length
                ? values[this.resultOrdinal]
                : Result.EXPELLED;
    }

    public static void encode(LockGridResultPacket packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.resultOrdinal);
        buffer.writeVarInt(packet.nodeIndex);
        buffer.writeVarInt(packet.progress);
        buffer.writeVarInt(packet.tolerance);
    }

    public static LockGridResultPacket decode(FriendlyByteBuf buffer) {
        return new LockGridResultPacket(
                buffer.readByte(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt()
        );
    }

    public static void handle(LockGridResultPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> net.seraphina.ninecun_lingtai.client.screen.LockGridScreen.acceptResult(packet)
        ));
        contextSupplier.get().setPacketHandled(true);
    }
}
