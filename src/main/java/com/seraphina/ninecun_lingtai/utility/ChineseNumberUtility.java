package com.seraphina.ninecun_lingtai.utility;

public final class ChineseNumberUtility {
    private static final String[] CHINESE_DIGITS = {
            "零", "一", "二", "三", "四", "五", "六", "七", "八", "九"
    };

    private ChineseNumberUtility() {
    }

    public static String formatTenths(float value, String integerUnit, String tenthUnit) {
        int roundedTenths = Math.max(0, Math.round(value * 10.0F));
        int integerPart = roundedTenths / 10;
        int tenthPart = roundedTenths % 10;
        StringBuilder formattedValue = new StringBuilder();
        if (integerPart > 0 || tenthPart == 0) {
            formattedValue.append(toChineseInteger(integerPart)).append(integerUnit);
        }
        if (tenthPart > 0) {
            formattedValue.append(CHINESE_DIGITS[tenthPart]).append(tenthUnit);
        }
        return formattedValue.toString();
    }

    public static String toChineseInteger(int value) {
        if (value < 0 || value > 99) {
            throw new IllegalArgumentException("Chinese integer value must be between zero and ninety-nine");
        }
        if (value < 10) {
            return CHINESE_DIGITS[value];
        }
        int tens = value / 10;
        int ones = value % 10;
        StringBuilder formattedValue = new StringBuilder();
        if (tens > 1) {
            formattedValue.append(CHINESE_DIGITS[tens]);
        }
        formattedValue.append("十");
        if (ones > 0) {
            formattedValue.append(CHINESE_DIGITS[ones]);
        }
        return formattedValue.toString();
    }
}
