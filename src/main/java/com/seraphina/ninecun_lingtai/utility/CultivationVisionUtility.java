package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.api.cultivation.CultivationApi;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class CultivationVisionUtility {
    private CultivationVisionUtility() {
    }

    public static boolean seesNightAsDay(Player player) {
        return seesNightAsDay(CultivationApi.getStage(player));
    }

    public static boolean seesNightAsDay(Optional<CultivationStage> stage) {
        return hasReachedRealm(stage, CultivationRealm.FOUNDATION_ESTABLISHMENT);
    }

    public static boolean hasReachedRealm(Optional<CultivationStage> stage, CultivationRealm realm) {
        return stage
                .map(value -> hasReachedRealm(value, realm))
                .orElse(false);
    }

    public static boolean hasReachedRealm(CultivationStage stage, CultivationRealm realm) {
        return stage.realm().ordinal() >= realm.ordinal();
    }
}
