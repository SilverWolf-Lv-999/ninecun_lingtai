package net.seraphina.ninecun_lingtai.common.player;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.soul.SoulSenseTargetCategory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

public final class PlayerSoulSenseData {
    private static final String SOUL_SENSE_KEY = ModMain.MOD_ID + ":soul_sense";
    private static final String FILTER_MASK_KEY = "filter_mask";
    /** mask 生成时的类别数：之外的新位默认开（老存档兼容，道心杀孽日志 §12.2）。 */
    private static final String FILTER_MASK_CATEGORY_COUNT_KEY = "filter_mask_categories";
    /** 未记录类别数的存档按追加 BLOOD_MARKED 前的 8 个旧类别处理。 */
    private static final int LEGACY_CATEGORY_COUNT = 8;

    public static int getFilterMask(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(SOUL_SENSE_KEY, Tag.TAG_COMPOUND)) {
            return SoulSenseTargetCategory.getFullMask();
        }
        CompoundTag soulSenseData = persistentData.getCompound(SOUL_SENSE_KEY);
        if (!soulSenseData.contains(FILTER_MASK_KEY, Tag.TAG_ANY_NUMERIC)) {
            return SoulSenseTargetCategory.getFullMask();
        }
        // 老存档掩码自动补上后加枚举位：存 mask 时未见过的新类别默认开，
        // 玩家在新版本里显式关掉的位（类别数已记录）不会被强行翻回。
        int storedMask = soulSenseData.getInt(FILTER_MASK_KEY);
        int storedCategoryCount = soulSenseData.contains(FILTER_MASK_CATEGORY_COUNT_KEY, Tag.TAG_ANY_NUMERIC)
                ? soulSenseData.getInt(FILTER_MASK_CATEGORY_COUNT_KEY)
                : LEGACY_CATEGORY_COUNT;
        int unseenCategoryBits = SoulSenseTargetCategory.getFullMask()
                & ~((1 << Math.max(0, Math.min(31, storedCategoryCount))) - 1);
        return (storedMask | unseenCategoryBits) & SoulSenseTargetCategory.getFullMask();
    }

    public static void setFilterMask(Player player, int filterMask) {
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag soulSenseData = persistentData.contains(SOUL_SENSE_KEY, Tag.TAG_COMPOUND)
                ? persistentData.getCompound(SOUL_SENSE_KEY)
                : new CompoundTag();
        soulSenseData.putInt(FILTER_MASK_KEY, filterMask & SoulSenseTargetCategory.getFullMask());
        soulSenseData.putInt(FILTER_MASK_CATEGORY_COUNT_KEY, SoulSenseTargetCategory.values().length);
        persistentData.put(SOUL_SENSE_KEY, soulSenseData);
    }

    public static boolean isCategoryEnabled(Player player, SoulSenseTargetCategory category) {
        return (getFilterMask(player) & category.getMaskBit()) != 0;
    }

    public static void copy(Player sourcePlayer, Player targetPlayer) {
        CompoundTag sourceData = sourcePlayer.getPersistentData();
        if (sourceData.contains(SOUL_SENSE_KEY, Tag.TAG_COMPOUND)) {
            targetPlayer.getPersistentData().put(
                    SOUL_SENSE_KEY,
                    sourceData.getCompound(SOUL_SENSE_KEY).copy()
            );
        }
    }
}
