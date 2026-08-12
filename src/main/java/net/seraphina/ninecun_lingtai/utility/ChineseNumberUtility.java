package net.seraphina.ninecun_lingtai.utility;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

public final class ChineseNumberUtility {
    private static final String[] CHINESE_DIGITS = {
            "零", "一", "二", "三", "四", "五", "六", "七", "八", "九"
    };
    private static final String[] GROUP_UNITS = {
            "", "万", "亿", "兆", "京", "垓", "秭", "穰", "沟", "涧", "正", "载", "极", "恒河沙", "阿僧祇", "那由他"
    };
    private static final BigInteger TEN_THOUSAND = BigInteger.valueOf(10_000L);

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
        return toChineseInteger(BigInteger.valueOf(value));
    }

    public static String toChineseInteger(long value) {
        return toChineseInteger(BigInteger.valueOf(value));
    }

    public static String toChineseInteger(BigInteger value) {
        if (value.signum() == 0) {
            return CHINESE_DIGITS[0];
        }
        if (value.signum() < 0) {
            return "负" + toChineseInteger(value.negate());
        }

        int[] groups = splitIntoGroups(value);
        StringBuilder formattedValue = new StringBuilder();
        boolean needsZero = false;
        for (int groupIndex = groups.length - 1; groupIndex >= 0; groupIndex--) {
            int groupValue = groups[groupIndex];
            if (groupValue == 0) {
                if (!formattedValue.isEmpty()) {
                    needsZero = true;
                }
                continue;
            }
            if (!formattedValue.isEmpty() && (needsZero || groupValue < 1_000)) {
                formattedValue.append(CHINESE_DIGITS[0]);
            }
            formattedValue.append(formatGroup(groupValue));
            formattedValue.append(getGroupUnit(groupIndex));
            needsZero = false;
        }
        if (formattedValue.indexOf("一十") == 0) {
            formattedValue.deleteCharAt(0);
        }
        return formattedValue.toString();
    }

    public static String toCompactChinese(BigInteger value) {
        if (value.signum() < 0) {
            return "负" + toCompactChinese(value.negate());
        }
        if (value.compareTo(TEN_THOUSAND) < 0) {
            return toChineseInteger(value);
        }
        int groupIndex = (value.toString().length() - 1) / 4;
        BigDecimal scaledValue = scaleToGroup(value, groupIndex);
        if (scaledValue.compareTo(new BigDecimal(TEN_THOUSAND)) >= 0) {
            groupIndex++;
            scaledValue = scaleToGroup(value, groupIndex);
        }
        return toChineseDecimal(scaledValue, 1) + getGroupUnit(groupIndex);
    }

    public static String toCompactChinese(BigDecimal value, int maximumFractionDigits) {
        if (value.signum() < 0) {
            return "负" + toCompactChinese(value.negate(), maximumFractionDigits);
        }
        if (value.compareTo(new BigDecimal(TEN_THOUSAND)) < 0) {
            return toChineseDecimal(value, maximumFractionDigits);
        }
        return toCompactChinese(value.setScale(0, RoundingMode.HALF_UP).toBigInteger());
    }

    public static String toCompactChinese(double value, int maximumFractionDigits) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Chinese compact value must be finite");
        }
        return toCompactChinese(BigDecimal.valueOf(value), maximumFractionDigits);
    }

    public static String formatDurationTicks(long ticks) {
        long totalSeconds = Math.max(0L, (ticks + 19L) / 20L);
        if (totalSeconds == 0L) {
            return "零秒";
        }
        long hours = totalSeconds / 3600L;
        long minutes = totalSeconds % 3600L / 60L;
        long seconds = totalSeconds % 60L;
        StringBuilder formattedDuration = new StringBuilder();
        if (hours > 0L) {
            formattedDuration.append(toChineseInteger(hours)).append("时");
        }
        if (minutes > 0L) {
            formattedDuration.append(toChineseInteger(minutes)).append("分");
        }
        if (seconds > 0L) {
            formattedDuration.append(toChineseInteger(seconds)).append("秒");
        }
        return formattedDuration.toString();
    }

    public static String toChineseDecimal(double value, int maximumFractionDigits) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Chinese decimal value must be finite");
        }
        return toChineseDecimal(BigDecimal.valueOf(value), maximumFractionDigits);
    }

    public static String toChineseDecimal(BigDecimal value, int maximumFractionDigits) {
        if (maximumFractionDigits < 0) {
            throw new IllegalArgumentException("Maximum fraction digits must not be negative");
        }
        BigDecimal roundedValue = value.setScale(maximumFractionDigits, RoundingMode.HALF_UP).stripTrailingZeros();
        if (roundedValue.signum() < 0) {
            return "负" + toChineseDecimal(roundedValue.negate(), maximumFractionDigits);
        }

        String plainValue = roundedValue.toPlainString();
        int decimalPointIndex = plainValue.indexOf('.');
        if (decimalPointIndex < 0) {
            return toChineseInteger(new BigInteger(plainValue));
        }

        StringBuilder formattedValue = new StringBuilder(
                toChineseInteger(new BigInteger(plainValue.substring(0, decimalPointIndex)))
        ).append("点");
        for (int index = decimalPointIndex + 1; index < plainValue.length(); index++) {
            formattedValue.append(CHINESE_DIGITS[plainValue.charAt(index) - '0']);
        }
        return formattedValue.toString();
    }

    private static BigDecimal scaleToGroup(BigInteger value, int groupIndex) {
        return new BigDecimal(value).divide(
                new BigDecimal(TEN_THOUSAND.pow(groupIndex)),
                1,
                RoundingMode.HALF_UP
        );
    }

    private static String getGroupUnit(int groupIndex) {
        if (groupIndex < GROUP_UNITS.length) {
            return GROUP_UNITS[groupIndex];
        }
        return "乘万的" + toChineseInteger(groupIndex) + "次方";
    }

    private static int[] splitIntoGroups(BigInteger value) {
        int groupCount = Math.max(1, (value.toString().length() + 3) / 4);
        int[] groups = new int[groupCount];
        BigInteger remainingValue = value;
        for (int groupIndex = 0; groupIndex < groupCount; groupIndex++) {
            BigInteger[] division = remainingValue.divideAndRemainder(TEN_THOUSAND);
            groups[groupIndex] = division[1].intValue();
            remainingValue = division[0];
        }
        return groups;
    }

    private static String formatGroup(int groupValue) {
        String[] digitUnits = {"千", "百", "十", ""};
        int divisor = 1_000;
        boolean pendingZero = false;
        StringBuilder formattedGroup = new StringBuilder();
        for (String digitUnit : digitUnits) {
            int digit = groupValue / divisor;
            groupValue %= divisor;
            if (digit > 0) {
                if (pendingZero) {
                    formattedGroup.append(CHINESE_DIGITS[0]);
                    pendingZero = false;
                }
                formattedGroup.append(CHINESE_DIGITS[digit]).append(digitUnit);
            } else if (!formattedGroup.isEmpty() && groupValue > 0) {
                pendingZero = true;
            }
            divisor /= 10;
        }
        return formattedGroup.toString();
    }
}
