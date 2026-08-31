package net.seraphina.ninecun_lingtai.common.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * S→C：锁纹界面开屏/换层。只含节点坐标/五行/雾区/邻接边/引位——
 * 主径、假流光路径、总层数、自毁阈值全部不在此包（服务端秘密）。
 */
public record OpenLockGridPacket(
        long sessionNonce,
        int grade,
        int layerOrdinal,
        int tolerance,
        int startNode,
        int cycleTicks,
        boolean deathPact,
        List<NodeEntry> nodes,
        List<int[]> edges
) {
    /** elementOrdinal 对应 ElementType.FIVE_ELEMENTS 下标。 */
    public record NodeEntry(short x, short y, byte elementOrdinal, boolean fog) {
    }

    public static void encode(OpenLockGridPacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.sessionNonce);
        buffer.writeVarInt(packet.grade);
        buffer.writeVarInt(packet.layerOrdinal);
        buffer.writeVarInt(packet.tolerance);
        buffer.writeVarInt(packet.startNode);
        buffer.writeVarInt(packet.cycleTicks);
        buffer.writeBoolean(packet.deathPact);
        buffer.writeVarInt(packet.nodes.size());
        for (NodeEntry node : packet.nodes) {
            buffer.writeShort(node.x);
            buffer.writeShort(node.y);
            buffer.writeByte(node.elementOrdinal);
            buffer.writeBoolean(node.fog);
        }
        buffer.writeVarInt(packet.edges.size());
        for (int[] edge : packet.edges) {
            buffer.writeVarInt(edge[0]);
            buffer.writeVarInt(edge[1]);
        }
    }

    public static OpenLockGridPacket decode(FriendlyByteBuf buffer) {
        long sessionNonce = buffer.readLong();
        int grade = buffer.readVarInt();
        int layerOrdinal = buffer.readVarInt();
        int tolerance = buffer.readVarInt();
        int startNode = buffer.readVarInt();
        int cycleTicks = buffer.readVarInt();
        boolean deathPact = buffer.readBoolean();
        int nodeCount = buffer.readVarInt();
        List<NodeEntry> nodes = new ArrayList<>(nodeCount);
        for (int index = 0; index < nodeCount; index++) {
            nodes.add(new NodeEntry(
                    buffer.readShort(),
                    buffer.readShort(),
                    buffer.readByte(),
                    buffer.readBoolean()
            ));
        }
        int edgeCount = buffer.readVarInt();
        List<int[]> edges = new ArrayList<>(edgeCount);
        for (int index = 0; index < edgeCount; index++) {
            edges.add(new int[]{buffer.readVarInt(), buffer.readVarInt()});
        }
        return new OpenLockGridPacket(
                sessionNonce, grade, layerOrdinal, tolerance, startNode,
                cycleTicks, deathPact, nodes, edges
        );
    }

    public static void handle(OpenLockGridPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> net.seraphina.ninecun_lingtai.client.screen.LockGridScreen.open(packet)
        ));
        contextSupplier.get().setPacketHandled(true);
    }
}
