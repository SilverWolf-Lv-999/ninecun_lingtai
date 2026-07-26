package com.seraphina.ninecun_lingtai.common.network;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.api.element.FiveElementValues;
import com.seraphina.ninecun_lingtai.api.technique.LearnedTechniqueClientData;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueClientData;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueProgress;
import com.seraphina.ninecun_lingtai.common.data.TechniqueManager;
import com.seraphina.ninecun_lingtai.common.network.packet.BreakthroughRequestPacket;
import com.seraphina.ninecun_lingtai.common.network.packet.CastSpellPacket;
import com.seraphina.ninecun_lingtai.common.network.packet.CultivationFeedbackPacket;
import com.seraphina.ninecun_lingtai.common.network.packet.ImprintTechniquePacket;
import com.seraphina.ninecun_lingtai.common.network.packet.ImprintTechniqueToJadeSlipPacket;
import com.seraphina.ninecun_lingtai.common.network.packet.LearnTechniquePacket;
import com.seraphina.ninecun_lingtai.common.network.packet.OpenAptitudeScreenPacket;
import com.seraphina.ninecun_lingtai.common.network.packet.OpenTechniqueImprintScreenPacket;
import com.seraphina.ninecun_lingtai.common.network.packet.OpenTechniqueScreenPacket;
import com.seraphina.ninecun_lingtai.common.network.packet.RequestCultivationInfoPacket;
import com.seraphina.ninecun_lingtai.common.network.packet.RequestTechniqueLibraryPacket;
import com.seraphina.ninecun_lingtai.common.network.packet.SelectSpellPacket;
import com.seraphina.ninecun_lingtai.common.network.packet.SetMeditationStatePacket;
import com.seraphina.ninecun_lingtai.common.network.packet.SubmitAptitudePacket;
import com.seraphina.ninecun_lingtai.common.network.packet.SyncCultivationInfoPacket;
import com.seraphina.ninecun_lingtai.common.network.packet.SyncMeditationStatePacket;
import com.seraphina.ninecun_lingtai.common.network.packet.SyncTechniqueLibraryPacket;
import com.seraphina.ninecun_lingtai.common.player.PlayerCultivationInfo;
import com.seraphina.ninecun_lingtai.common.player.PlayerImprintedTechniqueData;
import com.seraphina.ninecun_lingtai.common.player.PlayerTechniqueData;
import com.seraphina.ninecun_lingtai.common.player.PlayerTechniqueProficiencyData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "12";
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
        CHANNEL.messageBuilder(
                        RequestCultivationInfoPacket.class,
                        nextPacketId++,
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(RequestCultivationInfoPacket::encode)
                .decoder(RequestCultivationInfoPacket::decode)
                .consumerMainThread(RequestCultivationInfoPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        SyncCultivationInfoPacket.class,
                        nextPacketId++,
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(SyncCultivationInfoPacket::encode)
                .decoder(SyncCultivationInfoPacket::decode)
                .consumerMainThread(SyncCultivationInfoPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        OpenTechniqueScreenPacket.class,
                        nextPacketId++,
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(OpenTechniqueScreenPacket::encode)
                .decoder(OpenTechniqueScreenPacket::decode)
                .consumerMainThread(OpenTechniqueScreenPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        OpenTechniqueImprintScreenPacket.class,
                        nextPacketId++,
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(OpenTechniqueImprintScreenPacket::encode)
                .decoder(OpenTechniqueImprintScreenPacket::decode)
                .consumerMainThread(OpenTechniqueImprintScreenPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        RequestTechniqueLibraryPacket.class,
                        nextPacketId++,
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(RequestTechniqueLibraryPacket::encode)
                .decoder(RequestTechniqueLibraryPacket::decode)
                .consumerMainThread(RequestTechniqueLibraryPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        SyncTechniqueLibraryPacket.class,
                        nextPacketId++,
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(SyncTechniqueLibraryPacket::encode)
                .decoder(SyncTechniqueLibraryPacket::decode)
                .consumerMainThread(SyncTechniqueLibraryPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        ImprintTechniquePacket.class,
                        nextPacketId++,
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(ImprintTechniquePacket::encode)
                .decoder(ImprintTechniquePacket::decode)
                .consumerMainThread(ImprintTechniquePacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        ImprintTechniqueToJadeSlipPacket.class,
                        nextPacketId++,
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(ImprintTechniqueToJadeSlipPacket::encode)
                .decoder(ImprintTechniqueToJadeSlipPacket::decode)
                .consumerMainThread(ImprintTechniqueToJadeSlipPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        LearnTechniquePacket.class,
                        nextPacketId++,
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(LearnTechniquePacket::encode)
                .decoder(LearnTechniquePacket::decode)
                .consumerMainThread(LearnTechniquePacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        BreakthroughRequestPacket.class,
                        nextPacketId++,
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(BreakthroughRequestPacket::encode)
                .decoder(BreakthroughRequestPacket::decode)
                .consumerMainThread(BreakthroughRequestPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        CultivationFeedbackPacket.class,
                        nextPacketId++,
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(CultivationFeedbackPacket::encode)
                .decoder(CultivationFeedbackPacket::decode)
                .consumerMainThread(CultivationFeedbackPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        SelectSpellPacket.class,
                        nextPacketId++,
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(SelectSpellPacket::encode)
                .decoder(SelectSpellPacket::decode)
                .consumerMainThread(SelectSpellPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        CastSpellPacket.class,
                        nextPacketId++,
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(CastSpellPacket::encode)
                .decoder(CastSpellPacket::decode)
                .consumerMainThread(CastSpellPacket::handle)
                .add();
    }

    public static void sendAptitudeScreenRequest(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), OpenAptitudeScreenPacket.INSTANCE);
    }

    public static void sendMeditationState(
            ServerPlayer recipient,
            UUID playerId,
            boolean meditating,
            FiveElementValues environmentValues,
            FiveElementValues movementAffinities,
            int realmLevel
    ) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> recipient),
                new SyncMeditationStatePacket(
                        playerId,
                        meditating,
                        environmentValues,
                        movementAffinities,
                        realmLevel
                )
        );
    }

    public static void sendMeditationStateToTrackingAndSelf(
            ServerPlayer player,
            boolean meditating,
            FiveElementValues environmentValues,
            FiveElementValues movementAffinities,
            int realmLevel
    ) {
        CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new SyncMeditationStatePacket(
                        player.getUUID(),
                        meditating,
                        environmentValues,
                        movementAffinities,
                        realmLevel
                )
        );
    }

    public static void sendCultivationInfo(ServerPlayer player) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncCultivationInfoPacket(PlayerCultivationInfo.capture(player))
        );
    }

    public static void sendCultivationFeedback(
            ServerPlayer player,
            boolean success,
            Component message
    ) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new CultivationFeedbackPacket(success, message)
        );
    }

    public static void sendOpenTechniqueScreen(
            ServerPlayer player,
            TechniqueClientData technique,
            boolean imprinted
    ) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new OpenTechniqueScreenPacket(technique, imprinted)
        );
    }

    public static void sendOpenTechniqueImprintScreen(
            ServerPlayer player,
            InteractionHand hand,
            List<TechniqueClientData> techniques
    ) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new OpenTechniqueImprintScreenPacket(hand, techniques)
        );
    }

    public static void sendTechniqueLibrary(ServerPlayer player) {
        Map<ResourceLocation, TechniqueProgress> learnedProgress = PlayerTechniqueData.getTechniques(player);
        Map<ResourceLocation, Long> proficiencies = PlayerTechniqueProficiencyData.getAll(player);
        List<LearnedTechniqueClientData> learnedTechniques = learnedProgress.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new LearnedTechniqueClientData(
                        entry.getKey(),
                        entry.getValue().comprehensionScore(),
                        proficiencies.getOrDefault(entry.getKey(), 0L),
                        entry.getValue().mastery()
                ))
                .toList();
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncTechniqueLibraryPacket(
                        TechniqueManager.INSTANCE.getTechniques().stream()
                                .map(TechniqueClientData::fromDefinition)
                                .toList(),
                        PlayerImprintedTechniqueData.getTechniques(player),
                        learnedTechniques
                )
        );
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}
