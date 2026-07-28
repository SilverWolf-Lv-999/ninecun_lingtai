package com.seraphina.ninecun_lingtai.client.utility;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationAnimationUtilityTest {
    private static final int HOLD_TICKS = 80;

    @Test
    void fadesInBeforeUnrolling() {
        assertEquals(0.0F, NotificationAnimationUtility.getOpacity(0.0F, HOLD_TICKS), 0.0001F);
        assertEquals(0.0F, NotificationAnimationUtility.getUnrollProgress(7.0F, HOLD_TICKS), 0.0001F);
        assertEquals(1.0F, NotificationAnimationUtility.getOpacity(8.0F, HOLD_TICKS), 0.0001F);
    }

    @Test
    void fullyOpensAndThenRollsUpBeforeFadingOut() {
        int openedAt = NotificationAnimationUtility.FADE_IN_TICKS
                + NotificationAnimationUtility.UNROLL_TICKS;
        int rollUpStart = openedAt + HOLD_TICKS;
        int fadeOutStart = rollUpStart + NotificationAnimationUtility.ROLL_UP_TICKS;

        assertEquals(1.0F, NotificationAnimationUtility.getUnrollProgress(openedAt, HOLD_TICKS), 0.0001F);
        assertEquals(1.0F, NotificationAnimationUtility.getUnrollProgress(rollUpStart, HOLD_TICKS), 0.0001F);
        assertEquals(0.0F, NotificationAnimationUtility.getUnrollProgress(fadeOutStart, HOLD_TICKS), 0.0001F);
        assertEquals(1.0F, NotificationAnimationUtility.getOpacity(fadeOutStart, HOLD_TICKS), 0.0001F);
        assertEquals(0.0F, NotificationAnimationUtility.getOpacity(
                NotificationAnimationUtility.getTotalTicks(HOLD_TICKS),
                HOLD_TICKS
        ), 0.0001F);
    }
}
