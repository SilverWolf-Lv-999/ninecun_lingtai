package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.common.player.PlayerSoulRoamingManager;
import net.seraphina.ninecun_lingtai.common.spell.SwordFlightManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 客户端蹲下右键请求登剑；服务端负责复核全部登剑条件。 */
public final class MountSwordFlightPacket {
    public static final MountSwordFlightPacket INSTANCE = new MountSwordFlightPacket();

    private MountSwordFlightPacket() {
    }

    public static void encode(MountSwordFlightPacket packet, FriendlyByteBuf buffer) {
    }

    public static MountSwordFlightPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(MountSwordFlightPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null && !PlayerSoulRoamingManager.isSoulRoaming(player)) {
            SwordFlightManager.requestMount(player, true);
        }
    }
}
