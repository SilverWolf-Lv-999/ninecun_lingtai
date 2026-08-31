package net.seraphina.ninecun_lingtai.common.formation;

import net.seraphina.ninecun_lingtai.client.player.ClientFormationOverlayData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * S→C：运转阵法的旗间灵力流帧（复用灵识线的世界渲染口径，神识开启才下发）。
 * 生门旗的出流段带 reversed 标记——<b>魂强不足 {@code LIFE_GATE_MIN_SOUL_STARS}
 * 星时服务端全部按正向下发</b>（同质化不留客户端破解面，与锁纹呼吸感同一原则）；
 * 惊扰满三次的戒备期整包停发。渲染不做任何高亮，逆流只能靠肉眼观察。
 */
public record FormationFlowPacket(BlockPos discPos, List<Segment> segments, int durationTicks) {

    public record Segment(float fromX, float fromY, float fromZ, float toX, float toY, float toZ, boolean reversed) {
    }

    public static void encode(FormationFlowPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.discPos);
        buffer.writeVarInt(packet.durationTicks);
        buffer.writeVarInt(packet.segments.size());
        for (Segment segment : packet.segments) {
            buffer.writeFloat(segment.fromX);
            buffer.writeFloat(segment.fromY);
            buffer.writeFloat(segment.fromZ);
            buffer.writeFloat(segment.toX);
            buffer.writeFloat(segment.toY);
            buffer.writeFloat(segment.toZ);
            buffer.writeBoolean(segment.reversed);
        }
    }

    public static FormationFlowPacket decode(FriendlyByteBuf buffer) {
        BlockPos discPos = buffer.readBlockPos();
        int durationTicks = buffer.readVarInt();
        int count = buffer.readVarInt();
        List<Segment> segments = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            segments.add(new Segment(
                    buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                    buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                    buffer.readBoolean()
            ));
        }
        return new FormationFlowPacket(discPos, segments, durationTicks);
    }

    public static void handle(FormationFlowPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientFormationOverlayData.acceptFlow(packet)
        ));
        contextSupplier.get().setPacketHandled(true);
    }
}
