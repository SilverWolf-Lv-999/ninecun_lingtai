package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.common.breakthrough.BreakthroughRitualManager;
import net.seraphina.ninecun_lingtai.common.player.PlayerSoulRoamingManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C→S：主动取消引灵仪式（§5.1 CANCELLED 分支：分文不动）。
 *
 * <p>TODO(接线): 注册进 ModNetwork（追加末尾 PLAY_TO_SERVER，升 PROTOCOL_VERSION）。</p>
 */
public final class CancelBreakthroughRitualPacket {
    public static final CancelBreakthroughRitualPacket INSTANCE = new CancelBreakthroughRitualPacket();

    private CancelBreakthroughRitualPacket() {
    }

    public static void encode(CancelBreakthroughRitualPacket packet, FriendlyByteBuf buffer) {
    }

    public static CancelBreakthroughRitualPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(
            CancelBreakthroughRitualPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null && !PlayerSoulRoamingManager.isSoulRoaming(player)) {
            BreakthroughRitualManager.cancel(player);
        }
    }
}
