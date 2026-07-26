package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import com.seraphina.ninecun_lingtai.common.config.CommonConfig;
import com.seraphina.ninecun_lingtai.common.lifespan.LifespanSnapshot;
import com.seraphina.ninecun_lingtai.common.player.PlayerCultivationData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.player.Player;

public final class LifespanUtility {
    public static final int DAYS_PER_YEAR = 36;
    public static final long TICKS_PER_DAY = 24000L;
    public static final long TICKS_PER_YEAR = DAYS_PER_YEAR * TICKS_PER_DAY;

    private LifespanUtility() {
    }

    public static boolean hasLifespan(LivingEntity entity) {
        return entity.getMobType() != MobType.UNDEAD
                && (entity instanceof Player || entity instanceof Mob);
    }

    public static boolean isImmortal(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        return PlayerCultivationData.getStage(player)
                .map(stage -> stage.realm().ordinal() >= CultivationRealm.SPIRIT_TRANSFORMATION.ordinal())
                .orElse(false);
    }

    public static int getMaximumLifespanYears(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return CommonConfig.DEFAULT_CREATURE_LIFESPAN_YEARS.get();
        }
        return PlayerCultivationData.getStage(player)
                .map(stage -> switch (stage.realm()) {
                    case QI_REFINING -> CommonConfig.QI_REFINING_LIFESPAN_YEARS.get();
                    case FOUNDATION_ESTABLISHMENT -> CommonConfig.FOUNDATION_ESTABLISHMENT_LIFESPAN_YEARS.get();
                    case GOLDEN_CORE -> CommonConfig.GOLDEN_CORE_LIFESPAN_YEARS.get();
                    case NASCENT_SOUL -> CommonConfig.NASCENT_SOUL_LIFESPAN_YEARS.get();
                    case SPIRIT_TRANSFORMATION, DAO_INTEGRATION, VOID_REFINEMENT, MAHAYANA, IMMORTAL -> 0;
                })
                .orElseGet(CommonConfig.MORTAL_PLAYER_LIFESPAN_YEARS::get);
    }

    public static long ticksForYears(int years) {
        if (years <= 0) {
            return 0L;
        }
        if (years > Long.MAX_VALUE / TICKS_PER_YEAR) {
            return Long.MAX_VALUE;
        }
        return years * TICKS_PER_YEAR;
    }

    public static String formatAge(long ageTicks) {
        return formatYearsAndDays(ageTicks, "岁");
    }

    public static String formatRemaining(LifespanSnapshot lifespan) {
        if (lifespan.immortal()) {
            return "与天地齐寿";
        }
        return formatYearsAndDays(lifespan.remainingTicks(), "年");
    }

    private static String formatYearsAndDays(long ticks, String yearUnit) {
        long nonNegativeTicks = Math.max(0L, ticks);
        long totalDays = nonNegativeTicks / TICKS_PER_DAY;
        long years = totalDays / DAYS_PER_YEAR;
        long days = totalDays % DAYS_PER_YEAR;
        if (days == 0L) {
            return years + yearUnit;
        }
        return years + yearUnit + days + "天";
    }
}
