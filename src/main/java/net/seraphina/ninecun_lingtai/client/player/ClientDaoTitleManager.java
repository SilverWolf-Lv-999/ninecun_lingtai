package net.seraphina.ninecun_lingtai.client.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.network.packet.RequestDaoTitlePacket;
import net.seraphina.ninecun_lingtai.common.player.DaoTitleSnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClientDaoTitleManager {
    private static final Map<UUID, DaoTitleSnapshot> SNAPSHOTS = new HashMap<>();

    private ClientDaoTitleManager() {
    }

    public static void beginOpen() {
        ModNetwork.sendToServer(RequestDaoTitlePacket.INSTANCE);
    }

    public static void accept(UUID playerId, DaoTitleSnapshot snapshot) {
        SNAPSHOTS.put(playerId, snapshot);
    }

    public static DaoTitleSnapshot get(UUID playerId) {
        return SNAPSHOTS.get(playerId);
    }

    public static void reset() {
        SNAPSHOTS.clear();
    }

    public static DaoTitleSnapshot getLocalSnapshot() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? null : SNAPSHOTS.get(player.getUUID());
    }
}
