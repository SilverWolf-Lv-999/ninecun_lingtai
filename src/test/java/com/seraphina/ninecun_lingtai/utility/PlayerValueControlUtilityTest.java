package com.seraphina.ninecun_lingtai.utility;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerValueControlUtilityTest {
    @Test
    void sanitizeApplicationClampsToPlayableRange() {
        assertEquals(0.0D, PlayerValueControlUtility.sanitizeApplication(-1.0D));
        assertEquals(0.5D, PlayerValueControlUtility.sanitizeApplication(0.5D));
        assertEquals(1.0D, PlayerValueControlUtility.sanitizeApplication(2.0D));
    }

    @Test
    void sanitizeApplicationFallsBackForInvalidNumbers() {
        assertEquals(1.0D, PlayerValueControlUtility.sanitizeApplication(Double.NaN));
        assertEquals(1.0D, PlayerValueControlUtility.sanitizeApplication(Double.POSITIVE_INFINITY));
    }

    @Test
    void applyApplicationScalesValue() {
        assertEquals(7.5D, PlayerValueControlUtility.applyApplication(15.0D, 0.5D));
    }
}
