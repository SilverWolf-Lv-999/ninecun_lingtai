package com.seraphina.ninecun_lingtai.common.player;

import com.seraphina.ninecun_lingtai.utility.PlayerValueControlUtility;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public record PlayerValueControlSettings(Map<PlayerValueControlType, Double> applications) {
    public PlayerValueControlSettings {
        EnumMap<PlayerValueControlType, Double> copiedApplications =
                new EnumMap<>(PlayerValueControlType.class);
        for (PlayerValueControlType type : PlayerValueControlType.values()) {
            double application = applications == null
                    ? PlayerValueControlUtility.DEFAULT_APPLICATION
                    : applications.getOrDefault(type, PlayerValueControlUtility.DEFAULT_APPLICATION);
            copiedApplications.put(type, PlayerValueControlUtility.sanitizeApplication(application));
        }
        applications = Collections.unmodifiableMap(copiedApplications);
    }

    public static PlayerValueControlSettings defaultSettings() {
        return new PlayerValueControlSettings(Map.of());
    }

    public double getApplication(PlayerValueControlType type) {
        return this.applications.getOrDefault(
                Objects.requireNonNull(type),
                PlayerValueControlUtility.DEFAULT_APPLICATION
        );
    }

    public PlayerValueControlSettings withApplication(
            PlayerValueControlType type,
            double application
    ) {
        EnumMap<PlayerValueControlType, Double> updatedApplications =
                new EnumMap<>(this.applications);
        updatedApplications.put(type, PlayerValueControlUtility.sanitizeApplication(application));
        return new PlayerValueControlSettings(updatedApplications);
    }
}
