package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.common.insight.InsightPool;
import net.seraphina.ninecun_lingtai.common.player.PlayerSoulRoamingManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C→S：悟道印记三选一落子。服务端只接受当前未过期候选集内的 id（防伪造包）。
 *
 * <p>TODO(接线): 注册进 ModNetwork（追加末尾 PLAY_TO_SERVER，升 PROTOCOL_VERSION）。</p>
 */
public record SelectInsightPacket(ResourceLocation insightId) {

    public static void encode(SelectInsightPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.insightId);
    }

    public static SelectInsightPacket decode(FriendlyByteBuf buffer) {
        return new SelectInsightPacket(buffer.readResourceLocation());
    }

    public static void handle(
            SelectInsightPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null && !PlayerSoulRoamingManager.isSoulRoaming(player)) {
            InsightPool.select(player, packet.insightId);
        }
    }
}
