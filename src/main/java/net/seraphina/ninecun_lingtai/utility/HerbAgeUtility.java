package net.seraphina.ninecun_lingtai.utility;

import net.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.seraphina.ninecun_lingtai.common.time.MonotonicCalendarUtility;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

/**
 * 灵药药龄（炼丹 M4-c）：采摘时以修真历（{@link MonotonicCalendarUtility}，
 * 防 {@code /time set} 回拨）记下采摘日，药龄随物品静置增长。
 *
 * <p>药效乘区：{@code 1.0 + min(药龄年 / 100, 0.5) × 0.2}，整体不超过
 * {@code 1 + alchemyM4.herbAgePotencyCap}（默认 +10%）；锄头移栽伤根的
 * "降品质"标记再乘 {@link #DEGRADED_POTENCY_MULTIPLIER}。
 * 未记录采摘日的物品（老物品/商店货）按 1.0 处理。
 */
public final class HerbAgeUtility {
    public static final String HARVEST_DAY_TAG = "ninecun_lingtai:herb_harvest_day";
    public static final String DEGRADED_TAG = "ninecun_lingtai:herb_degraded";

    /** 移栽伤根的药效折损。 */
    public static final double DEGRADED_POTENCY_MULTIPLIER = 0.85D;
    /** 修真历口径：1 年 = 36 修真日（LifespanUtility 同源）。 */
    private static final double DAYS_PER_YEAR = 36.0D;
    private static final double AGE_CENTURY_FRACTION_CAP = 0.5D;
    private static final double AGE_POTENCY_FACTOR = 0.2D;

    private HerbAgeUtility() {
    }

    /** 采摘落 NBT：记当前修真历日。 */
    public static void markHarvested(ItemStack stack, MinecraftServer server) {
        stack.getOrCreateTag().putLong(
                HARVEST_DAY_TAG,
                MonotonicCalendarUtility.getCalendarDay(server)
        );
    }

    /** 锄头移栽伤根标记（药效 ×{@value #DEGRADED_POTENCY_MULTIPLIER}）。 */
    public static void markDegraded(ItemStack stack) {
        stack.getOrCreateTag().putBoolean(DEGRADED_TAG, true);
    }

    public static boolean isDegraded(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(DEGRADED_TAG);
    }

    /** 药龄（修真历年，未记录返回 0）。 */
    public static double getAgeYears(ItemStack stack, MinecraftServer server) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(HARVEST_DAY_TAG)) {
            return 0.0D;
        }
        long elapsedDays = Math.max(
                0L,
                MonotonicCalendarUtility.getCalendarDay(server) - tag.getLong(HARVEST_DAY_TAG)
        );
        return elapsedDays / DAYS_PER_YEAR;
    }

    /** 单味灵药的药效乘区（药龄增益封顶 + 伤根折损）。 */
    public static double getPotencyMultiplier(ItemStack stack, MinecraftServer server) {
        double ageBonus = Math.min(
                CommonConfig.ALCHEMY_HERB_AGE_POTENCY_CAP.get(),
                Math.min(getAgeYears(stack, server) / 100.0D, AGE_CENTURY_FRACTION_CAP)
                        * AGE_POTENCY_FACTOR
        );
        double potency = 1.0D + ageBonus;
        return isDegraded(stack) ? potency * DEGRADED_POTENCY_MULTIPLIER : potency;
    }
}
