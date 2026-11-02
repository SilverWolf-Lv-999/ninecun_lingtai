package net.seraphina.ninecun_lingtai.common.daotitle;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.player.DaoTitleSnapshot;
import net.seraphina.ninecun_lingtai.utility.DaoTitleUtility;

public final class DaoTitleManager {
    private DaoTitleManager() {
    }

    public static String getDisplayTitle(ServerPlayer player) {
        return DaoTitleUtility.getDisplayTitle(player);
    }

    public static Component formatName(ServerPlayer player, Component username) {
        DaoTitleSnapshot snapshot = DaoTitleUtility.getSnapshot(player);
        return DaoTitleUtility.formatName(username, snapshot);
    }

    public static void syncToAll(ServerPlayer subject) {
        for (ServerPlayer recipient : subject.server.getPlayerList().getPlayers()) {
            ModNetwork.sendDaoTitle(recipient, subject);
        }
    }

    public static void syncTo(ServerPlayer recipient, ServerPlayer subject) {
        ModNetwork.sendDaoTitle(recipient, subject);
    }
}
