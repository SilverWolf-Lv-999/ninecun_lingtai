package net.seraphina.ninecun_lingtai.client.utility;

public final class BezierUtility {

    public static float cubic(
            float progress,
            float start,
            float controlStart,
            float controlEnd,
            float end
    ) {
        float clampedProgress = Math.max(0.0F, Math.min(1.0F, progress));
        float inverseProgress = 1.0F - clampedProgress;
        return inverseProgress * inverseProgress * inverseProgress * start
                + 3.0F * inverseProgress * inverseProgress * clampedProgress * controlStart
                + 3.0F * inverseProgress * clampedProgress * clampedProgress * controlEnd
                + clampedProgress * clampedProgress * clampedProgress * end;
    }
}
