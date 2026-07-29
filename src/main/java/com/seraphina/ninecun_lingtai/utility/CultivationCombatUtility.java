package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;

import java.util.Optional;

public final class CultivationCombatUtility {
    public static boolean canInstantlyKillLowerRealmCultivator(
            Optional<CultivationStage> attackerStage,
            Optional<CultivationStage> defenderStage
    ) {
        return attackerStage
                .map(stage -> stage.realm().ordinal() >= CultivationRealm.SPIRIT_TRANSFORMATION.ordinal())
                .orElse(false)
                && defenderStage
                .map(stage -> stage.realm().ordinal() < CultivationRealm.SPIRIT_TRANSFORMATION.ordinal())
                .orElse(true);
    }
}
