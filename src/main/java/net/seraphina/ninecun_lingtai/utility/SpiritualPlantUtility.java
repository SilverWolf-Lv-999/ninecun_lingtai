package net.seraphina.ninecun_lingtai.utility;

import net.seraphina.ninecun_lingtai.api.plant.SpiritualPlantGrade;

public final class SpiritualPlantUtility {
    public static final double GRADE_STEP_MULTIPLIER = 0.5D;
    public static final int MINIMUM_GRADE_OFFSET = -2;
    public static final int MAXIMUM_GRADE_OFFSET = 4;

    private SpiritualPlantUtility() {
    }

    public static SpiritualPlantGrade calculateGrade(
            SpiritualPlantGrade baseGrade,
            double elementEnvironmentMultiplier
    ) {
        return baseGrade.offset(calculateGradeOffset(elementEnvironmentMultiplier));
    }

    public static int calculateGradeOffset(double elementEnvironmentMultiplier) {
        if (!Double.isFinite(elementEnvironmentMultiplier) || elementEnvironmentMultiplier < 0.0D) {
            throw new IllegalArgumentException("Element environment multiplier must be finite and non-negative");
        }
        int gradeOffset = (int) Math.floor(
                (elementEnvironmentMultiplier - 1.0D) / GRADE_STEP_MULTIPLIER
        );
        return Math.max(MINIMUM_GRADE_OFFSET, Math.min(MAXIMUM_GRADE_OFFSET, gradeOffset));
    }
}
