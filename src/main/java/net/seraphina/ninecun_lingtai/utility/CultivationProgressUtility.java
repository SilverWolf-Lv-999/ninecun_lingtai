package net.seraphina.ninecun_lingtai.utility;

import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;

public final class CultivationProgressUtility {
    private static final double[] CHAOS_QI_CONVERSION_COSTS = {
            100.0D, 500.0D, 1000.0D, 2000.0D, 3000.0D,
            4500.0D, 6000.0D, 8000.0D, 10000.0D
    };
    private static final double[] HEAVENLY_LAW_REQUIREMENTS = {
            10.0D, 100.0D, 220.0D, 360.0D, 520.0D,
            660.0D, 780.0D, 860.0D, 900.0D, 1000.0D,
            1250.0D, 1500.0D, 1750.0D, 2000.0D, 2250.0D,
            2500.0D, 2750.0D, 2900.0D
    };

    public static boolean usesSpiritualEnergy(CultivationStage stage) {
        return stage.realm().ordinal() <= CultivationRealm.NASCENT_SOUL.ordinal();
    }

    public static boolean usesChaosQi(CultivationStage stage) {
        return stage.realm() == CultivationRealm.SPIRIT_TRANSFORMATION;
    }

    public static boolean usesHeavenlyLaws(CultivationStage stage) {
        return stage.realm() == CultivationRealm.DAO_INTEGRATION
                || stage.realm() == CultivationRealm.VOID_REFINEMENT;
    }

    public static boolean usesImmortalEssence(CultivationStage stage) {
        return stage.realm() == CultivationRealm.MAHAYANA;
    }

    public static double getChaosQiConversionCost(CultivationStage stage) {
        if (!usesChaosQi(stage)) {
            return 0.0D;
        }
        return CHAOS_QI_CONVERSION_COSTS[stage.layer() - 1];
    }

    public static double getChaosQiRequirement(CultivationStage stage) {
        return usesChaosQi(stage) ? stage.layer() : 0.0D;
    }

    public static double getHeavenlyLawRequirement(CultivationStage stage) {
        if (stage.realm() == CultivationRealm.DAO_INTEGRATION) {
            return HEAVENLY_LAW_REQUIREMENTS[stage.layer() - 1];
        }
        if (stage.realm() == CultivationRealm.VOID_REFINEMENT) {
            return HEAVENLY_LAW_REQUIREMENTS[8 + stage.layer()];
        }
        if (stage.realm() == CultivationRealm.MAHAYANA) {
            return 3000.0D;
        }
        return 0.0D;
    }

    public static double getImmortalEssenceRequirement(CultivationStage stage) {
        return stage.realm() == CultivationRealm.MAHAYANA
                ? stage.layer() * (double) stage.layer()
                : stage.realm() == CultivationRealm.IMMORTAL ? 81.0D : 0.0D;
    }

    public static double getHeavenlyLawGainPerSecond(int soulStars) {
        return Math.max(1, soulStars) / (40.0D * 60.0D);
    }

    public static long getImmortalEssenceMillisPerDrop(CultivationStage stage) {
        int layer = Math.max(1, stage.layer());
        return Math.max(1L, Math.round(180000.0D * Math.pow(10.0D / 3.0D, layer - 1)));
    }

    public static long getImmortalBreakthroughMillis(CultivationStage stage, int soulStars) {
        long[] delays = {100L, 120L, 160L, 220L, 300L, 400L, 550L, 700L};
        if (stage.realm() != CultivationRealm.IMMORTAL || stage.layer() >= CultivationStage.MAXIMUM_LAYER) {
            return Long.MAX_VALUE;
        }
        return Math.max(1L, delays[stage.layer() - 1] * 60L * 60L * 1000L / Math.max(1, soulStars));
    }
}
