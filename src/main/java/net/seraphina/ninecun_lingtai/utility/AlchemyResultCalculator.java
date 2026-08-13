package net.seraphina.ninecun_lingtai.utility;

import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyBrewScore;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeDefinition;
import net.seraphina.ninecun_lingtai.api.aptitude.SoulStrength;
import net.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.api.element.ElementRelation;
import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.minecraft.util.RandomSource;

import javax.annotation.Nullable;
import java.util.Optional;

public final class AlchemyResultCalculator {
    public static final double MINIMUM_SUCCESS_RATE = 0.05D;
    public static final double MAXIMUM_SUCCESS_RATE = 0.98D;
    public static final double EXPLOSION_THRESHOLD = 0.8D;
    public static final double WINDOW_START = 1.0D;
    public static final double WINDOW_END = 1.3D;
    public static final double CHARRED_THRESHOLD = 1.6D;
    public static final double OVERFIRE_PENALTY_STEP = 0.15D;

    private static final double DEFAULT_FIRE_WEIGHT = 0.35D;
    private static final double DEFAULT_ENVIRONMENT_WEIGHT = 0.20D;
    private static final double DEFAULT_ROOT_WEIGHT = 0.15D;
    private static final double DEFAULT_SOUL_WEIGHT = 0.15D;
    private static final double DEFAULT_REALM_WEIGHT = 0.15D;
    private static final double WEIGHT_SUM_TOLERANCE = 0.001D;

    public static double calculateRootScore(
            Optional<SpiritualRoot> spiritualRoot,
            @Nullable ElementType primaryElement
    ) {
        if (primaryElement == null) {
            return 1.0D;
        }
        if (spiritualRoot.isEmpty()) {
            return 0.0D;
        }
        SpiritualRoot root = spiritualRoot.get();
        double qualityFactor = 0.6D + 0.4D * root.quality();
        if (root.isChaos()) {
            return 0.90D * qualityFactor;
        }
        double bestCoefficient = 0.0D;
        for (ElementType rootElement : root.elements()) {
            bestCoefficient = Math.max(
                    bestCoefficient,
                    getRelationCoefficient(rootElement.getRelationTo(primaryElement))
            );
        }
        return bestCoefficient * qualityFactor;
    }

    private static double getRelationCoefficient(ElementRelation relation) {
        return switch (relation) {
            case SAME -> 1.00D;
            case GENERATES -> 0.92D;
            case GENERATED_BY -> 0.85D;
            case OVERCOMES, OVERCOME_BY -> 0.60D;
            default -> 0.75D;
        };
    }

    public static double calculateSoulScore(Optional<SoulStrength> soulStrength) {
        return 0.55D + 0.45D * soulStrength.map(SoulStrength::quality).orElse(0.0F);
    }

    public static double calculateRealmScore(Optional<CultivationStage> stage, int tier) {
        if (stage.isEmpty()) {
            return 0.0D;
        }
        int delta = stage.get().realm().ordinal() - (tier - 1);
        if (delta < 0) {
            return 0.0D;
        }
        if (delta == 0) {
            return 0.85D + 0.15D * stage.get().layer() / (double) CultivationStage.MAXIMUM_LAYER;
        }
        return delta == 1 ? 1.05D : 1.10D;
    }

    public static double calculateEnvironmentScore(double multiplier, boolean hasPrimaryElement) {
        if (!hasPrimaryElement) {
            return 1.0D;
        }
        if (multiplier <= 0.0D) {
            return 0.55D;
        }
        if (multiplier < 1.0D) {
            return 0.55D + 0.45D * multiplier;
        }
        return Math.min(1.10D, 1.0D + 0.10D * (multiplier - 1.0D));
    }

    public static double calculateFireScore(
            int hitTicks,
            int totalTicks,
            double rootQuality,
            boolean hasHeatCurve
    ) {
        if (!hasHeatCurve) {
            return 1.0D;
        }
        double rawScore = getRawFireScore(hitTicks, totalTicks, true);
        return Math.pow(rawScore, 1.6D - 0.6D * clamp01(rootQuality));
    }

    public static double getRawFireScore(int hitTicks, int totalTicks, boolean hasHeatCurve) {
        if (!hasHeatCurve) {
            return 1.0D;
        }
        if (totalTicks <= 0) {
            return 0.0D;
        }
        return clamp01(hitTicks / (double) totalTicks);
    }

    public static AlchemyBrewScore combine(
            double fireScore,
            double environmentScore,
            double rootScore,
            double soulScore,
            double realmScore,
            double baseSuccessRate
    ) {
        double fireWeight = CommonConfig.ALCHEMY_FIRE_SCORE_WEIGHT.get();
        double environmentWeight = CommonConfig.ALCHEMY_ENVIRONMENT_SCORE_WEIGHT.get();
        double rootWeight = CommonConfig.ALCHEMY_ROOT_SCORE_WEIGHT.get();
        double soulWeight = CommonConfig.ALCHEMY_SOUL_SCORE_WEIGHT.get();
        double realmWeight = CommonConfig.ALCHEMY_REALM_SCORE_WEIGHT.get();
        double weightSum = fireWeight + environmentWeight + rootWeight + soulWeight + realmWeight;
        if (Math.abs(weightSum - 1.0D) > WEIGHT_SUM_TOLERANCE) {
            fireWeight = DEFAULT_FIRE_WEIGHT;
            environmentWeight = DEFAULT_ENVIRONMENT_WEIGHT;
            rootWeight = DEFAULT_ROOT_WEIGHT;
            soulWeight = DEFAULT_SOUL_WEIGHT;
            realmWeight = DEFAULT_REALM_WEIGHT;
        }
        double totalScore = fireScore * fireWeight
                + environmentScore * environmentWeight
                + rootScore * rootWeight
                + soulScore * soulWeight
                + realmScore * realmWeight;
        double successRate = Math.max(
                MINIMUM_SUCCESS_RATE,
                Math.min(MAXIMUM_SUCCESS_RATE, baseSuccessRate * totalScore)
        );
        return new AlchemyBrewScore(
                fireScore,
                environmentScore,
                realmScore,
                rootScore,
                soulScore,
                totalScore,
                successRate
        );
    }

    public static int getRankOffset(double totalScore, double rawFireScore) {
        if (totalScore >= 1.0D && rawFireScore >= 0.9D) {
            return 2;
        }
        if (totalScore >= 0.90D) {
            return 1;
        }
        if (totalScore >= 0.75D) {
            return 0;
        }
        if (totalScore >= 0.60D) {
            return -1;
        }
        return -2;
    }

    public static boolean rollBonusYield(
            double totalScore,
            double rawFireScore,
            RandomSource random
    ) {
        return totalScore >= 1.0D && rawFireScore >= 0.9D && random.nextDouble() < 0.20D;
    }

    public static int getOverfireRankPenalty(double harvestTiming) {
        if (harvestTiming <= WINDOW_END) {
            return 0;
        }
        return (int) Math.ceil((harvestTiming - WINDOW_END) / OVERFIRE_PENALTY_STEP);
    }

    public static int getEffectiveDuration(int brewingDuration, double environmentMultiplier) {
        double clampedMultiplier = Math.max(0.5D, Math.min(1.5D, environmentMultiplier));
        return Math.max(1, (int) Math.round(brewingDuration / clampedMultiplier));
    }

    public static double calculateToxin(AlchemyRecipeDefinition recipe, int rank) {
        double rankFactor = 1.3D - 0.3D * rank / 9.0D;
        double baseFactor = 1.3D - 0.3D / 9.0D;
        return recipe.pillToxin() * rankFactor / baseFactor;
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
