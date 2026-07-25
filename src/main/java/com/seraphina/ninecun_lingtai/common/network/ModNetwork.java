package com.seraphina.ninecun_lingtai.common.network;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.common.network.packet.OpenAptitudeScreenPacket;
import com.seraphina.ninecun_lingtai.common.network.packet.SubmitAptitudePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(ModMain.MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    private static int nextPacketId;

    private ModNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(
                        OpenAptitudeScreenPacket.class,
                        nextPacketId++,
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(OpenAptitudeScreenPacket::encode)
                .decoder(OpenAptitudeScreenPacket::decode)
                .consumerMainThread(OpenAptitudeScreenPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        SubmitAptitudePacket.class,
                        nextPacketId++,
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(SubmitAptitudePacket::encode)
                .decoder(SubmitAptitudePacket::decode)
                .consumerMainThread(SubmitAptitudePacket::handle)
                .add();
    }

    public static void sendAptitudeScreenRequest(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), OpenAptitudeScreenPacket.INSTANCE);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}
