package net.seraphina.ninecun_lingtai.common.animation;

import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;

import java.util.Optional;

public enum BreakthroughAnimationType {
    MORTAL_TO_QI,
    QI_TO_FOUNDATION,
    FOUNDATION_TO_GOLDEN_CORE,
    GOLDEN_CORE_TO_NASCENT_SOUL;

    public static Optional<BreakthroughAnimationType> fromTransition(
            Optional<CultivationStage> previousStage,
            CultivationStage currentStage
    ) {
        if (previousStage.isEmpty() && currentStage.realm() == CultivationRealm.QI_REFINING) {
            return Optional.of(MORTAL_TO_QI);
        }
        if (previousStage.map(CultivationStage::realm).orElse(null) == CultivationRealm.QI_REFINING
                && currentStage.realm() == CultivationRealm.FOUNDATION_ESTABLISHMENT) {
            return Optional.of(QI_TO_FOUNDATION);
        }
        if (previousStage.map(CultivationStage::realm).orElse(null)
                        == CultivationRealm.FOUNDATION_ESTABLISHMENT
                && currentStage.realm() == CultivationRealm.GOLDEN_CORE) {
            return Optional.of(FOUNDATION_TO_GOLDEN_CORE);
        }
        if (previousStage.map(CultivationStage::realm).orElse(null)
                        == CultivationRealm.GOLDEN_CORE
                && currentStage.realm() == CultivationRealm.NASCENT_SOUL) {
            return Optional.of(GOLDEN_CORE_TO_NASCENT_SOUL);
        }
        return Optional.empty();
    }

    public static Optional<BreakthroughAnimationType> fromOrdinal(int ordinal) {
        BreakthroughAnimationType[] values = values();
        return ordinal >= 0 && ordinal < values.length
                ? Optional.of(values[ordinal])
                : Optional.empty();
    }
}
