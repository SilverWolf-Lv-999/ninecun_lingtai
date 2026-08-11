package com.seraphina.ninecun_lingtai.client.utility;

import net.minecraft.util.Mth;

public final class AnimationUtility {

    public static float progress(long elapsedMilliseconds, long durationMilliseconds) {
        if (durationMilliseconds <= 0L) {
            return 1.0F;
        }
        return Mth.clamp((float) elapsedMilliseconds / durationMilliseconds, 0.0F, 1.0F);
    }

    public static float easeOutCubic(float progress) {
        float inverseProgress = 1.0F - Mth.clamp(progress, 0.0F, 1.0F);
        return 1.0F - inverseProgress * inverseProgress * inverseProgress;
    }

    public static float easeInOutCubic(float progress) {
        float clampedProgress = Mth.clamp(progress, 0.0F, 1.0F);
        if (clampedProgress < 0.5F) {
            return 4.0F * clampedProgress * clampedProgress * clampedProgress;
        }
        float inverseProgress = -2.0F * clampedProgress + 2.0F;
        return 1.0F - inverseProgress * inverseProgress * inverseProgress / 2.0F;
    }

    public static float easeOutBack(float progress) {
        float clampedProgress = Mth.clamp(progress, 0.0F, 1.0F);
        float overshoot = 1.70158F;
        float shiftedProgress = clampedProgress - 1.0F;
        return 1.0F + (overshoot + 1.0F) * shiftedProgress * shiftedProgress * shiftedProgress
                + overshoot * shiftedProgress * shiftedProgress;
    }

    public static float approach(float currentValue, float targetValue, float deltaSeconds, float speed) {
        float blend = 1.0F - (float) Math.exp(-Math.max(0.0F, deltaSeconds) * speed);
        return Mth.lerp(blend, currentValue, targetValue);
    }
}
