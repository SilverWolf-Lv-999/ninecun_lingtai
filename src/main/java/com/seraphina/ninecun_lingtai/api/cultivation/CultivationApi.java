package com.seraphina.ninecun_lingtai.api.cultivation;

import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.common.player.PlayerCultivationData;
import com.seraphina.ninecun_lingtai.common.player.PlayerCultivationManager;
import com.seraphina.ninecun_lingtai.utility.CultivationUtility;
import net.minecraft.world.entity.player.Player;

import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;

public final class CultivationApi {
    private CultivationApi() {
    }

    public static Optional<CultivationStage> getStage(Player player, ElementType spiritualRootElement) {
        return PlayerCultivationData.getStage(player, spiritualRootElement);
    }

    public static Map<ElementType, CultivationStage> getAllStages(Player player) {
        return PlayerCultivationData.getAllStages(player);
    }

    public static Optional<BigInteger> getNextRequiredSpiritualEnergy(
            Player player,
            ElementType spiritualRootElement
    ) {
        return CultivationUtility.getNextRequiredSpiritualEnergy(getStage(player, spiritualRootElement));
    }

    public static BreakthroughResult tryBreakthrough(Player player, ElementType spiritualRootElement) {
        return PlayerCultivationManager.tryBreakthrough(player, spiritualRootElement);
    }
}
