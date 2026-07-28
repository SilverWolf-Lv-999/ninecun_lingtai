package com.seraphina.ninecun_lingtai.client.utility;

import com.seraphina.ninecun_lingtai.utility.NotificationUtility;
import net.minecraft.util.Mth;

public final class NotificationAnimationUtility {
    public static final int FADE_IN_TICKS = 8;
    public static final int UNROLL_TICKS = 18;
    public static final int ROLL_UP_TICKS = 18;
    public static final int FADE_OUT_TICKS = 8;
    public static final int MINIMUM_HOLD_TICKS = NotificationUtility.MINIMUM_DURATION_TICKS;
    public static final int MAXIMUM_HOLD_TICKS = NotificationUtility.MAXIMUM_DURATION_TICKS;

    private NotificationAnimationUtility() {
    }

    public static int clampHoldTicks(int holdTicks) {
        return NotificationUtility.clampDurationTicks(holdTicks);
    }

    public static int getTotalTicks(int holdTicks) {
        return FADE_IN_TICKS
                + UNROLL_TICKS
                + clampHoldTicks(holdTicks)
                + ROLL_UP_TICKS
                + FADE_OUT_TICKS;
    }

    public static float getOpacity(float ageTicks, int holdTicks) {
        float clampedAge = Math.max(0.0F, ageTicks);
        if (clampedAge < FADE_IN_TICKS) {
            return smoothStep(clampedAge / FADE_IN_TICKS);
        }
        int fadeOutStart = FADE_IN_TICKS + UNROLL_TICKS + clampHoldTicks(holdTicks) + ROLL_UP_TICKS;
        if (clampedAge >= fadeOutStart) {
            return 1.0F - smoothStep((clampedAge - fadeOutStart) / FADE_OUT_TICKS);
        }
        return 1.0F;
    }

    public static float getUnrollProgress(float ageTicks, int holdTicks) {
        float clampedAge = Math.max(0.0F, ageTicks);
        if (clampedAge < FADE_IN_TICKS) {
            return 0.0F;
        }
        float unrollEnd = FADE_IN_TICKS + UNROLL_TICKS;
        if (clampedAge < unrollEnd) {
            return smoothStep((clampedAge - FADE_IN_TICKS) / UNROLL_TICKS);
        }
        int rollUpStart = FADE_IN_TICKS + UNROLL_TICKS + clampHoldTicks(holdTicks);
        if (clampedAge >= rollUpStart) {
            return 1.0F - smoothStep((clampedAge - rollUpStart) / ROLL_UP_TICKS);
        }
        return 1.0F;
    }

    public static float getTextRevealProgress(float ageTicks) {
        float revealStart = FADE_IN_TICKS + UNROLL_TICKS * 0.12F;
        float revealDuration = UNROLL_TICKS * 0.88F;
        return smoothStep((ageTicks - revealStart) / revealDuration);
    }

    private static float smoothStep(float progress) {
        float clampedProgress = Mth.clamp(progress, 0.0F, 1.0F);
        return clampedProgress * clampedProgress * (3.0F - 2.0F * clampedProgress);
    }
}
