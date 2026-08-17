package net.seraphina.ninecun_lingtai.utility;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public final class ChineseNumeralUtility {
    private static final String[] CHINESE_DIGITS = {
            "零", "一", "二", "三", "四", "五", "六", "七", "八", "九"
    };
    private static final String[] SMALL_UNITS = {"", "十", "百", "千"};
    private static final String[] LARGE_UNITS = {"", "万", "亿", "兆", "京"};
    private static final BigInteger SECTION_BASE = BigInteger.valueOf(10_000L);

    public static String toChinese(long value) {
        if (value == 0L) {
            return CHINESE_DIGITS[0];
        }

        BigInteger remainingValue = BigInteger.valueOf(value).abs();
        List<Integer> sections = new ArrayList<>();
        while (remainingValue.signum() > 0) {
            BigInteger[] division = remainingValue.divideAndRemainder(SECTION_BASE);
            sections.add(division[1].intValue());
            remainingValue = division[0];
        }

        StringBuilder result = new StringBuilder();
        boolean needsZero = false;
        for (int index = sections.size() - 1; index >= 0; index--) {
            int section = sections.get(index);
            if (section == 0) {
                needsZero = result.length() > 0;
                continue;
            }
            if (result.length() > 0 && (needsZero || section < 1_000)) {
                result.append(CHINESE_DIGITS[0]);
            }
            result.append(convertSection(section)).append(LARGE_UNITS[index]);
            needsZero = false;
        }

        return value < 0L ? "负" + result : result.toString();
    }

    public static String replaceArabicNumerals(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder(value.length());
        int index = 0;
        while (index < value.length()) {
            char character = value.charAt(index);
            if (character < '0' || character > '9') {
                result.append(character);
                index++;
                continue;
            }

            int start = index;
            while (index < value.length()) {
                char numericCharacter = value.charAt(index);
                if (numericCharacter < '0' || numericCharacter > '9') {
                    break;
                }
                index++;
            }
            appendChineseNumerals(result, value.substring(start, index));
        }
        return result.toString();
    }

    private static void appendChineseNumerals(StringBuilder result, String arabicNumerals) {
        try {
            result.append(toChinese(Long.parseLong(arabicNumerals)));
        } catch (NumberFormatException exception) {
            for (int index = 0; index < arabicNumerals.length(); index++) {
                result.append(CHINESE_DIGITS[arabicNumerals.charAt(index) - '0']);
            }
        }
    }

    private static String convertSection(int section) {
        StringBuilder result = new StringBuilder();
        boolean needsZero = false;
        for (int index = SMALL_UNITS.length - 1; index >= 0; index--) {
            int unitValue = (int) Math.pow(10, index);
            int digit = section / unitValue % 10;
            if (digit == 0) {
                needsZero = result.length() > 0;
                continue;
            }
            if (needsZero) {
                result.append(CHINESE_DIGITS[0]);
            }
            if (digit != 1 || index != 1 || result.length() > 0) {
                result.append(CHINESE_DIGITS[digit]);
            }
            result.append(SMALL_UNITS[index]);
            needsZero = false;
        }
        return result.toString();
    }
}
