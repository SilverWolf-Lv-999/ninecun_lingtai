package net.seraphina.ninecun_lingtai.common.player;

import java.util.Arrays;
import java.util.Optional;

public enum GreatCausalityTargetMode {
    SELECTED("selected"),
    RANDOM("random");

    private final String serializedName;

    GreatCausalityTargetMode(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public static Optional<GreatCausalityTargetMode> fromSerializedName(String serializedName) {
        return Arrays.stream(values())
                .filter(mode -> mode.serializedName.equals(serializedName))
                .findFirst();
    }
}
