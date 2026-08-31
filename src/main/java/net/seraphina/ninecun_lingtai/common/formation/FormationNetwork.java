package net.seraphina.ninecun_lingtai.common.formation;

import net.seraphina.ninecun_lingtai.ModMain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 阵法专用网络通道（M4 并发约束：不往 ModNetwork 的共享自增 id 里插包，
 * 独立通道天然免 id 冲突）。{@code init()} 由 FormationModEventHandler 在
 * FMLCommonSetupEvent 调用，双端各自完成注册。
 */
public final class FormationNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(ModMain.MOD_ID, "formation"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    private static boolean initialized;

    private FormationNetwork() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        CHANNEL.messageBuilder(FormationGuidePacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FormationGuidePacket::encode)
                .decoder(FormationGuidePacket::decode)
                .consumerMainThread(FormationGuidePacket::handle)
                .add();
        CHANNEL.messageBuilder(FormationFlowPacket.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FormationFlowPacket::encode)
                .decoder(FormationFlowPacket::decode)
                .consumerMainThread(FormationFlowPacket::handle)
                .add();
    }

    public static void sendGuide(ServerPlayer player, FormationGuidePacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendFlow(ServerPlayer player, FormationFlowPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
