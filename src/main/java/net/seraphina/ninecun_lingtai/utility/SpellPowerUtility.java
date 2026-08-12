package net.seraphina.ninecun_lingtai.utility;

import net.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueMasteryStage;

import java.util.Optional;

public final class SpellPowerUtility {
    public static final double MAJOR_REALM_POWER_MULTIPLIER = 6.0D;
    public static final double MINOR_LAYER_POWER_MULTIPLIER = 1.12D;

    /**
     * 熟练度在相邻掌握阶段之间连续提升威力，同时不会低于已经参悟到的掌握阶段。
     */
    public static double getProficiencyEfficiency(
            TechniqueMasteryStage masteryStage,
            long proficiency
    ) {
        double proficiencyEfficiency = getEfficiencyForProficiency(Math.max(0L, proficiency));
        return Math.max(masteryStage.getEfficiency(), proficiencyEfficiency);
    }

    /**
     * 灵根长度就是施法资质：一寸只能发挥九分之一，九寸能够完全发挥。
     */
    public static double getAptitudeEfficiency(Optional<SpiritualRoot> spiritualRoot) {
        return spiritualRoot.map(root -> (double) root.quality()).orElse(0.0D);
    }

    public static double getPowerEfficiency(
            TechniqueMasteryStage masteryStage,
            long proficiency,
            Optional<SpiritualRoot> spiritualRoot
    ) {
        return getProficiencyEfficiency(masteryStage, proficiency)
                * getAptitudeEfficiency(spiritualRoot);
    }

    /**
     * 本次施法相对练气一层基础威力的总倍率。
     * 境界、熟练度与灵根资质统一汇入此倍率，法术效果与灵力消耗共用同一结果。
     */
    public static double getPowerMultiplier(int cultivationStrength, double powerEfficiency) {
        if (!Double.isFinite(powerEfficiency) || powerEfficiency < 0.0D) {
            throw new IllegalArgumentException("powerEfficiency must be a non-negative finite number");
        }
        return getCultivationPower(cultivationStrength) * powerEfficiency;
    }

    /**
     * 练气一层为一倍；每跨一个大境界提升六倍，同境界内每升一层提升一成二。
     */
    public static double getCultivationPower(int cultivationStrength) {
        int progressionIndex = Math.max(1, cultivationStrength) - 1;
        int majorRealmIndex = progressionIndex / CultivationStage.MAXIMUM_LAYER;
        int minorLayerIndex = progressionIndex % CultivationStage.MAXIMUM_LAYER;
        return Math.pow(MAJOR_REALM_POWER_MULTIPLIER, majorRealmIndex)
                * Math.pow(MINOR_LAYER_POWER_MULTIPLIER, minorLayerIndex);
    }

    private static double getEfficiencyForProficiency(long proficiency) {
        TechniqueMasteryStage currentStage = TechniqueMasteryStage.getStageForProficiency(proficiency);
        Optional<TechniqueMasteryStage> nextStage = currentStage.next();
        if (nextStage.isEmpty()) {
            return currentStage.getEfficiency();
        }
        long currentRequirement = currentStage.getRequiredProficiency();
        long nextRequirement = nextStage.get().getRequiredProficiency();
        double progress = (proficiency - currentRequirement)
                / (double) (nextRequirement - currentRequirement);
        return currentStage.getEfficiency()
                + (nextStage.get().getEfficiency() - currentStage.getEfficiency()) * progress;
    }
}
