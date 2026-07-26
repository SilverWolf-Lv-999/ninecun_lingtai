package com.seraphina.ninecun_lingtai.api.cultivation;

import com.seraphina.ninecun_lingtai.common.player.PlayerCultivationData;
import com.seraphina.ninecun_lingtai.common.player.PlayerCultivationManager;
import com.seraphina.ninecun_lingtai.utility.CultivationUtility;
import net.minecraft.world.entity.player.Player;

import java.math.BigInteger;
import java.util.Optional;

public final class CultivationApi {
    private CultivationApi() {
    }

    public static Optional<CultivationStage> getStage(Player player) {
        return PlayerCultivationData.getStage(player);
    }

    public static Optional<BigInteger> getNextRequiredSpiritualEnergy(Player player) {
        return CultivationUtility.getNextRequiredSpiritualEnergy(getStage(player));
    }

    public static BreakthroughResult tryBreakthrough(Player player) {
        return PlayerCultivationManager.tryBreakthrough(player);
    }

    public static BreakthroughResult tryAdvanceLayer(Player player) {
        return PlayerCultivationManager.tryAdvanceLayer(player);
    }

    public static BreakthroughResult tryBreakthroughRealm(Player player) {
        return PlayerCultivationManager.tryBreakthroughRealm(player);
    }

    public static boolean isAtRealmPeak(Player player) {
        return getStage(player)
                .map(stage -> stage.layer() >= CultivationStage.MAXIMUM_LAYER)
                .orElse(false);
    }
}
