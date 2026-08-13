package net.seraphina.ninecun_lingtai.common.player;

import net.seraphina.ninecun_lingtai.ModMain;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

public final class PlayerPillToxinData {
    public static final double EROSION_THRESHOLD = 100.0D;
    public static final double MARROW_THRESHOLD = 150.0D;
    public static final double OUTBREAK_THRESHOLD = 200.0D;

    private static final String TOXIN_KEY = ModMain.MOD_ID + ":pill_toxin";
    private static final String RESISTANCE_KEY = ModMain.MOD_ID + ":pill_resistance";
    private static final String RESISTANCE_COUNT_KEY = "Count";
    private static final String RESISTANCE_WINDOW_END_KEY = "WindowEnd";
    private static final long RESISTANCE_WINDOW_TICKS = 3L * 24000L;

    public static double getToxin(Player player) {
        return Math.max(0.0D, player.getPersistentData().getDouble(TOXIN_KEY));
    }

    public static void setToxin(Player player, double toxin) {
        if (toxin <= 0.0D || !Double.isFinite(toxin)) {
            player.getPersistentData().remove(TOXIN_KEY);
            return;
        }
        player.getPersistentData().putDouble(TOXIN_KEY, toxin);
    }

    public static void addToxin(Player player, double amount) {
        if (amount > 0.0D && Double.isFinite(amount)) {
            setToxin(player, getToxin(player) + amount);
        }
    }

    public static void reduceToxin(Player player, double amount) {
        if (amount > 0.0D && Double.isFinite(amount)) {
            setToxin(player, getToxin(player) - amount);
        }
    }

    public static double getAbsorptionMultiplier(Player player) {
        double toxin = getToxin(player);
        if (toxin >= MARROW_THRESHOLD) {
            return 0.5D;
        }
        return toxin >= EROSION_THRESHOLD ? 0.7D : 1.0D;
    }

    public static double getPillEffectMultiplier(Player player) {
        double toxin = getToxin(player);
        if (toxin >= MARROW_THRESHOLD) {
            return 0.6D;
        }
        return toxin >= EROSION_THRESHOLD ? 0.85D : 1.0D;
    }

    public static boolean refusesPills(Player player) {
        return getToxin(player) >= OUTBREAK_THRESHOLD;
    }

    public static double useAndGetResistanceMultiplier(Player player, String category, long gameTime) {
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag resistanceData = persistentData.contains(RESISTANCE_KEY, Tag.TAG_COMPOUND)
                ? persistentData.getCompound(RESISTANCE_KEY)
                : new CompoundTag();
        CompoundTag categoryData = resistanceData.getCompound(category);
        int usedCount = categoryData.getLong(RESISTANCE_WINDOW_END_KEY) >= gameTime
                ? categoryData.getInt(RESISTANCE_COUNT_KEY)
                : 0;
        double multiplier = switch (usedCount) {
            case 0 -> 1.0D;
            case 1 -> 0.75D;
            case 2 -> 0.5D;
            default -> 0.25D;
        };
        categoryData.putInt(RESISTANCE_COUNT_KEY, usedCount + 1);
        categoryData.putLong(RESISTANCE_WINDOW_END_KEY, gameTime + RESISTANCE_WINDOW_TICKS);
        resistanceData.put(category, categoryData);
        persistentData.put(RESISTANCE_KEY, resistanceData);
        return multiplier;
    }

    public static void copy(Player sourcePlayer, Player targetPlayer) {
        CompoundTag sourceData = sourcePlayer.getPersistentData();
        if (sourceData.contains(TOXIN_KEY, Tag.TAG_ANY_NUMERIC)) {
            targetPlayer.getPersistentData().putDouble(TOXIN_KEY, sourceData.getDouble(TOXIN_KEY));
        }
        if (sourceData.contains(RESISTANCE_KEY, Tag.TAG_COMPOUND)) {
            targetPlayer.getPersistentData().put(
                    RESISTANCE_KEY,
                    sourceData.getCompound(RESISTANCE_KEY).copy()
            );
        }
    }
}
