package com.seraphina.ninecun_lingtai.common.player;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.utility.PlayerValueControlUtility;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.EnumMap;
import java.util.Map;

public final class PlayerValueControlData {
    private static final String VALUE_CONTROL_KEY = ModMain.MOD_ID + ":value_control";

    private PlayerValueControlData() {
    }

    public static PlayerValueControlSettings getSettings(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(VALUE_CONTROL_KEY, Tag.TAG_COMPOUND)) {
            return PlayerValueControlSettings.defaultSettings();
        }
        CompoundTag valueControlData = persistentData.getCompound(VALUE_CONTROL_KEY);
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
        CompoundTag valueControlData = new CompoundTag();
        PlayerValueControlSettings sanitizedSettings = settings == null
                ? PlayerValueControlSettings.defaultSettings()
                : settings;
        for (PlayerValueControlType type : PlayerValueControlType.values()) {
            valueControlData.putDouble(
                    type.getSerializedName(),
                    sanitizedSettings.getApplication(type)
            );
        }
        player.getPersistentData().put(VALUE_CONTROL_KEY, valueControlData);
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
        CompoundTag sourceData = sourcePlayer.getPersistentData();
        if (sourceData.contains(VALUE_CONTROL_KEY, Tag.TAG_COMPOUND)) {
            targetPlayer.getPersistentData().put(
                    VALUE_CONTROL_KEY,
                    sourceData.getCompound(VALUE_CONTROL_KEY).copy()
            );
        }
    }
}
