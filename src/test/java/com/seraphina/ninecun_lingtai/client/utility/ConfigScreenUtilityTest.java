package com.seraphina.ninecun_lingtai.client.utility;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigScreenUtilityTest {
    @Test
    void decimalInputAllowsIncompleteValuesWhileTyping() {
        assertTrue(ConfigScreenUtility.isPotentialDecimal(""));
        assertTrue(ConfigScreenUtility.isPotentialDecimal("-"));
        assertTrue(ConfigScreenUtility.isPotentialDecimal("12."));
        assertTrue(ConfigScreenUtility.isPotentialDecimal(".5"));
        assertFalse(ConfigScreenUtility.isPotentialDecimal("1.2.3"));
        assertFalse(ConfigScreenUtility.isPotentialDecimal("value"));
    }

    @Test
    void parseDoubleRequiresFiniteValueWithinRange() {
        assertEquals(1.25D, ConfigScreenUtility.parseDoubleInRange("1.25", 0.0D, 2.0D).orElseThrow());
        assertTrue(ConfigScreenUtility.parseDoubleInRange("", 0.0D, 2.0D).isEmpty());
        assertTrue(ConfigScreenUtility.parseDoubleInRange("3", 0.0D, 2.0D).isEmpty());
        assertTrue(ConfigScreenUtility.parseDoubleInRange("NaN", 0.0D, 2.0D).isEmpty());
    }

    @Test
    void formatDoubleRemovesUnnecessaryZeroes() {
        assertEquals("1", ConfigScreenUtility.formatDouble(1.0D));
        assertEquals("1.25", ConfigScreenUtility.formatDouble(1.25D));
    }
}
