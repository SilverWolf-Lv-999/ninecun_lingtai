package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.client.screen.MindTrialOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * S→C：心魔试炼阶段/台词键/计时/心魔血量帧（心魔即己 §7）。
 * 相位切换即发 + 每 10 tick 心跳（对齐 sendTribulationState 节流习惯）。
 *
 * <p><b>尚未注册</b>：ModNetwork 由其它系统持有，注册（含 PROTOCOL_VERSION 升版）
 * 与服务端发送调用见开发日志"需集中接线"。注册前 HUD Overlay 收不到数据，
 * 阶段提示暂经 NotificationUtility/聊天下发（服务端已实现）。</p>
 */
public record SyncMindTrialPacket(
        byte phaseOrdinal,
        int remainingTicks,
        float demonHealthPct,
        List<String> lineKeys,
        boolean bodyEchoFlash
) {
    public static void encode(SyncMindTrialPacket packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.phaseOrdinal);
        buffer.writeVarInt(packet.remainingTicks);
        buffer.writeFloat(packet.demonHealthPct);
        buffer.writeVarInt(packet.lineKeys.size());
        for (String lineKey : packet.lineKeys) {
            buffer.writeUtf(lineKey);
        }
        buffer.writeBoolean(packet.bodyEchoFlash);
    }

    public static SyncMindTrialPacket decode(FriendlyByteBuf buffer) {
        byte phaseOrdinal = buffer.readByte();
        int remainingTicks = buffer.readVarInt();
        float demonHealthPct = buffer.readFloat();
        int lineCount = buffer.readVarInt();
        List<String> lineKeys = new ArrayList<>(lineCount);
        for (int index = 0; index < lineCount; index++) {
            lineKeys.add(buffer.readUtf());
        }
        return new SyncMindTrialPacket(
                phaseOrdinal,
                remainingTicks,
                demonHealthPct,
                lineKeys,
                buffer.readBoolean()
        );
    }

    public static void handle(SyncMindTrialPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MindTrialOverlay.accept(packet))
        );
        contextSupplier.get().setPacketHandled(true);
    }
}
