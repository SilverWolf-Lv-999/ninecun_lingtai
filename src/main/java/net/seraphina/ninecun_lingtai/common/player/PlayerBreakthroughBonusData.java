package net.seraphina.ninecun_lingtai.common.player;

import net.seraphina.ninecun_lingtai.ModMain;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

/**
 * 破境丹"成功率加成"数据（T13 破境丹重定位，与旧的能量折扣
 * {@link PlayerBreakthroughDiscountData} 并存、互不影响）。
 * 同类只吃一颗：写入时不叠加、取 max；服用时若已有不低于新值的加成返回 false，
 * 由 PillEffectApplier 的 breakthrough_success_bonus case 提示玩家（需集中接线）。
 */
public final class PlayerBreakthroughBonusData {
    private static final String BONUS_KEY = ModMain.MOD_ID + ":breakthrough_success_bonus";
    private static final String BONUS_VALUE_KEY = "Bonus";
    private static final String EXPIRE_GAME_TIME_KEY = "ExpireGameTime";
    /** 本次冲关（掷骰一刻）是否借了破境丹之力——畏雷心障化解校验用。 */
    private static final String ATTEMPT_USED_PILL_KEY = ModMain.MOD_ID + ":breakthrough_attempt_used_pill";
    private static final double MAXIMUM_BONUS = 0.5D;

    private PlayerBreakthroughBonusData() {
    }

    /** 当前有效的成功率加成（加算项，0 表示未服丹或已过期）。 */
    public static double getSuccessBonus(Player player, long gameTime) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(BONUS_KEY, Tag.TAG_COMPOUND)) {
            return 0.0D;
        }
        CompoundTag bonusData = persistentData.getCompound(BONUS_KEY);
        if (bonusData.getLong(EXPIRE_GAME_TIME_KEY) < gameTime) {
            persistentData.remove(BONUS_KEY);
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(MAXIMUM_BONUS, bonusData.getDouble(BONUS_VALUE_KEY)));
    }

    /**
     * 服丹写入。已有未过期且不低于新值的加成时不写入并返回 false（同类只吃一颗）；
     * 否则取 max 写入并返回 true。
     */
    public static boolean applyBonus(Player player, double bonus, long expireGameTime) {
        double currentBonus = getSuccessBonus(player, player.level().getGameTime());
        if (currentBonus >= bonus) {
            return false;
        }
        CompoundTag bonusData = new CompoundTag();
        bonusData.putDouble(BONUS_VALUE_KEY, Math.min(MAXIMUM_BONUS, bonus));
        bonusData.putLong(EXPIRE_GAME_TIME_KEY, expireGameTime);
        player.getPersistentData().put(BONUS_KEY, bonusData);
        return true;
    }

    /** 破境掷骰后（无论成败）清除加成——一颗丹只保一次冲关。 */
    public static void clearBonus(Player player) {
        player.getPersistentData().remove(BONUS_KEY);
    }

    /**
     * 掷骰清加成之前记录"本次冲关是否借了破境丹之力"——加成在 roll 时即被清除，
     * 渡劫成功结算（tickAftermath）无法再从加成本身判断，畏雷心障化解校验依赖此标记。
     */
    public static void recordAttemptPillUse(Player player, boolean usedPill) {
        player.getPersistentData().putBoolean(ATTEMPT_USED_PILL_KEY, usedPill);
    }

    /**
     * 消费"本次冲关是否吃了破境丹"标记（读后即清）。未经历掷骰（无标记）时回落为
     * 查询当前生效加成，兼容不走仪式的路径。
     */
    public static boolean consumeAttemptPillUse(Player player, long gameTime) {
        CompoundTag persistentData = player.getPersistentData();
        if (persistentData.contains(ATTEMPT_USED_PILL_KEY)) {
            boolean usedPill = persistentData.getBoolean(ATTEMPT_USED_PILL_KEY);
            persistentData.remove(ATTEMPT_USED_PILL_KEY);
            return usedPill;
        }
        return getSuccessBonus(player, gameTime) > 0.0D;
    }

    /** 克隆链复制（需集中接线到 PlayerAptitudeEventHandler.onPlayerClone）。 */
    public static void copy(Player sourcePlayer, Player targetPlayer) {
        CompoundTag sourceData = sourcePlayer.getPersistentData();
        if (sourceData.contains(BONUS_KEY, Tag.TAG_COMPOUND)) {
            targetPlayer.getPersistentData().put(BONUS_KEY, sourceData.getCompound(BONUS_KEY).copy());
        }
    }
}
