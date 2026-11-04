package net.seraphina.ninecun_lingtai.common.player;

import net.seraphina.ninecun_lingtai.ModMain;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

public final class PlayerCultivationProgressData {
    private static final String CULTIVATION_PROGRESS_KEY = ModMain.MOD_ID + ":cultivation_progress";
    private static final String CHAOS_QI_KEY = "chaos_qi";
    private static final String HEAVENLY_LAWS_KEY = "heavenly_laws";
    private static final String IMMORTAL_ESSENCE_KEY = "immortal_essence";
    private static final String IMMORTAL_PROGRESS_MILLIS_KEY = "immortal_progress_millis";
    private static final String LAST_REAL_TIME_MILLIS_KEY = "last_real_time_millis";

    public static double getChaosQi(Player player) {
        return getDouble(player, CHAOS_QI_KEY);
    }

    public static void addChaosQi(Player player, double amount) {
        setDouble(player, CHAOS_QI_KEY, getChaosQi(player) + amount);
    }

    public static void setChaosQi(Player player, double amount) {
        setDouble(player, CHAOS_QI_KEY, amount);
    }

    public static double getHeavenlyLaws(Player player) {
        return getDouble(player, HEAVENLY_LAWS_KEY);
    }

    public static void addHeavenlyLaws(Player player, double amount) {
        setDouble(player, HEAVENLY_LAWS_KEY, getHeavenlyLaws(player) + amount);
    }

    public static void setHeavenlyLaws(Player player, double amount) {
        setDouble(player, HEAVENLY_LAWS_KEY, amount);
    }

    public static double getImmortalEssence(Player player) {
        return getDouble(player, IMMORTAL_ESSENCE_KEY);
    }

    public static void addImmortalEssence(Player player, double amount) {
        setDouble(player, IMMORTAL_ESSENCE_KEY, getImmortalEssence(player) + amount);
    }

    public static void setImmortalEssence(Player player, double amount) {
        setDouble(player, IMMORTAL_ESSENCE_KEY, amount);
    }

    public static long getImmortalProgressMillis(Player player) {
        return getLong(player, IMMORTAL_PROGRESS_MILLIS_KEY);
    }

    public static void setImmortalProgressMillis(Player player, long amount) {
        CompoundTag data = getData(player);
        data.putLong(IMMORTAL_PROGRESS_MILLIS_KEY, Math.max(0L, amount));
        saveData(player, data);
    }

    public static long getLastRealTimeMillis(Player player) {
        return getLong(player, LAST_REAL_TIME_MILLIS_KEY);
    }

    public static void setLastRealTimeMillis(Player player, long amount) {
        CompoundTag data = getData(player);
        data.putLong(LAST_REAL_TIME_MILLIS_KEY, Math.max(0L, amount));
        saveData(player, data);
    }

    public static void resetImmortalProgress(Player player) {
        CompoundTag data = getData(player);
        data.remove(IMMORTAL_PROGRESS_MILLIS_KEY);
        data.remove(LAST_REAL_TIME_MILLIS_KEY);
        saveData(player, data);
    }

    public static void copy(Player sourcePlayer, Player targetPlayer) {
        CompoundTag sourceData = sourcePlayer.getPersistentData();
        if (sourceData.contains(CULTIVATION_PROGRESS_KEY, Tag.TAG_COMPOUND)) {
            targetPlayer.getPersistentData().put(
                    CULTIVATION_PROGRESS_KEY,
                    sourceData.getCompound(CULTIVATION_PROGRESS_KEY).copy()
            );
        }
    }

    private static double getDouble(Player player, String key) {
        CompoundTag data = getData(player);
        double value = data.getDouble(key);
        return Double.isFinite(value) && value > 0.0D ? value : 0.0D;
    }

    private static long getLong(Player player, String key) {
        return Math.max(0L, getData(player).getLong(key));
    }

    private static void setDouble(Player player, String key, double amount) {
        CompoundTag data = getData(player);
        if (!Double.isFinite(amount) || amount <= 0.0D) {
            data.remove(key);
        } else {
            data.putDouble(key, amount);
        }
        saveData(player, data);
    }

    private static CompoundTag getData(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        return persistentData.contains(CULTIVATION_PROGRESS_KEY, Tag.TAG_COMPOUND)
                ? persistentData.getCompound(CULTIVATION_PROGRESS_KEY)
                : new CompoundTag();
    }

    private static void saveData(Player player, CompoundTag data) {
        player.getPersistentData().put(CULTIVATION_PROGRESS_KEY, data);
    }
}
