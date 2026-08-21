package net.seraphina.ninecun_lingtai.utility;

import net.minecraft.util.Mth;

public final class BezierUtility {
    public static float cubicEase(float progress, float firstControlPoint, float secondControlPoint) {
        float clampedProgress = Mth.clamp(progress, 0.0F, 1.0F);
        float inverseProgress = 1.0F - clampedProgress;
        return 3.0F * inverseProgress * inverseProgress * clampedProgress * firstControlPoint
                + 3.0F * inverseProgress * clampedProgress * clampedProgress * secondControlPoint
                + clampedProgress * clampedProgress * clampedProgress;
    }
}
