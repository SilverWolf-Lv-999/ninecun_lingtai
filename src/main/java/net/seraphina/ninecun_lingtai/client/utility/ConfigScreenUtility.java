package net.seraphina.ninecun_lingtai.client.utility;

import java.math.BigDecimal;
import java.util.OptionalDouble;
import java.util.regex.Pattern;

public final class ConfigScreenUtility {
    private static final Pattern DECIMAL_INPUT_PATTERN = Pattern.compile("-?\\d*(?:\\.\\d*)?");

    public static boolean isPotentialDecimal(String value) {
        return value != null && DECIMAL_INPUT_PATTERN.matcher(value).matches();
    }

    public static OptionalDouble parseDoubleInRange(String value, double minimum, double maximum) {
        if (value == null || value.isBlank()) {
            return OptionalDouble.empty();
        }
        try {
            double parsedValue = Double.parseDouble(value);
            if (!Double.isFinite(parsedValue) || parsedValue < minimum || parsedValue > maximum) {
                return OptionalDouble.empty();
            }
            return OptionalDouble.of(parsedValue);
        } catch (NumberFormatException exception) {
            return OptionalDouble.empty();
        }
    }

    public static String formatDouble(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
