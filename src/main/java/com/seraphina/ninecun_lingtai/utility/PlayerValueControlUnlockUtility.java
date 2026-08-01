package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.api.cultivation.CultivationApi;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import com.seraphina.ninecun_lingtai.common.player.PlayerTechniqueData;
import com.seraphina.ninecun_lingtai.common.player.PlayerValueControlData;
import com.seraphina.ninecun_lingtai.common.player.PlayerValueControlSettings;
import com.seraphina.ninecun_lingtai.common.player.PlayerValueControlType;
import com.seraphina.ninecun_lingtai.common.spell.SwordFlightManager;
import net.minecraft.world.entity.player.Player;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class PlayerValueControlUnlockUtility {
    public static boolean isUnlocked(Player player, PlayerValueControlType type) {
        return switch (type) {
            case ATTACK_DAMAGE -> hasReachedRealm(player, CultivationRealm.QI_REFINING);
            case SWORD_FLIGHT_SPEED -> PlayerTechniqueData.hasTechnique(
                    player,
                    SwordFlightManager.TECHNIQUE_ID
            );
            case LIGHT_FLIGHT_SPEED -> hasReachedRealm(player, CultivationRealm.GOLDEN_CORE);
        };
    }

    public static Set<PlayerValueControlType> getUnlockedTypes(Player player) {
        EnumSet<PlayerValueControlType> unlockedTypes = EnumSet.noneOf(PlayerValueControlType.class);
        for (PlayerValueControlType type : PlayerValueControlType.values()) {
            if (isUnlocked(player, type)) {
                unlockedTypes.add(type);
            }
        }
        return Set.copyOf(unlockedTypes);
    }

    public static double getEffectiveApplication(Player player, PlayerValueControlType type) {
        if (!isUnlocked(player, type)) {
            return PlayerValueControlUtility.DEFAULT_APPLICATION;
        }
        return PlayerValueControlData.getApplication(player, type);
    }

    public static PlayerValueControlSettings mergeUnlockedApplications(
            Player player,
            PlayerValueControlSettings currentSettings,
            PlayerValueControlSettings requestedSettings
    ) {
        Map<PlayerValueControlType, Double> applications = new EnumMap<>(PlayerValueControlType.class);
        PlayerValueControlSettings existingSettings = currentSettings == null
                ? PlayerValueControlSettings.defaultSettings()
                : currentSettings;
        PlayerValueControlSettings sanitizedRequestedSettings = requestedSettings == null
                ? PlayerValueControlSettings.defaultSettings()
                : requestedSettings;
        for (PlayerValueControlType type : PlayerValueControlType.values()) {
            applications.put(
                    type,
                    isUnlocked(player, type)
                            ? sanitizedRequestedSettings.getApplication(type)
                            : existingSettings.getApplication(type)
            );
        }
        return new PlayerValueControlSettings(applications);
    }

    private static boolean hasReachedRealm(Player player, CultivationRealm realm) {
        return CultivationApi.getStage(player)
                .map(stage -> stage.realm().ordinal() >= realm.ordinal())
                .orElse(false);
    }

}
