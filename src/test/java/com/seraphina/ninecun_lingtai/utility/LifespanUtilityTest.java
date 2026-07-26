package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.common.lifespan.LifespanSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LifespanUtilityTest {
    @Test
    void oneYearContainsThirtySixMinecraftDays() {
        assertEquals(864000L, LifespanUtility.ticksForYears(1));
    }

    @Test
    void formatsAgeAndRemainingLifespan() {
        long ageTicks = LifespanUtility.TICKS_PER_YEAR + 5L * LifespanUtility.TICKS_PER_DAY;
        LifespanSnapshot lifespan = new LifespanSnapshot(ageTicks, 2, false);

        assertEquals("1岁5天", LifespanUtility.formatAge(lifespan.ageTicks()));
        assertEquals("0年31天", LifespanUtility.formatRemaining(lifespan));
        assertFalse(lifespan.expired());
    }

    @Test
    void immortalLifespanNeverExpires() {
        LifespanSnapshot lifespan = new LifespanSnapshot(Long.MAX_VALUE, 0, true);

        assertEquals("与天地齐寿", LifespanUtility.formatRemaining(lifespan));
        assertFalse(lifespan.expired());
    }

    @Test
    void finiteLifespanExpiresAtMaximumAge() {
        LifespanSnapshot lifespan = new LifespanSnapshot(LifespanUtility.ticksForYears(30), 30, false);

        assertTrue(lifespan.expired());
        assertEquals(0L, lifespan.remainingTicks());
    }
}
