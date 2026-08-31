package net.seraphina.ninecun_lingtai.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * S→C：一缕流光的渲染帧事件（观纹相位每周期下发一批）。客户端只做补间动画。
 * 魂强差 ≤ −2 时服务端不置 breathing（同质化在服务端做，不发了让客户端隐藏）。
 */
public record LockGridFlowPacket(
        int flowId,
        List<Integer> nodeIndices,
        boolean breathing,
        boolean highlighted,
        int runTicks
) {
    public static void encode(LockGridFlowPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.flowId);
        buffer.writeVarInt(packet.nodeIndices.size());
        for (int nodeIndex : packet.nodeIndices) {
            buffer.writeVarInt(nodeIndex);
        }
        buffer.writeBoolean(packet.breathing);
        buffer.writeBoolean(packet.highlighted);
        buffer.writeVarInt(packet.runTicks);
    }

    public static LockGridFlowPacket decode(FriendlyByteBuf buffer) {
        int flowId = buffer.readVarInt();
        int count = buffer.readVarInt();
        List<Integer> nodeIndices = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            nodeIndices.add(buffer.readVarInt());
        }
        return new LockGridFlowPacket(
                flowId,
                nodeIndices,
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readVarInt()
        );
    }

    public static void handle(LockGridFlowPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> net.seraphina.ninecun_lingtai.client.screen.LockGridScreen.acceptFlow(packet)
        ));
        contextSupplier.get().setPacketHandled(true);
    }
}
