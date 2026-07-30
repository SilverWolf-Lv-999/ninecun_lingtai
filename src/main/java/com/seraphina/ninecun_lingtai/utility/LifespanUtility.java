package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.common.config.CommonConfig;
import com.seraphina.ninecun_lingtai.common.creature.CreatureCultivationData;
import com.seraphina.ninecun_lingtai.common.lifespan.LifespanSnapshot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class LifespanUtility {
    public static final int DAYS_PER_YEAR = 36;
    public static final long TICKS_PER_DAY = 24000L;
    public static final long TICKS_PER_YEAR = DAYS_PER_YEAR * TICKS_PER_DAY;

    private LifespanUtility() {
    }

    public static boolean hasLifespan(LivingEntity entity) {
        return (entity.getMobType() != MobType.UNDEAD || isBossLevelCreature(entity))
                && (entity instanceof Player || entity instanceof Mob);
    }

    public static boolean isImmortal(LivingEntity entity) {
        return CreatureCultivationData.getStage(entity)
                .map(stage -> stage.realm().ordinal() >= CultivationRealm.SPIRIT_TRANSFORMATION.ordinal())
                .orElse(false);
    }

    public static int getMaximumLifespanYears(LivingEntity entity) {
        int baseLifespanYears;
        if (entity instanceof Player) {
            baseLifespanYears = CommonConfig.MORTAL_PLAYER_LIFESPAN_YEARS.get();
        } else if (isBossLevelCreature(entity)) {
            baseLifespanYears = CommonConfig.BOSS_CREATURE_LIFESPAN_YEARS.get();
        } else {
            baseLifespanYears = CommonConfig.DEFAULT_CREATURE_LIFESPAN_YEARS.get();
        }
        return getMaximumLifespanYears(CreatureCultivationData.getStage(entity), baseLifespanYears);
    }

    public static int getMaximumLifespanYears(
            Optional<CultivationStage> stage,
            int baseLifespanYears
    ) {
        return stage
                .map(cultivationStage -> getEffectiveMaximumLifespanYears(
                        baseLifespanYears,
                        getCultivationLifespanYears(cultivationStage),
                        cultivationStage.realm().ordinal() >= CultivationRealm.SPIRIT_TRANSFORMATION.ordinal()
                ))
                .orElseGet(() -> getEffectiveMaximumLifespanYears(baseLifespanYears, 0, false));
    }

    public static int getEffectiveMaximumLifespanYears(
            int baseLifespanYears,
            int cultivationLifespanYears,
            boolean immortal
    ) {
        if (immortal) {
            return 0;
        }
        return Math.max(Math.max(0, baseLifespanYears), Math.max(0, cultivationLifespanYears));
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

    public static long ticksForYearsAndDays(int years, int days) {
        long dayTicks = Math.max(0, days) * TICKS_PER_DAY;
        return saturatingAdd(ticksForYears(years), dayTicks);
    }

    public static String formatMaximum(LifespanSnapshot lifespan) {
        if (lifespan.immortal()) {
            return "无上限（长生）";
        }
        return lifespan.maximumLifespanYears() + "年";
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

    private static int getCultivationLifespanYears(CultivationStage stage) {
        return switch (stage.realm()) {
            case QI_REFINING -> CommonConfig.QI_REFINING_LIFESPAN_YEARS.get();
            case FOUNDATION_ESTABLISHMENT -> CommonConfig.FOUNDATION_ESTABLISHMENT_LIFESPAN_YEARS.get();
            case GOLDEN_CORE -> CommonConfig.GOLDEN_CORE_LIFESPAN_YEARS.get();
            case NASCENT_SOUL -> CommonConfig.NASCENT_SOUL_LIFESPAN_YEARS.get();
            case SPIRIT_TRANSFORMATION, DAO_INTEGRATION, VOID_REFINEMENT, MAHAYANA, IMMORTAL -> 0;
        };
    }

    private static boolean isBossLevelCreature(LivingEntity entity) {
        return entity instanceof Warden
                || entity instanceof WitherBoss
                || entity instanceof EnderDragon;
    }

    private static long saturatingAdd(long left, long right) {
        if (right > Long.MAX_VALUE - left) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }
}
