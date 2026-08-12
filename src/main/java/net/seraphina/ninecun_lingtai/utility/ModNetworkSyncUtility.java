package net.seraphina.ninecun_lingtai.utility;

import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;

public final class ModNetworkSyncUtility {
    public static void syncAfterDaoScroll(ServerPlayer player) {
        ModNetwork.sendCultivationInfo(player);
        ModNetwork.sendDefenseResource(player);
        ModNetwork.sendTechniqueLibrary(player);
    }
}
