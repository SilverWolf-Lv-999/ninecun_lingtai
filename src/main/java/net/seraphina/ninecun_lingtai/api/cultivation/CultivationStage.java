package net.seraphina.ninecun_lingtai.api.cultivation;

import java.util.Objects;
import java.util.Optional;

public record CultivationStage(CultivationRealm realm, int layer) {
    public static final int MINIMUM_LAYER = 1;
    public static final int MAXIMUM_LAYER = 9;

    public CultivationStage {
        Objects.requireNonNull(realm, "realm");
        if (layer < MINIMUM_LAYER || layer > MAXIMUM_LAYER) {
            throw new IllegalArgumentException("Cultivation layer must be between one and nine");
        }
    }

    public Optional<CultivationStage> next() {
        if (this.layer < MAXIMUM_LAYER) {
            return Optional.of(new CultivationStage(this.realm, this.layer + 1));
        }
        return this.realm.next().map(nextRealm -> new CultivationStage(nextRealm, MINIMUM_LAYER));
    }

    public static CultivationStage first() {
        return new CultivationStage(CultivationRealm.QI_REFINING, MINIMUM_LAYER);
    }
}
