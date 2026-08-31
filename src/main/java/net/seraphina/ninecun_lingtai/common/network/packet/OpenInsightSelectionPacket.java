package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.client.screen.InsightSelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * S→C：打开悟道印记三选一界面（T14）。只发候选 id，名称/描述走语言键。
 *
 * <p>TODO(接线): 注册进 ModNetwork（追加末尾 PLAY_TO_CLIENT，升 PROTOCOL_VERSION），
 * 并在 InsightPool.offerCandidates 处发包。</p>
 */
public record OpenInsightSelectionPacket(List<ResourceLocation> candidateIds, long expireGameTime) {

    public static void encode(OpenInsightSelectionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.candidateIds.size());
        for (ResourceLocation candidateId : packet.candidateIds) {
            buffer.writeResourceLocation(candidateId);
        }
        buffer.writeVarLong(packet.expireGameTime);
    }

    public static OpenInsightSelectionPacket decode(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        List<ResourceLocation> candidateIds = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            candidateIds.add(buffer.readResourceLocation());
        }
        return new OpenInsightSelectionPacket(candidateIds, buffer.readVarLong());
    }

    public static void handle(
            OpenInsightSelectionPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> Minecraft.getInstance().setScreen(
                        new InsightSelectionScreen(packet.candidateIds)
                )
        );
    }
}
