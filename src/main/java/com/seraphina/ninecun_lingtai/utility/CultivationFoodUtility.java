package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.api.cultivation.CultivationApi;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class CultivationFoodUtility {
    private CultivationFoodUtility() {
    }

    public static boolean isFasting(Player player) {
        return isFasting(CultivationApi.getStage(player));
    }

    public static boolean isFasting(Optional<CultivationStage> stage) {
        return stage
                .map(CultivationFoodUtility::isFasting)
                .orElse(false);
    }

    public static boolean isFasting(CultivationStage stage) {
        return stage.realm().ordinal() >= CultivationRealm.FOUNDATION_ESTABLISHMENT.ordinal();
    }
}
