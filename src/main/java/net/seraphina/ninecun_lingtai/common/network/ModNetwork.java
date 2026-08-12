package net.seraphina.ninecun_lingtai.common.network;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeDefinition;
import net.seraphina.ninecun_lingtai.api.element.FiveElementValues;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeClientData;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeJeiData;
import net.seraphina.ninecun_lingtai.api.technique.LearnedTechniqueClientData;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueClientData;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueProgress;
import net.seraphina.ninecun_lingtai.common.data.TechniqueManager;
import net.seraphina.ninecun_lingtai.common.data.AlchemyRecipeManager;
import net.seraphina.ninecun_lingtai.common.entity.spell.DefenseSpellGuardianEntity;
import net.seraphina.ninecun_lingtai.common.debug.DaoScrollSnapshot;
import net.seraphina.ninecun_lingtai.common.network.packet.BreakthroughRequestPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.BoostLightFlightPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.CastSpellPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.CloseStorageRingPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.CultivationFeedbackPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.DismountSwordFlightPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.ImprintTechniquePacket;
import net.seraphina.ninecun_lingtai.common.network.packet.ImprintTechniqueToJadeSlipPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.ImprintAlchemyRecipeToFormulaSlipPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.LearnTechniquePacket;
import net.seraphina.ninecun_lingtai.common.network.packet.LearnAlchemyRecipePacket;
import net.seraphina.ninecun_lingtai.common.network.packet.MountSwordFlightPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.OpenAptitudeScreenPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.AlchemyFurnaceActionPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.OpenAlchemyRecipeImprintScreenPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.OpenAlchemyRecipeScreenPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.OpenDaoScrollScreenPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.OpenTechniqueImprintScreenPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.OpenTechniqueScreenPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.PushingMountainsClearSelectionPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.PushingMountainsEntityizePacket;
import net.seraphina.ninecun_lingtai.common.network.packet.PushingMountainsRestorePacket;
import net.seraphina.ninecun_lingtai.common.network.packet.PushingMountainsSelectPointPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.PushingMountainsThrowPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.RequestCultivationInfoPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.RequestPlayerValueControlSettingsPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.RequestAlchemyRecipeLibraryPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.RequestTechniqueLibraryPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.SelectSpellPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.SetStorageRingImmersionPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.SoulInteractionPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.SoulMovementPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.StartSoulRoamingPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.SetShrinkEarthPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.SetPlayerValueControlSettingsPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.SetLightFlightPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.SetMeditationStatePacket;
import net.seraphina.ninecun_lingtai.common.network.packet.SetPressureStatePacket;
import net.seraphina.ninecun_lingtai.common.network.packet.SubmitAptitudePacket;
import net.seraphina.ninecun_lingtai.common.network.packet.SubmitDaoScrollPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.StorageRingCameraPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.StorageRingInteractionPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.SmoothShrinkEarthTeleportPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.ShowNotificationPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.SyncCultivationInfoPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.SyncDefenseResourcePacket;
import net.seraphina.ninecun_lingtai.common.network.packet.SyncDefenseSpellStatePacket;
import net.seraphina.ninecun_lingtai.common.network.packet.SyncLightFlightPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.SyncMeditationStatePacket;
import net.seraphina.ninecun_lingtai.common.network.packet.SyncPossessionPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.SyncPressureStatePacket;
import net.seraphina.ninecun_lingtai.common.network.packet.SyncPlayerValueControlSettingsPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.SyncShrinkEarthPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.SyncSoulRoamingPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.SyncStorageRingProjectionPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.SyncSwordFlightPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.SyncTechniqueLibraryPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.SyncAlchemyRecipesPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.SyncTribulationStatePacket;
import net.seraphina.ninecun_lingtai.common.network.packet.TriggerBodyShieldPacket;
import net.seraphina.ninecun_lingtai.common.flight.LightFlightManager;
import net.seraphina.ninecun_lingtai.common.player.PlayerLightFlightData;
import net.seraphina.ninecun_lingtai.common.player.PlayerShrinkEarthData;
import net.seraphina.ninecun_lingtai.common.player.PlayerValueControlData;
import net.seraphina.ninecun_lingtai.common.player.PlayerSwordFlightData;
import net.seraphina.ninecun_lingtai.common.player.DefenseResourceSnapshot;
import net.seraphina.ninecun_lingtai.common.player.DefenseSpellSnapshot;
import net.seraphina.ninecun_lingtai.common.player.PlayerCultivationInfo;
import net.seraphina.ninecun_lingtai.common.player.PlayerDefenseResourceData;
import net.seraphina.ninecun_lingtai.common.player.PlayerDefenseSpellData;
import net.seraphina.ninecun_lingtai.common.player.PlayerImprintedTechniqueData;
import net.seraphina.ninecun_lingtai.common.player.PlayerLearnedAlchemyRecipeData;
import net.seraphina.ninecun_lingtai.common.player.PlayerSoulRoamingManager;
import net.seraphina.ninecun_lingtai.common.player.PlayerTechniqueData;
import net.seraphina.ninecun_lingtai.common.player.PlayerTechniqueProficiencyData;
import net.seraphina.ninecun_lingtai.common.player.PossessionSyncData;
import net.seraphina.ninecun_lingtai.common.player.SoulRoamingSyncData;
import net.seraphina.ninecun_lingtai.utility.PlayerValueControlUnlockUtility;
import net.seraphina.ninecun_lingtai.utility.DefenseSpellUtility;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.seraphina.ninecun_lingtai.common.tribulation.TribulationSession;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "67676767";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(ModMain.MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    private static int nextPacketId;

    public static void register() {
        CHANNEL.messageBuilder(
                        AlchemyFurnaceActionPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(AlchemyFurnaceActionPacket::encode)
                .decoder(AlchemyFurnaceActionPacket::decode)
                .consumerMainThread(AlchemyFurnaceActionPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        OpenAptitudeScreenPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(OpenAptitudeScreenPacket::encode)
                .decoder(OpenAptitudeScreenPacket::decode)
                .consumerMainThread(OpenAptitudeScreenPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        SubmitAptitudePacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(SubmitAptitudePacket::encode)
                .decoder(SubmitAptitudePacket::decode)
                .consumerMainThread(SubmitAptitudePacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        SetMeditationStatePacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(SetMeditationStatePacket::encode)
                .decoder(SetMeditationStatePacket::decode)
                .consumerMainThread(SetMeditationStatePacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        SetPressureStatePacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(SetPressureStatePacket::encode)
                .decoder(SetPressureStatePacket::decode)
                .consumerMainThread(SetPressureStatePacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        SyncPressureStatePacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(SyncPressureStatePacket::encode)
                .decoder(SyncPressureStatePacket::decode)
                .consumerMainThread(SyncPressureStatePacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        SyncMeditationStatePacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(SyncMeditationStatePacket::encode)
                .decoder(SyncMeditationStatePacket::decode)
                .consumerMainThread(SyncMeditationStatePacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        SyncTribulationStatePacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(SyncTribulationStatePacket::encode)
                .decoder(SyncTribulationStatePacket::decode)
                .consumerMainThread(SyncTribulationStatePacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        RequestCultivationInfoPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(RequestCultivationInfoPacket::encode)
                .decoder(RequestCultivationInfoPacket::decode)
                .consumerMainThread(RequestCultivationInfoPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        SyncCultivationInfoPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(SyncCultivationInfoPacket::encode)
                .decoder(SyncCultivationInfoPacket::decode)
                .consumerMainThread(SyncCultivationInfoPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        OpenTechniqueScreenPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(OpenTechniqueScreenPacket::encode)
                .decoder(OpenTechniqueScreenPacket::decode)
                .consumerMainThread(OpenTechniqueScreenPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        OpenAlchemyRecipeScreenPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(OpenAlchemyRecipeScreenPacket::encode)
                .decoder(OpenAlchemyRecipeScreenPacket::decode)
                .consumerMainThread(OpenAlchemyRecipeScreenPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        OpenAlchemyRecipeImprintScreenPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(OpenAlchemyRecipeImprintScreenPacket::encode)
                .decoder(OpenAlchemyRecipeImprintScreenPacket::decode)
                .consumerMainThread(OpenAlchemyRecipeImprintScreenPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        OpenTechniqueImprintScreenPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(OpenTechniqueImprintScreenPacket::encode)
                .decoder(OpenTechniqueImprintScreenPacket::decode)
                .consumerMainThread(OpenTechniqueImprintScreenPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        RequestTechniqueLibraryPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(RequestTechniqueLibraryPacket::encode)
                .decoder(RequestTechniqueLibraryPacket::decode)
                .consumerMainThread(RequestTechniqueLibraryPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        RequestAlchemyRecipeLibraryPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(RequestAlchemyRecipeLibraryPacket::encode)
                .decoder(RequestAlchemyRecipeLibraryPacket::decode)
                .consumerMainThread(RequestAlchemyRecipeLibraryPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        SyncTechniqueLibraryPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(SyncTechniqueLibraryPacket::encode)
                .decoder(SyncTechniqueLibraryPacket::decode)
                .consumerMainThread(SyncTechniqueLibraryPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        SyncAlchemyRecipesPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(SyncAlchemyRecipesPacket::encode)
                .decoder(SyncAlchemyRecipesPacket::decode)
                .consumerMainThread(SyncAlchemyRecipesPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        ImprintAlchemyRecipeToFormulaSlipPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(ImprintAlchemyRecipeToFormulaSlipPacket::encode)
                .decoder(ImprintAlchemyRecipeToFormulaSlipPacket::decode)
                .consumerMainThread(ImprintAlchemyRecipeToFormulaSlipPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        LearnAlchemyRecipePacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(LearnAlchemyRecipePacket::encode)
                .decoder(LearnAlchemyRecipePacket::decode)
                .consumerMainThread(LearnAlchemyRecipePacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        ImprintTechniquePacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(ImprintTechniquePacket::encode)
                .decoder(ImprintTechniquePacket::decode)
                .consumerMainThread(ImprintTechniquePacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        ImprintTechniqueToJadeSlipPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(ImprintTechniqueToJadeSlipPacket::encode)
                .decoder(ImprintTechniqueToJadeSlipPacket::decode)
                .consumerMainThread(ImprintTechniqueToJadeSlipPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        LearnTechniquePacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(LearnTechniquePacket::encode)
                .decoder(LearnTechniquePacket::decode)
                .consumerMainThread(LearnTechniquePacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        BreakthroughRequestPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(BreakthroughRequestPacket::encode)
                .decoder(BreakthroughRequestPacket::decode)
                .consumerMainThread(BreakthroughRequestPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        CultivationFeedbackPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(CultivationFeedbackPacket::encode)
                .decoder(CultivationFeedbackPacket::decode)
                .consumerMainThread(CultivationFeedbackPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        ShowNotificationPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(ShowNotificationPacket::encode)
                .decoder(ShowNotificationPacket::decode)
                .consumerMainThread(ShowNotificationPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        SelectSpellPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(SelectSpellPacket::encode)
                .decoder(SelectSpellPacket::decode)
                .consumerMainThread(SelectSpellPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        CastSpellPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(CastSpellPacket::encode)
                .decoder(CastSpellPacket::decode)
                .consumerMainThread(CastSpellPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        SyncSwordFlightPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(SyncSwordFlightPacket::encode)
                .decoder(SyncSwordFlightPacket::decode)
                .consumerMainThread(SyncSwordFlightPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        DismountSwordFlightPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(DismountSwordFlightPacket::encode)
                .decoder(DismountSwordFlightPacket::decode)
                .consumerMainThread(DismountSwordFlightPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        MountSwordFlightPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(MountSwordFlightPacket::encode)
                .decoder(MountSwordFlightPacket::decode)
                .consumerMainThread(MountSwordFlightPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        SetLightFlightPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(SetLightFlightPacket::encode)
                .decoder(SetLightFlightPacket::decode)
                .consumerMainThread(SetLightFlightPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        BoostLightFlightPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(BoostLightFlightPacket::encode)
                .decoder(BoostLightFlightPacket::decode)
                .consumerMainThread(BoostLightFlightPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        SetShrinkEarthPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(SetShrinkEarthPacket::encode)
                .decoder(SetShrinkEarthPacket::decode)
                .consumerMainThread(SetShrinkEarthPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        SyncShrinkEarthPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(SyncShrinkEarthPacket::encode)
                .decoder(SyncShrinkEarthPacket::decode)
                .consumerMainThread(SyncShrinkEarthPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        SmoothShrinkEarthTeleportPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(SmoothShrinkEarthTeleportPacket::encode)
                .decoder(SmoothShrinkEarthTeleportPacket::decode)
                .consumerMainThread(SmoothShrinkEarthTeleportPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        SyncLightFlightPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(SyncLightFlightPacket::encode)
                .decoder(SyncLightFlightPacket::decode)
                .consumerMainThread(SyncLightFlightPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        RequestPlayerValueControlSettingsPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(RequestPlayerValueControlSettingsPacket::encode)
                .decoder(RequestPlayerValueControlSettingsPacket::decode)
                .consumerMainThread(RequestPlayerValueControlSettingsPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        SetPlayerValueControlSettingsPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(SetPlayerValueControlSettingsPacket::encode)
                .decoder(SetPlayerValueControlSettingsPacket::decode)
                .consumerMainThread(SetPlayerValueControlSettingsPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        SyncPlayerValueControlSettingsPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(SyncPlayerValueControlSettingsPacket::encode)
                .decoder(SyncPlayerValueControlSettingsPacket::decode)
                .consumerMainThread(SyncPlayerValueControlSettingsPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        TriggerBodyShieldPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(TriggerBodyShieldPacket::encode)
                .decoder(TriggerBodyShieldPacket::decode)
                .consumerMainThread(TriggerBodyShieldPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        SyncDefenseResourcePacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(SyncDefenseResourcePacket::encode)
                .decoder(SyncDefenseResourcePacket::decode)
                .consumerMainThread(SyncDefenseResourcePacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        SyncDefenseSpellStatePacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(SyncDefenseSpellStatePacket::encode)
                .decoder(SyncDefenseSpellStatePacket::decode)
                .consumerMainThread(SyncDefenseSpellStatePacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        StartSoulRoamingPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(StartSoulRoamingPacket::encode)
                .decoder(StartSoulRoamingPacket::decode)
                .consumerMainThread(StartSoulRoamingPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        SyncSoulRoamingPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(SyncSoulRoamingPacket::encode)
                .decoder(SyncSoulRoamingPacket::decode)
                .consumerMainThread(SyncSoulRoamingPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        SoulMovementPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(SoulMovementPacket::encode)
                .decoder(SoulMovementPacket::decode)
                .consumerMainThread(SoulMovementPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        SoulInteractionPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(SoulInteractionPacket::encode)
                .decoder(SoulInteractionPacket::decode)
                .consumerMainThread(SoulInteractionPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        SyncPossessionPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(SyncPossessionPacket::encode)
                .decoder(SyncPossessionPacket::decode)
                .consumerMainThread(SyncPossessionPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        PushingMountainsSelectPointPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(PushingMountainsSelectPointPacket::encode)
                .decoder(PushingMountainsSelectPointPacket::decode)
                .consumerMainThread(PushingMountainsSelectPointPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        PushingMountainsEntityizePacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(PushingMountainsEntityizePacket::encode)
                .decoder(PushingMountainsEntityizePacket::decode)
                .consumerMainThread(PushingMountainsEntityizePacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        PushingMountainsClearSelectionPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(PushingMountainsClearSelectionPacket::encode)
                .decoder(PushingMountainsClearSelectionPacket::decode)
                .consumerMainThread(PushingMountainsClearSelectionPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        PushingMountainsRestorePacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(PushingMountainsRestorePacket::encode)
                .decoder(PushingMountainsRestorePacket::decode)
                .consumerMainThread(PushingMountainsRestorePacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        PushingMountainsThrowPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(PushingMountainsThrowPacket::encode)
                .decoder(PushingMountainsThrowPacket::decode)
                .consumerMainThread(PushingMountainsThrowPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        OpenDaoScrollScreenPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(OpenDaoScrollScreenPacket::encode)
                .decoder(OpenDaoScrollScreenPacket::decode)
                .consumerMainThread(OpenDaoScrollScreenPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        SubmitDaoScrollPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(SubmitDaoScrollPacket::encode)
                .decoder(SubmitDaoScrollPacket::decode)
                .consumerMainThread(SubmitDaoScrollPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        CloseStorageRingPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(CloseStorageRingPacket::encode)
                .decoder(CloseStorageRingPacket::decode)
                .consumerMainThread(CloseStorageRingPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        StorageRingCameraPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(StorageRingCameraPacket::encode)
                .decoder(StorageRingCameraPacket::decode)
                .consumerMainThread(StorageRingCameraPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        SetStorageRingImmersionPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(SetStorageRingImmersionPacket::encode)
                .decoder(SetStorageRingImmersionPacket::decode)
                .consumerMainThread(SetStorageRingImmersionPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        StorageRingInteractionPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(StorageRingInteractionPacket::encode)
                .decoder(StorageRingInteractionPacket::decode)
                .consumerMainThread(StorageRingInteractionPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                        SyncStorageRingProjectionPacket.class,
                        getId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(SyncStorageRingProjectionPacket::encode)
                .decoder(SyncStorageRingProjectionPacket::decode)
                .consumerMainThread(SyncStorageRingProjectionPacket::handle)
                .add();
    }

    public static int getId() {
        return nextPacketId++;
    }

    public static void sendStorageRingProjection(
            ServerPlayer player,
            UUID ringId,
            Vec3 cameraPosition,
            float cameraYRot,
            float cameraXRot,
            List<SyncStorageRingProjectionPacket.BlockSnapshot> blocks
    ) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncStorageRingProjectionPacket(
                        true,
                        ringId,
                        cameraPosition,
                        cameraYRot,
                        cameraXRot,
                        blocks
                )
        );
    }

    public static void sendStorageRingProjectionInactive(ServerPlayer player) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                SyncStorageRingProjectionPacket.inactive()
        );
    }

    /** 把玩家当前的缩地状态广播给所有追踪者与其本人。 */
    public static void sendShrinkEarthState(ServerPlayer player) {
        CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new SyncShrinkEarthPacket(
                        player.getUUID(),
                        PlayerShrinkEarthData.isActive(player),
                        PlayerShrinkEarthData.getCompressionScale(player)
                )
        );
    }

    /** 把某玩家的缩地状态单发给一名观察者。 */
    public static void sendShrinkEarthStateTo(ServerPlayer recipient, ServerPlayer subject) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> recipient),
                new SyncShrinkEarthPacket(
                        subject.getUUID(),
                        PlayerShrinkEarthData.isActive(subject),
                        PlayerShrinkEarthData.getCompressionScale(subject)
                )
        );
    }

    public static void sendShrinkEarthTeleport(ServerPlayer player, Vec3 from, Vec3 to) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SmoothShrinkEarthTeleportPacket(from, to)
        );
    }

    /** 把玩家当前的御剑状态广播给所有追踪者与其本人。 */
    public static void sendSwordFlightState(ServerPlayer player) {
        CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new SyncSwordFlightPacket(
                        player.getUUID(),
                        PlayerSwordFlightData.isFlying(player),
                        PlayerSwordFlightData.getSword(player)
                )
        );
    }

    /** 把某玩家的御剑状态单发给一名观察者（开始追踪或刚登录时补发）。 */
    public static void sendSwordFlightStateTo(ServerPlayer recipient, ServerPlayer subject) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> recipient),
                new SyncSwordFlightPacket(
                        subject.getUUID(),
                        PlayerSwordFlightData.isFlying(subject),
                        PlayerSwordFlightData.getSword(subject)
                )
        );
    }

    /** 把玩家当前的遁光状态广播给所有追踪者与其本人。 */
    public static void sendLightFlightState(ServerPlayer player) {
        CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new SyncLightFlightPacket(
                        player.getUUID(),
                        PlayerLightFlightData.isFlying(player),
                        LightFlightManager.getLightColor(player),
                        LightFlightManager.isBoosting(player),
                        LightFlightManager.getFlightStyle(player)
                )
        );
    }

    /** 把某玩家的遁光状态单发给一名观察者。 */
    public static void sendLightFlightStateTo(ServerPlayer recipient, ServerPlayer subject) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> recipient),
                new SyncLightFlightPacket(
                        subject.getUUID(),
                        PlayerLightFlightData.isFlying(subject),
                        LightFlightManager.getLightColor(subject),
                        LightFlightManager.isBoosting(subject),
                        LightFlightManager.getFlightStyle(subject)
                )
        );
    }

    public static void sendBodyShield(LivingEntity entity) {
        CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
                new TriggerBodyShieldPacket(entity.getId())
        );
    }

    public static void sendPressureState(
            ServerPlayer player,
            boolean affected,
            float severity,
            boolean immobilized
    ) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncPressureStatePacket(affected, severity, immobilized)
        );
    }

    public static void sendDefenseSpellState(ServerPlayer subject) {
        SyncDefenseSpellStatePacket packet = createDefenseSpellStatePacket(subject);
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> subject), packet);
    }

    public static void sendDefenseSpellStateTo(ServerPlayer recipient, ServerPlayer subject) {
        SyncDefenseSpellStatePacket packet = createDefenseSpellStatePacket(subject);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> recipient), packet);
    }

    private static SyncDefenseSpellStatePacket createDefenseSpellStatePacket(ServerPlayer subject) {
        Optional<DefenseSpellSnapshot> bodyShield = PlayerDefenseSpellData.getBodyShield(subject);
        Optional<DefenseSpellSnapshot> guardian = DefenseSpellUtility.getGuardian(subject)
                .map(DefenseSpellGuardianEntity::createSnapshot);
        return new SyncDefenseSpellStatePacket(subject.getId(), bodyShield, guardian);
    }

    public static void sendAptitudeScreenRequest(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), OpenAptitudeScreenPacket.INSTANCE);
    }

    public static void sendOpenDaoScrollScreen(
            ServerPlayer player,
            DaoScrollSnapshot snapshot,
            boolean editingSelf
    ) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new OpenDaoScrollScreenPacket(snapshot, editingSelf)
        );
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

    public static void sendTribulationState(
            ServerPlayer player,
            TribulationSession session
    ) {
        sendTribulationState(player, session, true);
    }

    public static void sendTribulationState(
            ServerPlayer player,
            TribulationSession session,
            boolean active
    ) {
        SyncTribulationStatePacket packet = createTribulationStatePacket(session, active);
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), packet);
    }

    public static void sendTribulationStateTo(
            ServerPlayer recipient,
            TribulationSession session
    ) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> recipient),
                createTribulationStatePacket(session, true)
        );
    }

    private static SyncTribulationStatePacket createTribulationStatePacket(
            TribulationSession session,
            boolean active
    ) {
        return new SyncTribulationStatePacket(
                session.playerId(),
                active,
                session.phase().ordinal(),
                session.phaseRemainingTicks(),
                session.type().strikeCount(),
                session.completedStrikes(),
                session.center().x,
                session.center().y,
                session.center().z,
                (float) session.radius(),
                (float) session.type().strengthMultiplier()
        );
    }

    public static void sendCultivationInfo(ServerPlayer player) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncCultivationInfoPacket(PlayerCultivationInfo.capture(player))
        );
    }

    public static void sendDefenseResource(ServerPlayer player) {
        if (PlayerSoulRoamingManager.isSoulRoaming(player)) {
            CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new SyncDefenseResourcePacket(DefenseResourceSnapshot.empty(0))
            );
            return;
        }
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncDefenseResourcePacket(PlayerDefenseResourceData.capture(player))
        );
    }

    public static void sendSoulRoamingState(ServerPlayer player, SoulRoamingSyncData syncData) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncSoulRoamingPacket(syncData)
        );
    }

    public static void sendPossessionState(ServerPlayer player, PossessionSyncData syncData) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncPossessionPacket(syncData)
        );
    }

    public static void sendPossessionStateToAll(
            List<ServerPlayer> players,
            PossessionSyncData syncData
    ) {
        for (ServerPlayer player : players) {
            sendPossessionState(player, syncData);
        }
    }

    public static void sendPlayerValueControlSettings(ServerPlayer player) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncPlayerValueControlSettingsPacket(
                        PlayerValueControlData.getSettings(player),
                        PlayerSoulRoamingManager.isSoulRoaming(player)
                                ? Set.of()
                                : PlayerValueControlUnlockUtility.getUnlockedTypes(player)
                )
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

    public static void sendNotification(
            ServerPlayer player,
            Component title,
            Component content,
            int durationTicks
    ) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new ShowNotificationPacket(title, content, durationTicks)
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

    public static void sendOpenAlchemyRecipeScreen(
            ServerPlayer player,
            AlchemyRecipeDefinition recipe,
            boolean learned
    ) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new OpenAlchemyRecipeScreenPacket(
                        AlchemyRecipeClientData.fromDefinition(recipe),
                        learned
                )
        );
    }

    public static void sendAlchemyRecipes(ServerPlayer player) {
        Set<ResourceLocation> learnedRecipeIds = PlayerSoulRoamingManager.isSoulRoaming(player)
                ? Set.of()
                : PlayerLearnedAlchemyRecipeData.getRecipes(player);
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncAlchemyRecipesPacket(
                        AlchemyRecipeManager.INSTANCE.getRecipes().stream()
                                .map(AlchemyRecipeClientData::fromDefinition)
                                .toList(),
                        learnedRecipeIds,
                        learnedRecipeIds.stream()
                                .flatMap(recipeId -> AlchemyRecipeManager.INSTANCE.getRecipe(recipeId).stream())
                                .map(AlchemyRecipeJeiData::fromDefinition)
                                .toList()
                )
        );
    }

    public static void sendOpenAlchemyRecipeImprintScreen(
            ServerPlayer player,
            InteractionHand hand,
            List<AlchemyRecipeClientData> recipes
    ) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new OpenAlchemyRecipeImprintScreenPacket(hand, recipes)
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
        if (PlayerSoulRoamingManager.isSoulRoaming(player)) {
            CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new SyncTechniqueLibraryPacket(
                            TechniqueManager.INSTANCE.getTechniques().stream()
                                    .map(TechniqueClientData::fromDefinition)
                                    .toList(),
                            Set.of(),
                            List.of()
                    )
            );
            return;
        }
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
