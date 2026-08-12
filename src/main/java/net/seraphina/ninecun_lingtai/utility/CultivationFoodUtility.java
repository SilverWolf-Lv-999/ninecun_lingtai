package net.seraphina.ninecun_lingtai.utility;

import net.seraphina.ninecun_lingtai.api.cultivation.CultivationApi;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class CultivationFoodUtility {

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
