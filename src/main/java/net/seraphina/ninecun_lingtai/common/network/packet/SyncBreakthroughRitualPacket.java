package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.client.screen.BreakthroughRitualHud;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S→C：引灵仪式状态同步（约每秒一次）。按 R6 只下发把握档位键与
 * （魂强 ≥ 5 星时）最拖后腿修正 id，绝不下发精确概率或掷骰结果。
 *
 * <p>TODO(接线): 注册进 ModNetwork（追加末尾 PLAY_TO_CLIENT，升 PROTOCOL_VERSION），
 * 见开发日志"需集中接线"。</p>
 */
public record SyncBreakthroughRitualPacket(
        boolean active,
        int remainingTicks,
        int totalTicks,
        String graspBucketKey,
        String draggingModifierId
) {
    public static SyncBreakthroughRitualPacket inactive() {
        return new SyncBreakthroughRitualPacket(false, 0, 0, "", "");
    }

    public static void encode(SyncBreakthroughRitualPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.active);
        buffer.writeVarInt(packet.remainingTicks);
        buffer.writeVarInt(packet.totalTicks);
        buffer.writeUtf(packet.graspBucketKey);
        buffer.writeUtf(packet.draggingModifierId);
    }

    public static SyncBreakthroughRitualPacket decode(FriendlyByteBuf buffer) {
        return new SyncBreakthroughRitualPacket(
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readUtf(),
                buffer.readUtf()
        );
    }

    public static void handle(
            SyncBreakthroughRitualPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> BreakthroughRitualHud.applySync(
                        packet.active,
                        packet.remainingTicks,
                        packet.totalTicks,
                        packet.graspBucketKey,
                        packet.draggingModifierId
                )
        );
    }
}
