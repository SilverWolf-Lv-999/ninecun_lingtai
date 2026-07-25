package com.seraphina.ninecun_lingtai.common.network;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.common.network.packet.OpenAptitudeScreenPacket;
import com.seraphina.ninecun_lingtai.common.network.packet.SetMeditationStatePacket;
import com.seraphina.ninecun_lingtai.common.network.packet.SubmitAptitudePacket;
import com.seraphina.ninecun_lingtai.common.network.packet.SyncMeditationStatePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.UUID;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "3";
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
        CHANNEL.messageBuilder(
                        SetMeditationStatePacket.class,
                        nextPacketId++,
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(SetMeditationStatePacket::encode)
                .decoder(SetMeditationStatePacket::decode)
                .consumerMainThread(SetMeditationStatePacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        SyncMeditationStatePacket.class,
                        nextPacketId++,
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(SyncMeditationStatePacket::encode)
                .decoder(SyncMeditationStatePacket::decode)
                .consumerMainThread(SyncMeditationStatePacket::handle)
                .add();
    }

    public static void sendAptitudeScreenRequest(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), OpenAptitudeScreenPacket.INSTANCE);
    }

    public static void sendMeditationState(ServerPlayer recipient, UUID playerId, boolean meditating) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> recipient),
                new SyncMeditationStatePacket(playerId, meditating)
        );
    }

    public static void sendMeditationStateToTrackingAndSelf(ServerPlayer player, boolean meditating) {
        CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new SyncMeditationStatePacket(player.getUUID(), meditating)
        );
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}
