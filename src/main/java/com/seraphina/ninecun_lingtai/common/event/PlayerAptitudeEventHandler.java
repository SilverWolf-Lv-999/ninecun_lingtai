package com.seraphina.ninecun_lingtai.common.event;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.player.PlayerAptitudeData;
import com.seraphina.ninecun_lingtai.common.player.PlayerCultivationData;
import com.seraphina.ninecun_lingtai.common.player.PlayerImprintedTechniqueData;
import com.seraphina.ninecun_lingtai.common.player.PlayerSpiritualEnergyData;
import com.seraphina.ninecun_lingtai.common.player.PlayerTechniqueData;
import com.seraphina.ninecun_lingtai.common.player.PlayerTechniqueProficiencyData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PlayerAptitudeEventHandler {
    private PlayerAptitudeEventHandler() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        requestScreenWhenMissing(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        PlayerAptitudeData.copyAptitude(event.getOriginal(), event.getEntity());
        PlayerSpiritualEnergyData.copy(event.getOriginal(), event.getEntity());
        PlayerCultivationData.copy(event.getOriginal(), event.getEntity());
        PlayerTechniqueData.copy(event.getOriginal(), event.getEntity());
        PlayerImprintedTechniqueData.copy(event.getOriginal(), event.getEntity());
        PlayerTechniqueProficiencyData.copy(event.getOriginal(), event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        requestScreenWhenMissing(event.getEntity());
    }

    private static void requestScreenWhenMissing(net.minecraft.world.entity.player.Player player) {
        if (player instanceof ServerPlayer serverPlayer
                && !PlayerAptitudeData.hasCompleteAptitude(serverPlayer)) {
            ModNetwork.sendAptitudeScreenRequest(serverPlayer);
        }
    }
}
