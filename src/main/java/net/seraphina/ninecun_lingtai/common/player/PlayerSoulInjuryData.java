package net.seraphina.ninecun_lingtai.common.player;

import net.seraphina.ninecun_lingtai.ModMain;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

/**
 * 魂伤数值槽（0-100 暗值），独立于温养进度持久化。
 * 铁律：业务写入一律经 {@code SoulInjuryUtility.addInjury/heal}，
 * 本类的 setter 仅供该工具类与调试指令使用（设计文档 R1）。
 */
public final class PlayerSoulInjuryData {
    public static final double MINIMUM_INJURY = 0.0D;
    public static final double MAXIMUM_INJURY = 100.0D;

    private static final String SOUL_INJURY_KEY = ModMain.MOD_ID + ":soul_injury";
    private static final String VALUE_KEY = "value";
    private static final String LAST_NATURAL_HEAL_TICK_KEY = "lastNaturalHealTick";

    public static double getInjury(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(SOUL_INJURY_KEY, Tag.TAG_COMPOUND)) {
            return MINIMUM_INJURY;
        }
        double injury = persistentData.getCompound(SOUL_INJURY_KEY).getDouble(VALUE_KEY);
        if (!Double.isFinite(injury)) {
            return MINIMUM_INJURY;
        }
        return Math.min(MAXIMUM_INJURY, Math.max(MINIMUM_INJURY, injury));
    }

    public static void setInjury(Player player, double injury) {
        if (!Double.isFinite(injury)) {
            return;
        }
        CompoundTag soulInjuryData = getOrCreateData(player);
        soulInjuryData.putDouble(
                VALUE_KEY,
                Math.min(MAXIMUM_INJURY, Math.max(MINIMUM_INJURY, injury))
        );
        player.getPersistentData().put(SOUL_INJURY_KEY, soulInjuryData);
    }

    /** 上次自然恢复/折寿结算的日历刻（overworld dayTime），0 表示尚未结算过。 */
    public static long getLastNaturalHealTick(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(SOUL_INJURY_KEY, Tag.TAG_COMPOUND)) {
            return 0L;
        }
        return persistentData.getCompound(SOUL_INJURY_KEY).getLong(LAST_NATURAL_HEAL_TICK_KEY);
    }

    public static void setLastNaturalHealTick(Player player, long calendarTick) {
        CompoundTag soulInjuryData = getOrCreateData(player);
        soulInjuryData.putLong(LAST_NATURAL_HEAL_TICK_KEY, Math.max(0L, calendarTick));
        player.getPersistentData().put(SOUL_INJURY_KEY, soulInjuryData);
    }

    public static void copy(Player sourcePlayer, Player targetPlayer) {
        CompoundTag sourceData = sourcePlayer.getPersistentData();
        if (sourceData.contains(SOUL_INJURY_KEY, Tag.TAG_COMPOUND)) {
            targetPlayer.getPersistentData().put(
                    SOUL_INJURY_KEY,
                    sourceData.getCompound(SOUL_INJURY_KEY).copy()
            );
        }
    }

    private static CompoundTag getOrCreateData(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        return persistentData.contains(SOUL_INJURY_KEY, Tag.TAG_COMPOUND)
                ? persistentData.getCompound(SOUL_INJURY_KEY)
                : new CompoundTag();
    }
}
