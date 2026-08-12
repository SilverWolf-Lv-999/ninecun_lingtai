package net.seraphina.ninecun_lingtai.client.player;

import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.network.packet.RequestCultivationInfoPacket;
import net.seraphina.ninecun_lingtai.common.player.PlayerCultivationInfo;

import java.util.Optional;

public final class ClientCultivationInfoManager {
    private static PlayerCultivationInfo cultivationInfo;
    private static Optional<CultivationStage> cultivationStage = Optional.empty();

    private ClientCultivationInfoManager() {
    }

    public static void beginOpen() {
        cultivationInfo = null;
        requestRefresh();
    }

    public static void requestRefresh() {
        ModNetwork.sendToServer(RequestCultivationInfoPacket.INSTANCE);
    }

    public static void accept(PlayerCultivationInfo updatedCultivationInfo) {
        cultivationInfo = updatedCultivationInfo;
        cultivationStage = updatedCultivationInfo.cultivationStage();
    }

    public static Optional<PlayerCultivationInfo> getCultivationInfo() {
        if (ClientSoulRoamingManager.isLocalPlayerSoulRoaming()) {
            return Optional.empty();
        }
        return Optional.ofNullable(cultivationInfo);
    }

    public static Optional<CultivationStage> getCultivationStage() {
        return cultivationStage;
    }

    public static void reset() {
        cultivationInfo = null;
        cultivationStage = Optional.empty();
    }
}
