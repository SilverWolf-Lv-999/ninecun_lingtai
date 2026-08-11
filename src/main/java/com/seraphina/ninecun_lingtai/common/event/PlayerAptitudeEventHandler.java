package com.seraphina.ninecun_lingtai.common.event;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.player.PlayerAptitudeData;
import com.seraphina.ninecun_lingtai.common.player.PlayerCultivationData;
import com.seraphina.ninecun_lingtai.common.player.PlayerDefenseResourceData;
import com.seraphina.ninecun_lingtai.common.player.PlayerImprintedTechniqueData;
import com.seraphina.ninecun_lingtai.common.player.PlayerLearnedAlchemyRecipeData;
import com.seraphina.ninecun_lingtai.common.player.PlayerSelectedSpellData;
import com.seraphina.ninecun_lingtai.common.player.PlayerSpiritualEnergyData;
import com.seraphina.ninecun_lingtai.common.player.PlayerTechniqueData;
import com.seraphina.ninecun_lingtai.common.player.PlayerTechniqueProficiencyData;
import com.seraphina.ninecun_lingtai.common.player.PlayerValueControlData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PlayerAptitudeEventHandler {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
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
                && !PlayerAptitudeData.hasCompleteAptitude(serverPlayer)) {
            ModNetwork.sendAptitudeScreenRequest(serverPlayer);
        }
    }
}
