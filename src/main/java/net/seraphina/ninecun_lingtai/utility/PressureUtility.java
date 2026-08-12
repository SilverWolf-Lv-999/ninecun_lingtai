package net.seraphina.ninecun_lingtai.utility;

import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.common.config.CommonConfig;

import java.util.Optional;

public final class PressureUtility {
    private static final double DEFAULT_BASE_RANGE = 5.0D;
    private static final double DEFAULT_LAYER_RANGE_BONUS = 1.0D;
    private static final double DEFAULT_REALM_RANGE_BONUS = 30.0D;
    private static final double DEFAULT_MAX_RANGE = 2000.0D;
    private static final int DEFAULT_INSTANT_DEATH_REALM_GAP = 2;
    private static final int NON_CULTIVATOR_RANK = -1;

    public static int getCultivationRank(CultivationStage stage) {
        return stage.realm().ordinal() * CultivationStage.MAXIMUM_LAYER
                + stage.layer()
                - CultivationStage.MINIMUM_LAYER;
    }

    public static int getCultivationRank(Optional<CultivationStage> stage) {
        return stage.map(PressureUtility::getCultivationRank).orElse(NON_CULTIVATOR_RANK);
    }

    public static double getPressureRange(CultivationStage stage) {
        double range = getDouble(CommonConfig.PRESSURE_BASE_RANGE, DEFAULT_BASE_RANGE)
                + stage.realm().ordinal() * getDouble(
                CommonConfig.PRESSURE_REALM_RANGE_BONUS,
                DEFAULT_REALM_RANGE_BONUS
        )
                + (stage.layer() - CultivationStage.MINIMUM_LAYER)
                * getDouble(CommonConfig.PRESSURE_LAYER_RANGE_BONUS, DEFAULT_LAYER_RANGE_BONUS);
        return Math.min(getDouble(CommonConfig.PRESSURE_MAX_RANGE, DEFAULT_MAX_RANGE), Math.max(0.0D, range));
    }

    public static boolean hasAdvancedEffect(CultivationStage sourceStage) {
        return sourceStage.realm().ordinal() >= CultivationRealm.FOUNDATION_ESTABLISHMENT.ordinal();
    }

    public static boolean isLower(CultivationStage sourceStage, Optional<CultivationStage> targetStage) {
        return getCultivationRank(sourceStage) > getCultivationRank(targetStage);
    }

    public static boolean shouldInstantlyKill(
            CultivationStage sourceStage,
            Optional<CultivationStage> targetStage
    ) {
        int realmGap = sourceStage.realm().ordinal()
                - targetStage.map(stage -> stage.realm().ordinal()).orElse(NON_CULTIVATOR_RANK);
        return hasAdvancedEffect(sourceStage)
                && realmGap >= getInt(
                CommonConfig.PRESSURE_INSTANT_DEATH_REALM_GAP,
                DEFAULT_INSTANT_DEATH_REALM_GAP
        );
    }

    public static float getSeverity(CultivationStage sourceStage, Optional<CultivationStage> targetStage) {
        int rankGap = Math.max(1, getCultivationRank(sourceStage) - getCultivationRank(targetStage));
        return (float) Math.min(1.0D, 0.25D + rankGap / 18.0D);
    }

    private static double getDouble(net.minecraftforge.common.ForgeConfigSpec.DoubleValue value, double fallback) {
        try {
            return value.get();
        } catch (IllegalStateException exception) {
            return fallback;
        }
    }

    private static int getInt(net.minecraftforge.common.ForgeConfigSpec.IntValue value, int fallback) {
        try {
            return value.get();
        } catch (IllegalStateException exception) {
            return fallback;
        }
    }
}
