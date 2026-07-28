package com.seraphina.ninecun_lingtai.utility;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChineseNumberUtilityTest {
    @Test
    void compactKeepsSmallValuesFullySpelled() {
        assertEquals("零", ChineseNumberUtility.toCompactChinese(BigInteger.ZERO));
        assertEquals("一千九百三十", ChineseNumberUtility.toCompactChinese(BigInteger.valueOf(1930L)));
        assertEquals("九千九百九十九", ChineseNumberUtility.toCompactChinese(BigInteger.valueOf(9999L)));
    }

    @Test
    void compactKeepsOnlyLeadingGroupWithOneFractionDigit() {
        assertEquals("一点二万", ChineseNumberUtility.toCompactChinese(BigInteger.valueOf(12_345L)));
        assertEquals(
                "一千二百三十四点六万",
                ChineseNumberUtility.toCompactChinese(BigInteger.valueOf(12_345_678L))
        );
        assertEquals(
                "一点二亿",
                ChineseNumberUtility.toCompactChinese(BigInteger.valueOf(123_456_789L))
        );
    }

    @Test
    void compactHandlesHugeCultivationRequirements() {
        assertEquals(
                "三千二百五十四点九垓",
                ChineseNumberUtility.toCompactChinese(new BigInteger("325487654321098765432109"))
        );
    }

    @Test
    void compactCarriesRoundingIntoTheNextUnit() {
        assertEquals(
                "一亿",
                ChineseNumberUtility.toCompactChinese(BigInteger.valueOf(99_999_999L))
        );
    }

    @Test
    void compactDecimalKeepsFractionBelowTenThousand() {
        assertEquals("零", ChineseNumberUtility.toCompactChinese(0.0D, 1));
        assertEquals("二十五点五", ChineseNumberUtility.toCompactChinese(25.5D, 1));
        assertEquals(
                "一点二万",
                ChineseNumberUtility.toCompactChinese(new BigDecimal("12345.6"), 1)
        );
    }

    @Test
    void compactHandlesNegativeValues() {
        assertEquals(
                "负一点二万",
                ChineseNumberUtility.toCompactChinese(BigInteger.valueOf(-12_345L))
        );
    }

    @Test
    void formatsDefenseSpellDurationsWithoutArabicDigits() {
        assertEquals("零秒", ChineseNumberUtility.formatDurationTicks(0));
        assertEquals("一秒", ChineseNumberUtility.formatDurationTicks(1));
        assertEquals("一分五秒", ChineseNumberUtility.formatDurationTicks(1_300));
        assertEquals("一时一分一秒", ChineseNumberUtility.formatDurationTicks(73_220));
    }
}
