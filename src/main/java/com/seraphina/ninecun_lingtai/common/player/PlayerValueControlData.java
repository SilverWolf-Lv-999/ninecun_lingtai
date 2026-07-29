package com.seraphina.ninecun_lingtai.common.player;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.utility.LivingEntityDataUtility;
import com.seraphina.ninecun_lingtai.utility.PlayerValueControlUtility;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.EnumMap;
import java.util.Map;

public final class PlayerValueControlData {
    private static final String LEGACY_VALUE_CONTROL_KEY = ModMain.MOD_ID + ":value_control";

    private PlayerValueControlData() {
    }

    public static PlayerValueControlSettings getSettings(Player player) {
        CompoundTag valueControlData = LivingEntityDataUtility.getValueControl(player);
        if (valueControlData.isEmpty()) {
            CompoundTag legacyData = player.getPersistentData().getCompound(LEGACY_VALUE_CONTROL_KEY);
            if (!legacyData.isEmpty()) {
                Map<PlayerValueControlType, Double> migratedApplications = new EnumMap<>(
                        PlayerValueControlType.class
                );
                for (PlayerValueControlType type : PlayerValueControlType.values()) {
                    String key = type.getSerializedName();
                    if (legacyData.contains(key, Tag.TAG_ANY_NUMERIC)) {
                        migratedApplications.put(type, legacyData.getDouble(key));
                    }
                }
                PlayerValueControlSettings migratedSettings = new PlayerValueControlSettings(
                        migratedApplications
                );
                setSettings(player, migratedSettings);
                player.getPersistentData().remove(LEGACY_VALUE_CONTROL_KEY);
                return migratedSettings;
            }
            return PlayerValueControlSettings.defaultSettings();
        }

        Map<PlayerValueControlType, Double> applications = new EnumMap<>(PlayerValueControlType.class);
        for (PlayerValueControlType type : PlayerValueControlType.values()) {
            String key = type.getSerializedName();
            if (valueControlData.contains(key, Tag.TAG_ANY_NUMERIC)) {
                applications.put(type, valueControlData.getDouble(key));
            }
        }
        return new PlayerValueControlSettings(applications);
    }

    public static double getApplication(Player player, PlayerValueControlType type) {
        return getSettings(player).getApplication(type);
    }

    public static void setSettings(Player player, PlayerValueControlSettings settings) {
        PlayerValueControlSettings sanitizedSettings = settings == null
                ? PlayerValueControlSettings.defaultSettings()
                : settings;
        CompoundTag valueControlData = new CompoundTag();
        for (PlayerValueControlType type : PlayerValueControlType.values()) {
            valueControlData.putDouble(
                    type.getSerializedName(),
                    PlayerValueControlUtility.sanitizeApplication(sanitizedSettings.getApplication(type))
            );
        }
        LivingEntityDataUtility.setValueControl(player, valueControlData);
        player.getPersistentData().remove(LEGACY_VALUE_CONTROL_KEY);
    }

    public static void setApplication(
            Player player,
            PlayerValueControlType type,
            double application
    ) {
        setSettings(player, getSettings(player).withApplication(
                type,
                PlayerValueControlUtility.sanitizeApplication(application)
        ));
    }

    public static void copy(Player sourcePlayer, Player targetPlayer) {
        setSettings(targetPlayer, getSettings(sourcePlayer));
    }

    public static void migratePersistentData(Player player) {
        if (player.getPersistentData().contains(LEGACY_VALUE_CONTROL_KEY, Tag.TAG_COMPOUND)) {
            getSettings(player);
        }
    }
}
