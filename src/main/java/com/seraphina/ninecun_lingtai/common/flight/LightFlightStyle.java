package com.seraphina.ninecun_lingtai.common.flight;

import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;

import java.util.Arrays;
import java.util.Optional;

public enum LightFlightStyle {
    LIGHT("light"),
    AIR_WALKING("air_walking");

    private final String serializedName;

    LightFlightStyle(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public static Optional<LightFlightStyle> forRealm(CultivationRealm realm) {
        if (realm == CultivationRealm.GOLDEN_CORE) {
            return Optional.of(LIGHT);
        }
        if (realm.ordinal() >= CultivationRealm.NASCENT_SOUL.ordinal()) {
            return Optional.of(AIR_WALKING);
        }
        return Optional.empty();
    }

    public static LightFlightStyle fromOrdinal(int ordinal) {
        LightFlightStyle[] styles = values();
        return ordinal >= 0 && ordinal < styles.length ? styles[ordinal] : LIGHT;
    }

    public static LightFlightStyle fromSerializedName(String serializedName) {
        return Arrays.stream(values())
                .filter(style -> style.serializedName.equals(serializedName))
                .findFirst()
                .orElse(LIGHT);
    }
}
