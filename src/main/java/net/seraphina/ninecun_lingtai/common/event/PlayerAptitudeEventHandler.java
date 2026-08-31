package net.seraphina.ninecun_lingtai.common.event;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.player.PlayerAptitudeData;
import net.seraphina.ninecun_lingtai.common.player.PlayerBreakthroughDiscountData;
import net.seraphina.ninecun_lingtai.common.player.PlayerCultivationData;
import net.seraphina.ninecun_lingtai.common.player.PlayerDefenseResourceData;
import net.seraphina.ninecun_lingtai.common.player.PlayerImprintedTechniqueData;
import net.seraphina.ninecun_lingtai.common.player.PlayerLearnedAlchemyRecipeData;
import net.seraphina.ninecun_lingtai.common.player.PlayerPillToxinData;
import net.seraphina.ninecun_lingtai.common.player.PlayerSoulSenseData;
import net.seraphina.ninecun_lingtai.common.player.PlayerSoulTemperingData;
import net.seraphina.ninecun_lingtai.common.player.PlayerSoulInjuryData;
import net.seraphina.ninecun_lingtai.common.player.PlayerBreakthroughBonusData;
import net.seraphina.ninecun_lingtai.common.player.PlayerBreakthroughFailureData;
import net.seraphina.ninecun_lingtai.common.behavior.PlayerBehaviorLedgerData;
import net.seraphina.ninecun_lingtai.common.insight.PlayerInsightData;
import net.seraphina.ninecun_lingtai.common.status.PlayerScarData;
import net.seraphina.ninecun_lingtai.common.player.PlayerSpiritInfusionData;
import net.seraphina.ninecun_lingtai.common.player.PlayerSelectedSpellData;
import net.seraphina.ninecun_lingtai.common.player.PlayerSpiritualEnergyData;
import net.seraphina.ninecun_lingtai.common.player.PlayerTechniqueData;
import net.seraphina.ninecun_lingtai.common.player.PlayerTechniqueProficiencyData;
import net.seraphina.ninecun_lingtai.common.player.PlayerValueControlData;
import net.seraphina.ninecun_lingtai.utility.PlayerAptitudeUtility;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PlayerAptitudeEventHandler {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer
                && CommonConfig.RANDOMIZE_PLAYER_APTITUDE.get()
                && !PlayerAptitudeData.hasCompleteAptitude(serverPlayer)) {
            PlayerAptitudeUtility.assignRandomAptitude(serverPlayer);
        }
        syncCultivationInfo(event.getEntity());
        requestScreenWhenMissing(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        PlayerAptitudeData.copyAptitude(event.getOriginal(), event.getEntity());
        PlayerSpiritualEnergyData.copy(event.getOriginal(), event.getEntity());
        PlayerCultivationData.copy(event.getOriginal(), event.getEntity());
        PlayerDefenseResourceData.copy(event.getOriginal(), event.getEntity());
        PlayerTechniqueData.copy(event.getOriginal(), event.getEntity());
        PlayerImprintedTechniqueData.copy(event.getOriginal(), event.getEntity());
        PlayerLearnedAlchemyRecipeData.copy(event.getOriginal(), event.getEntity());
        PlayerTechniqueProficiencyData.copy(event.getOriginal(), event.getEntity());
        PlayerSelectedSpellData.copy(event.getOriginal(), event.getEntity());
        PlayerValueControlData.copy(event.getOriginal(), event.getEntity());
        PlayerPillToxinData.copy(event.getOriginal(), event.getEntity());
        PlayerBreakthroughDiscountData.copy(event.getOriginal(), event.getEntity());
        PlayerSpiritInfusionData.copy(event.getOriginal(), event.getEntity());
        PlayerSoulTemperingData.copy(event.getOriginal(), event.getEntity());
        PlayerSoulSenseData.copy(event.getOriginal(), event.getEntity());
        PlayerScarData.copy(event.getOriginal(), event.getEntity());
        PlayerSoulInjuryData.copy(event.getOriginal(), event.getEntity());
        PlayerBreakthroughFailureData.copy(event.getOriginal(), event.getEntity());
        PlayerBreakthroughBonusData.copy(event.getOriginal(), event.getEntity());
        PlayerInsightData.copy(event.getOriginal(), event.getEntity());
        PlayerBehaviorLedgerData.copy(event.getOriginal(), event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        syncCultivationInfo(event.getEntity());
        requestScreenWhenMissing(event.getEntity());
    }

    private static void syncCultivationInfo(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            PlayerDefenseResourceData.clampToStage(serverPlayer);
            ModNetwork.sendCultivationInfo(serverPlayer);
            ModNetwork.sendDefenseResource(serverPlayer);
            ModNetwork.sendAlchemyRecipes(serverPlayer);
        }
    }

    private static void requestScreenWhenMissing(Player player) {
        if (player instanceof ServerPlayer serverPlayer
                && !CommonConfig.RANDOMIZE_PLAYER_APTITUDE.get()
                && !PlayerAptitudeData.hasCompleteAptitude(serverPlayer)) {
            ModNetwork.sendAptitudeScreenRequest(serverPlayer);
        }
    }
}
