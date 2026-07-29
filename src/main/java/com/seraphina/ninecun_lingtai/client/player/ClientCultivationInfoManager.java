package com.seraphina.ninecun_lingtai.client.player;

import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationApi;
import com.seraphina.ninecun_lingtai.common.lifespan.LifespanSnapshot;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.network.packet.RequestCultivationInfoPacket;
import com.seraphina.ninecun_lingtai.common.player.PlayerCultivationInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class ClientCultivationInfoManager {
    private static LifespanSnapshot lifespan = LifespanSnapshot.empty();
    private static boolean initialized;

    private ClientCultivationInfoManager() {
    }

    public static void beginOpen() {
        initialized = false;
        requestRefresh();
    }

    public static void requestRefresh() {
        ModNetwork.sendToServer(RequestCultivationInfoPacket.INSTANCE);
    }

    public static void accept(LifespanSnapshot updatedLifespan) {
        lifespan = updatedLifespan == null ? LifespanSnapshot.empty() : updatedLifespan;
        initialized = true;
    }

    public static Optional<PlayerCultivationInfo> getCultivationInfo() {
        if (ClientSoulRoamingManager.isLocalPlayerSoulRoaming()) {
            return Optional.empty();
        }
        Player player = Minecraft.getInstance().player;
        return initialized && player != null
                ? Optional.of(PlayerCultivationInfo.capture(player, lifespan))
                : Optional.empty();
    }

    public static Optional<CultivationStage> getCultivationStage() {
        Player player = Minecraft.getInstance().player;
        return player == null ? Optional.empty() : CultivationApi.getStage(player);
    }

    public static void reset() {
        lifespan = LifespanSnapshot.empty();
        initialized = false;
    }
}
