package net.seraphina.ninecun_lingtai.common.formation;

import net.seraphina.ninecun_lingtai.client.player.ClientFormationOverlayData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * S→C：布阵幻影指引帧（手持阵盘潜行时下发）。
 * 每旗位：期望点 + 五行（-1 = 阵图不限定）+ 是否已对齐；
 * 神识开启时追加"最近实旗位置"用于精确偏差连线（阵禁篇 6.2：布阵也吃魂强）。
 */
public record FormationGuidePacket(BlockPos discPos, List<Entry> entries) {

    public record Entry(
            BlockPos expectedPos,
            byte elementIndex,
            boolean aligned,
            @Nullable BlockPos actualPos
    ) {
    }

    public static void encode(FormationGuidePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.discPos);
        buffer.writeVarInt(packet.entries.size());
        for (Entry entry : packet.entries) {
            buffer.writeBlockPos(entry.expectedPos);
            buffer.writeByte(entry.elementIndex);
            buffer.writeBoolean(entry.aligned);
            buffer.writeBoolean(entry.actualPos != null);
            if (entry.actualPos != null) {
                buffer.writeBlockPos(entry.actualPos);
            }
        }
    }

    public static FormationGuidePacket decode(FriendlyByteBuf buffer) {
        BlockPos discPos = buffer.readBlockPos();
        int count = buffer.readVarInt();
        List<Entry> entries = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            BlockPos expectedPos = buffer.readBlockPos();
            byte elementIndex = buffer.readByte();
            boolean aligned = buffer.readBoolean();
            BlockPos actualPos = buffer.readBoolean() ? buffer.readBlockPos() : null;
            entries.add(new Entry(expectedPos, elementIndex, aligned, actualPos));
        }
        return new FormationGuidePacket(discPos, entries);
    }

    public static void handle(FormationGuidePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientFormationOverlayData.acceptGuide(packet)
        ));
        contextSupplier.get().setPacketHandled(true);
    }
}
