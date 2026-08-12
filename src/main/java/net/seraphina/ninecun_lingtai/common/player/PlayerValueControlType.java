package net.seraphina.ninecun_lingtai.common.player;

import java.util.Arrays;
import java.util.Optional;

public enum PlayerValueControlType {
    ATTACK_DAMAGE("attack_damage"),
    SWORD_FLIGHT_SPEED("sword_flight_speed"),
    LIGHT_FLIGHT_SPEED("light_flight_speed");

    private final String serializedName;

    PlayerValueControlType(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public static Optional<PlayerValueControlType> fromSerializedName(String serializedName) {
        return Arrays.stream(values())
                .filter(type -> type.serializedName.equals(serializedName))
                .findFirst();
    }
}
