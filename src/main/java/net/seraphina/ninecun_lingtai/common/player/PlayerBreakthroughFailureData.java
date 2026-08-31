package net.seraphina.ninecun_lingtai.common.player;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

/**
 * 破境失败记账（瓶颈补偿 pity + 瓶颈期）。NBT 模式照抄 {@link PlayerBreakthroughDiscountData}，
 * 按"目标境界"分桶计数：每失败一次该境界 pity +3%（上限 +15%），成功后清零该境界计数。
 * 受击/登出等中断只记"未遂"，不计入 pity（设计文档 R8：小层突破路径不得触碰本类）。
 *
 * <p>瓶颈期以 overworld gameTime 的绝对刻记录（修真历上线前的过渡语义），与
 * {@code ScarType.BOTTLENECK} 双轨互备：风险结算写 Scar，本类同步一份到期刻，
 * 仪式入口两者任一命中即拒绝发起。</p>
 */
public final class PlayerBreakthroughFailureData {
    private static final String ROOT_KEY = ModMain.MOD_ID + ":breakthrough_failure";
    private static final String PER_REALM_KEY = "PerRealm";
    private static final String FAIL_COUNT_KEY = "FailCount";
    private static final String LAST_FAIL_GAME_TIME_KEY = "LastFailGameTime";
    private static final String INTERRUPT_COUNT_KEY = "InterruptCount";
    private static final String BOTTLENECK_UNTIL_TICK_KEY = "BottleneckUntilTick";

    private PlayerBreakthroughFailureData() {
    }

    /** 瓶颈补偿：min(失败次数 × pityStep, pityCap)，读 breakthrough 配置组。 */
    public static double getPityBonus(Player player, CultivationRealm targetRealm) {
        return Math.min(
                CommonConfig.BREAKTHROUGH_PITY_CAP.get(),
                getFailCount(player, targetRealm) * CommonConfig.BREAKTHROUGH_PITY_STEP.get()
        );
    }

    public static int getFailCount(Player player, CultivationRealm targetRealm) {
        CompoundTag realmTag = getRealmTag(player, targetRealm);
        return realmTag == null ? 0 : realmTag.getInt(FAIL_COUNT_KEY);
    }

    /** 掷骰失败时调用；只允许大境界目标路径调用（R8）。 */
    public static void recordFailure(Player player, CultivationRealm targetRealm, long gameTime) {
        CompoundTag realmTag = getOrCreateRealmTag(player, targetRealm);
        realmTag.putInt(FAIL_COUNT_KEY, realmTag.getInt(FAIL_COUNT_KEY) + 1);
        realmTag.putLong(LAST_FAIL_GAME_TIME_KEY, gameTime);
    }

    /** 仪式被打断（受击/登出/换维度/重启）时记一次"未遂"，不影响 pity。 */
    public static void recordInterruption(Player player, CultivationRealm targetRealm) {
        CompoundTag realmTag = getOrCreateRealmTag(player, targetRealm);
        realmTag.putInt(INTERRUPT_COUNT_KEY, realmTag.getInt(INTERRUPT_COUNT_KEY) + 1);
    }

    /** 破境成功后清零该境界的失败计数（pity 归零）。 */
    public static void clearFailures(Player player, CultivationRealm targetRealm) {
        CompoundTag rootTag = getRootTag(player);
        if (rootTag != null && rootTag.contains(PER_REALM_KEY, Tag.TAG_COMPOUND)) {
            rootTag.getCompound(PER_REALM_KEY).remove(targetRealm.getSerializedName());
        }
    }

    public static void setBottleneckUntil(Player player, long untilGameTick) {
        getOrCreateRootTag(player).putLong(BOTTLENECK_UNTIL_TICK_KEY, untilGameTick);
    }

    public static boolean isInBottleneck(Player player, long gameTime) {
        CompoundTag rootTag = getRootTag(player);
        return rootTag != null && rootTag.getLong(BOTTLENECK_UNTIL_TICK_KEY) > gameTime;
    }

    public static long getBottleneckRemainingTicks(Player player, long gameTime) {
        CompoundTag rootTag = getRootTag(player);
        return rootTag == null
                ? 0L
                : Math.max(0L, rootTag.getLong(BOTTLENECK_UNTIL_TICK_KEY) - gameTime);
    }

    /** 克隆链复制（需集中接线到 PlayerAptitudeEventHandler.onPlayerClone）。 */
    public static void copy(Player sourcePlayer, Player targetPlayer) {
        CompoundTag sourceData = sourcePlayer.getPersistentData();
        if (sourceData.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            targetPlayer.getPersistentData().put(ROOT_KEY, sourceData.getCompound(ROOT_KEY).copy());
        }
    }

    private static CompoundTag getRootTag(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        return persistentData.contains(ROOT_KEY, Tag.TAG_COMPOUND)
                ? persistentData.getCompound(ROOT_KEY)
                : null;
    }

    private static CompoundTag getOrCreateRootTag(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            persistentData.put(ROOT_KEY, new CompoundTag());
        }
        return persistentData.getCompound(ROOT_KEY);
    }

    private static CompoundTag getRealmTag(Player player, CultivationRealm realm) {
        CompoundTag rootTag = getRootTag(player);
        if (rootTag == null || !rootTag.contains(PER_REALM_KEY, Tag.TAG_COMPOUND)) {
            return null;
        }
        CompoundTag perRealmTag = rootTag.getCompound(PER_REALM_KEY);
        return perRealmTag.contains(realm.getSerializedName(), Tag.TAG_COMPOUND)
                ? perRealmTag.getCompound(realm.getSerializedName())
                : null;
    }

    private static CompoundTag getOrCreateRealmTag(Player player, CultivationRealm realm) {
        CompoundTag rootTag = getOrCreateRootTag(player);
        if (!rootTag.contains(PER_REALM_KEY, Tag.TAG_COMPOUND)) {
            rootTag.put(PER_REALM_KEY, new CompoundTag());
        }
        CompoundTag perRealmTag = rootTag.getCompound(PER_REALM_KEY);
        if (!perRealmTag.contains(realm.getSerializedName(), Tag.TAG_COMPOUND)) {
            perRealmTag.put(realm.getSerializedName(), new CompoundTag());
        }
        return perRealmTag.getCompound(realm.getSerializedName());
    }
}
